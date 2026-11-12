import { useEffect, useRef, useState, type ReactNode } from "react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"

import { anuncioCorregido, anuncioRegistrado, type RegistroAnimo } from "@/features/bienestar"
import { useAuth } from "@/features/auth/useAuth"
import {
  estadoDelGuion,
  proximaPregunta,
  responderPregunta,
  responderRepregunta,
  saltearPregunta,
  saltearRepregunta as saltearRepreguntaEnElBack,
  siguienteRepregunta,
  type OrigenDeRepregunta,
} from "@/features/historia/api"
import { aperturaDeRepregunta } from "@/features/historia/aperturaDeRepregunta"
import {
  CIERRE_SIN_REPREGUNTA,
  CONTARTE_ALGO_MAS,
  INVITACION_AHORA_NO,
  INVITACION_SI,
  INVITACION_TEXTO,
  OTRA_PREGUNTA,
  RESPUESTAS_AHORA_NO,
  RESPUESTAS_CONTARTE_ALGO_MAS,
  SALTEAR,
  SEGUIR_MI_HISTORIA,
  YA_RESPONDISTE_TODO,
} from "@/features/historia/mensajes"
import { pideSeguirConLaHistoria } from "@/features/historia/pideSeguirConLaHistoria"
import { rotar } from "@/features/historia/rotar"
import { RUTA_HISTORIA_DE_VIDA } from "@/features/historia/rutas"
import type { DatoFaltante, PreguntaDelGuion, Repregunta } from "@/features/historia/tipos"
import { clavesHistoria, useEstadoDelGuion } from "@/features/historia/useHistoria"
import { ApiError } from "@/lib/api"
import { mensajeDeError } from "@/lib/errores"

import { enviarMensaje } from "./api"
import { ChatContexto, type ChatContextoValor } from "./contexto"
import {
  DURACION_ESPERA_INICIAL_MS,
  INTERVALO_ESPERA_INICIAL_MS,
  INTERVALO_ESPERA_MS,
  LIMITE_TURNOS,
  TOPE_ESPERA_MS,
  type MensajeChat,
  type ObjetivoGuion,
  type ResultadoApertura,
  type ResultadoCompletar,
  type TurnoChat,
} from "./tipos"

/**
 * La conversación se sostiene por encima de las pantallas, así sobrevive a ir a Recordatorios o
 * Perfil y volver. La `key` es el id de la persona: al cerrar sesión o ingresar con otra cuenta,
 * React vuelve a crear el estado y empieza de cero. Nada se guarda en el navegador (5.8).
 */
export function ChatProvider({ children }: { children: ReactNode }) {
  const { usuario } = useAuth()
  return <EstadoDelChat key={usuario?.id ?? "sin-sesion"}>{children}</EstadoDelChat>
}

/** Con 503 el texto es el que manda el back (6.4); con cualquier otro fallo, el general. */
function textoDeError(error: unknown): string {
  if (error instanceof ApiError && error.status === 503 && error.detalle) return error.detalle
  return mensajeDeError(error)
}

/** El texto de un mensaje tal como viaja al modelo: la introducción y la pregunta del guion, juntas. */
function textoDelTurno(mensaje: MensajeChat): string {
  return mensaje.preguntaDestacada ? `${mensaje.texto}\n${mensaje.preguntaDestacada}`.trim() : mensaje.texto
}

/**
 * La conversación que viaja con el mensaje nuevo (11.4, paso 3): la pregunta contextual de Alma
 * (solo si hay ánimo, el vigente en este momento) y los mensajes, sin el mensaje nuevo. Hasta los
 * últimos 10. No se mandan el saludo, la pregunta del widget ni la burbuja del ánimo, ni la
 * invitación, el cierre y el aviso de "sin pendientes" (textos fijos del front que no generó el modelo),
 * ni las burbujas de los botones tocados (`origen: "ACCION"`, contrato del mapa de vida, 2.9).
 */
function armarConversacion(animo: RegistroAnimo | null, mensajes: MensajeChat[]): TurnoChat[] {
  const turnos: TurnoChat[] = []
  if (animo) turnos.push({ rol: "ALMA", texto: animo.preguntaContextual })
  for (const mensaje of mensajes) {
    if (mensaje.origen === "ACCION") continue
    if (mensaje.tipo && mensaje.tipo !== "TEXTO" && mensaje.tipo !== "GUION") continue
    turnos.push({ rol: mensaje.rol, texto: textoDelTurno(mensaje) })
  }
  return turnos.slice(-LIMITE_TURNOS)
}

const dormir = (ms: number) => new Promise<void>((resolver) => setTimeout(resolver, ms))

/** Una pregunta que ya no existe o ya no corresponde: reintentar daría el mismo error. */
const esConflicto = (e: unknown) => e instanceof ApiError && (e.status === 404 || e.status === 409)

function EstadoDelChat({ children }: { children: ReactNode }) {
  const navegar = useNavigate()
  const clienteConsultas = useQueryClient()
  const { usuario } = useAuth()

  const [animo, setAnimo] = useState<RegistroAnimo | null>(null)
  const [cambiandoAnimo, setCambiandoAnimo] = useState(false)
  const [yaEscribio, setYaEscribio] = useState(false)
  const [mensajes, setMensajes] = useState<MensajeChat[]>([])
  const [pendiente, setPendiente] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [errorReintentable, setErrorReintentable] = useState(false)
  const [borrador, setBorrador] = useState("")
  const [anuncioAnimo, setAnuncioAnimo] = useState("")

  // Modo guion (contrato de historia de vida, 2.1). Cada valor que se lee dentro de una espera o de
  // un pedido tiene también su copia en una ref, para no leer un estado viejo.
  const [objetivoActivo, setObjetivoActivo] = useState<ObjetivoGuion | null>(null)
  const [esperandoRepregunta, setEsperandoRepregunta] = useState(false)
  const [cierrePendiente, setCierrePendiente] = useState(false)
  const [invitacionPendiente, setInvitacionPendiente] = useState(false)
  const [ocupadoGuion, setOcupadoGuion] = useState(false)
  // Hubo una alarma en esta visita: ni el botón fijo ni la invitación a pedido se ofrecen (contrato del mapa de vida, 2.8).
  const [huboAlarma, setHuboAlarma] = useState(false)
  // Cuántas preguntas quedan sin responder: lo pide la consulta compartida con Historia de vida y se refresca al
  // invalidar tras responder o saltear.
  const estadoGuion = useEstadoDelGuion()

  const objetivoRef = useRef<ObjetivoGuion | null>(null)
  /** La pregunta del guion que dio origen a la charla: hace falta para pedir la repregunta correcta (2.1). */
  const preguntaOrigenRef = useRef<number | null>(null)
  /**
   * El recuerdo que se está completando con "Seguir completando" (contrato del mapa de vida, 2.7). Excluyente con
   * `preguntaOrigenRef`: la charla usa uno u otro, nunca los dos. Con este, las repreguntas siguientes se piden con
   * `recuerdoId=` y `excluir=`.
   */
  const recuerdoEnCursoRef = useRef<string | null>(null)
  /**
   * El id del mensaje del guion (pregunta o repregunta) que espera respuesta. Hay uno en la lista si y solo
   * si hay un objetivo activo: así una pregunta nunca queda dos veces sin responder ni queda escrita sin botones.
   */
  const bloqueSinResponderRef = useRef<number | null>(null)
  /** Los datos ya preguntados del recuerdo de esta charla: viajan como `excluir` (2.4). */
  const datosPreguntadosRef = useRef<DatoFaltante[]>([])
  /** La invitación está en pantalla esperando un "Sí" o un "Ahora no". */
  const invitacionRef = useRef({ pendiente: false })
  const alarmaRef = useRef(false)
  /** Recuerdos que ya tuvieron su apertura en esta charla (2.11) y cuántas aperturas se usaron (rota las tres frases). */
  const recuerdosAbiertosRef = useRef(new Set<string>())
  const aperturasUsadasRef = useRef(0)
  /** Cuántas veces se usó cada respuesta fija de Alma a un botón (2.9): así rotan las variantes. */
  const respuestasFijasUsadasRef = useRef({ contarAlgoMas: 0, ahoraNo: 0 })
  const accionDeReintentoRef = useRef<(() => void) | null>(null)
  const esperandoRef = useRef(false)
  const ocupadoRef = useRef(false)
  const vivoRef = useRef(true)
  // Evita que un doble toque mande dos veces (el estado `pendiente` se actualiza un instante después).
  const enviando = useRef(false)
  const proximoId = useRef(1)
  const campoDeMensaje = useRef<HTMLInputElement>(null)

  useEffect(() => {
    vivoRef.current = true
    return () => {
      vivoRef.current = false
    }
  }, [])

  const mutacion = useMutation({
    mutationFn: ({ mensaje, conversacion }: { mensaje: string; conversacion: TurnoChat[] }) =>
      enviarMensaje(mensaje, conversacion),
  })

  // ── Ayudas ──

  function agregar(mensaje: Omit<MensajeChat, "id">): number {
    const id = proximoId.current++
    setMensajes((actuales) => [...actuales, { id, ...mensaje }])
    return id
  }

  /** Saca de la conversación la pregunta o repregunta del guion que la persona no llegó a responder. */
  function retirarBloqueSinResponder() {
    const id = bloqueSinResponderRef.current
    if (id === null) return
    bloqueSinResponderRef.current = null
    setMensajes((actuales) => actuales.filter((mensaje) => mensaje.id !== id))
  }

  function fijarObjetivo(objetivo: ObjetivoGuion | null) {
    objetivoRef.current = objetivo
    setObjetivoActivo(objetivo)
  }

  function limpiarError() {
    setError(null)
    setErrorReintentable(false)
    accionDeReintentoRef.current = null
  }

  /** Con `reintento` el aviso lleva "Probar de nuevo"; con `null`, no (reenviar no tendría sentido). */
  function ponerError(texto: string, reintento: (() => void) | null) {
    setError(texto)
    setErrorReintentable(reintento !== null)
    accionDeReintentoRef.current = reintento
  }

  /** La línea de tiempo y la tarjeta "Próxima parada" dependen de lo que pasa en el guion. */
  function invalidarHistoria() {
    void clienteConsultas.invalidateQueries({ queryKey: clavesHistoria.raiz(usuario?.id) })
  }

  /** A qué apunta el pedido de la repregunta: el recuerdo en curso, la pregunta de origen o, sin ninguno, todos. */
  function origenDeRepregunta(): OrigenDeRepregunta {
    if (recuerdoEnCursoRef.current !== null) return { recuerdoId: recuerdoEnCursoRef.current }
    if (preguntaOrigenRef.current !== null) return { preguntaId: preguntaOrigenRef.current }
    return null
  }

  /** Un error de un pedido del guion: si la pregunta ya no corresponde (404/409), se sale del modo guion y se refresca. */
  function errorDelGuion(e: unknown, reintento: () => void) {
    if (esConflicto(e)) {
      fijarObjetivo(null)
      retirarBloqueSinResponder()
      invalidarHistoria()
      ponerError(textoDeError(e), null)
    } else {
      ponerError(textoDeError(e), reintento)
    }
  }

  /** Un pedido del guion a la vez: deshabilita los botones y evita el doble toque. */
  async function conBloqueo(accion: () => Promise<void>) {
    if (ocupadoRef.current || enviando.current || esperandoRef.current) return
    ocupadoRef.current = true
    setOcupadoGuion(true)
    try {
      await accion()
    } finally {
      ocupadoRef.current = false
      setOcupadoGuion(false)
    }
  }

  // ── Invitación y burbujas de acciones (contrato del mapa de vida, 2.8 y 2.9) ──

  function cerrarInvitacion() {
    invitacionRef.current.pendiente = false
    setInvitacionPendiente(false)
  }

  function marcarAlarma() {
    alarmaRef.current = true
    setHuboAlarma(true)
  }

  /** Lo que toca la persona deja una burbuja suya. Es local: no viaja al modelo ni cuenta como "ya escribió". */
  function agregarAccion(texto: string) {
    agregar({ rol: "PERSONA", texto, origen: "ACCION" })
  }

  /** La respuesta de Alma a un botón, sin llamar al modelo: una de las variantes, rotando (contrato 2.9). */
  function responderConTextoFijo(variantes: readonly string[], cual: "contarAlgoMas" | "ahoraNo") {
    const veces = respuestasFijasUsadasRef.current[cual]++
    agregar({ rol: "ALMA", tipo: "RESPUESTA_FIJA", texto: rotar(variantes, veces) })
  }

  function hayAlgoEnCurso() {
    return ocupadoRef.current || enviando.current || esperandoRef.current
  }

  /**
   * La persona escribió que quiere seguir con su historia y el chat ya respondió sin alarma (2.8 b). Se pide el
   * estado fresco (no el de la caché) y se ofrece la invitación, o el aviso de que ya respondió todo. Se muestra
   * aunque antes ya se hubiera mostrado o rechazado.
   */
  async function invitarAPedido() {
    try {
      const estado = await clienteConsultas.fetchQuery({
        queryKey: clavesHistoria.estado(usuario?.id),
        queryFn: estadoDelGuion,
        staleTime: 0,
      })
      if (!vivoRef.current || alarmaRef.current || objetivoRef.current !== null) return
      if (estado.pendientes > 0) {
        if (invitacionRef.current.pendiente) return
        invitacionRef.current.pendiente = true
        setInvitacionPendiente(true)
        agregar({ rol: "ALMA", tipo: "INVITACION", texto: INVITACION_TEXTO })
      } else {
        agregar({ rol: "ALMA", tipo: "SIN_PENDIENTES", texto: YA_RESPONDISTE_TODO })
      }
    } catch {
      // Sin invitación: es un extra, no un error para la persona.
    }
  }

  // ── Guion ──

  function mostrarPregunta(pregunta: PreguntaDelGuion, introduccion: string | null) {
    // Nunca dos bloques sin responder a la vez.
    retirarBloqueSinResponder()
    datosPreguntadosRef.current = []
    recuerdosAbiertosRef.current.clear()
    recuerdoEnCursoRef.current = null
    preguntaOrigenRef.current = pregunta.id
    fijarObjetivo({ tipo: "PREGUNTA", preguntaId: pregunta.id })
    setCierrePendiente(false)
    // Abrir una pregunta del guion cuenta como "ya empezó la conversación": el widget de ánimo desaparece,
    // igual que cuando la persona manda su primer mensaje (el ingreso queda sin ánimo si no eligió).
    setYaEscribio(true)
    setCambiandoAnimo(false)
    bloqueSinResponderRef.current = agregar({
      rol: "ALMA",
      tipo: "GUION",
      texto: introduccion ?? "",
      preguntaDestacada: pregunta.texto,
    })
  }

  function mostrarRepregunta(repregunta: Repregunta) {
    // Una repregunta por dato en la charla (2.4): se anota al mostrarla para excluirla de la próxima.
    datosPreguntadosRef.current = [...datosPreguntadosRef.current, repregunta.dato]
    retirarBloqueSinResponder()
    fijarObjetivo({ tipo: "REPREGUNTA", recuerdoId: repregunta.recuerdoId, dato: repregunta.dato })
    // Antes de la primera repregunta de cada recuerdo va una frase fija que lo nombra (2.11), en otro párrafo.
    let texto = repregunta.texto
    if (!recuerdosAbiertosRef.current.has(repregunta.recuerdoId)) {
      recuerdosAbiertosRef.current.add(repregunta.recuerdoId)
      const apertura = aperturaDeRepregunta(repregunta.tituloRecuerdo, aperturasUsadasRef.current++)
      texto = `${apertura}\n\n${texto}`
    }
    bloqueSinResponderRef.current = agregar({ rol: "ALMA", tipo: "GUION", texto })
  }

  function mostrarCierre() {
    fijarObjetivo(null)
    setCierrePendiente(true)
    agregar({ rol: "ALMA", tipo: "CIERRE", texto: CIERRE_SIN_REPREGUNTA })
  }

  /** Pide la próxima pregunta y la deja en el chat; sin pendientes, deja el mensaje final. Maneja sus errores. */
  async function irAProxima() {
    try {
      const proxima = await proximaPregunta()
      if (!proxima.pregunta || !proxima.quedanPendientes) {
        fijarObjetivo(null)
        invalidarHistoria()
        if (proxima.mensajeFinal) agregar({ rol: "ALMA", tipo: "CIERRE", texto: proxima.mensajeFinal })
        return
      }
      mostrarPregunta(proxima.pregunta, proxima.introduccion)
    } catch (e) {
      ponerError(textoDeError(e), () => void conBloqueo(irAProxima))
    }
  }

  /**
   * La espera de la extracción (2.3 y 2.10): consulta cada 1 s durante los primeros 10 s y cada 2 s después mientras
   * el back dice PROCESANDO, hasta 60 s.
   * Un error de red o 5xx se reintenta una vez; si vuelve a fallar, se sigue sin repregunta y se avisa.
   */
  async function esperarRepregunta() {
    const inicio = Date.now()
    const intervalo = () =>
      Date.now() - inicio < DURACION_ESPERA_INICIAL_MS ? INTERVALO_ESPERA_INICIAL_MS : INTERVALO_ESPERA_MS
    let fallos = 0
    try {
      while (vivoRef.current) {
        let respuesta
        try {
          respuesta = await siguienteRepregunta(origenDeRepregunta(), datosPreguntadosRef.current)
          fallos = 0
        } catch (e) {
          fallos++
          if (fallos >= 2) {
            // La respuesta ya está guardada y no hay nada que reintentar: se sigue sin repregunta, sin aviso.
            // Solo se registra el tipo de error en la consola (nunca texto de la persona).
            console.error(
              "Historia de vida: falló dos veces la espera de la repregunta",
              e instanceof ApiError ? `status ${e.status}` : "error desconocido"
            )
            mostrarCierre()
            return
          }
          await dormir(intervalo())
          continue
        }
        if (!vivoRef.current) return
        if (respuesta.estado === "HAY" && respuesta.repregunta) {
          mostrarRepregunta(respuesta.repregunta)
          return
        }
        if (respuesta.estado === "NINGUNA" || Date.now() - inicio >= TOPE_ESPERA_MS) {
          mostrarCierre()
          return
        }
        await dormir(intervalo())
      }
    } finally {
      esperandoRef.current = false
      setEsperandoRepregunta(false)
      // La extracción terminó: la etapa, el año y los datos pendientes de los recuerdos pueden haber cambiado.
      invalidarHistoria()
    }
  }

  /**
   * "Seguir completando este recuerdo" (contrato del mapa de vida, 2.7), desde Historia de vida. Pide la repregunta de
   * ese recuerdo; con PROCESANDO espera como en 2.10 (tope de 60 s, que se trata como NINGUNA). Con HAY empieza una
   * charla nueva sobre el recuerdo: muestra la apertura y la repregunta y activa el objetivo REPREGUNTA, con
   * `recuerdoEnCursoRef` en lugar de la pregunta de origen. No deja burbuja: la pantalla navega al chat. Si falla,
   * lanza el error para que la pantalla lo muestre.
   *
   * Si el chat está ocupado (esperando la respuesta del modelo o la extracción de una respuesta anterior), espera a
   * que se libere en lugar de rendirse: antes devolvía `YA_ACTIVA` enseguida, la pantalla navegaba al chat igual y no
   * aparecía nada (ni la apertura ni la repregunta). Solo si pasa el tope de 60 s devuelve `YA_ACTIVA`, y la pantalla
   * avisa en lugar de navegar.
   */
  async function seguirCompletando(recuerdoId: string): Promise<ResultadoCompletar> {
    const esperaDelChat = Date.now()
    while (hayAlgoEnCurso() && vivoRef.current && Date.now() - esperaDelChat < TOPE_ESPERA_MS) {
      await dormir(INTERVALO_ESPERA_INICIAL_MS)
    }
    if (hayAlgoEnCurso()) return { resultado: "YA_ACTIVA" }
    ocupadoRef.current = true
    setOcupadoGuion(true)
    try {
      const inicio = Date.now()
      while (vivoRef.current) {
        const respuesta = await siguienteRepregunta({ recuerdoId }, [])
        if (!vivoRef.current) break
        if (respuesta.estado === "HAY" && respuesta.repregunta) {
          limpiarError()
          cerrarInvitacion()
          datosPreguntadosRef.current = []
          recuerdosAbiertosRef.current.clear()
          preguntaOrigenRef.current = null
          recuerdoEnCursoRef.current = recuerdoId
          setCierrePendiente(false)
          setYaEscribio(true)
          setCambiandoAnimo(false)
          mostrarRepregunta(respuesta.repregunta)
          return { resultado: "ABIERTA" }
        }
        if (respuesta.estado === "NINGUNA" || Date.now() - inicio >= TOPE_ESPERA_MS) break
        await dormir(
          Date.now() - inicio < DURACION_ESPERA_INICIAL_MS ? INTERVALO_ESPERA_INICIAL_MS : INTERVALO_ESPERA_MS
        )
      }
      // Los datos que faltaban ya no hacen falta o se agotaron los intentos: se refresca la lista.
      invalidarHistoria()
      return { resultado: "NINGUNA" }
    } finally {
      ocupadoRef.current = false
      setOcupadoGuion(false)
    }
  }

  function iniciarEspera() {
    esperandoRef.current = true
    setEsperandoRepregunta(true)
    void esperarRepregunta()
  }

  /** 7.1 y 7.2: ALARMA (200) corta el modo guion; ACEPTADA (202) empieza la espera. */
  async function responderAlGuion(objetivo: ObjetivoGuion, texto: string) {
    const resultado =
      objetivo.tipo === "PREGUNTA"
        ? await responderPregunta(objetivo.preguntaId, texto)
        : await responderRepregunta(objetivo.recuerdoId, objetivo.dato, texto)

    // Contestada (aceptada o con alarma): el bloque queda en la conversación como historial.
    bloqueSinResponderRef.current = null
    if (resultado.estado === "ALARMA") {
      agregar({ rol: "ALMA", texto: resultado.respuesta })
      fijarObjetivo(null)
      preguntaOrigenRef.current = null
      marcarAlarma()
      return
    }
    fijarObjetivo(null)
    invalidarHistoria()
    iniciarEspera()
  }

  // ── Envío ──

  async function enviar(textoIngresado: string) {
    const texto = textoIngresado.trim()
    if (!texto || enviando.current || esperandoRef.current || ocupadoRef.current) return
    enviando.current = true

    const objetivo = objetivoRef.current
    // El ánimo vigente en este momento: no se guarda aparte.
    const conversacion = armarConversacion(animo, mensajes)
    const idPersona = proximoId.current++

    setMensajes((actuales) => [...actuales, { id: idPersona, rol: "PERSONA", texto }])
    // Se limpia el campo solo si todavía tiene lo que se manda (un reintento no pisa lo nuevo).
    setBorrador((actual) => (actual.trim() === texto ? "" : actual))
    limpiarError()
    setYaEscribio(true)
    // Si tenía abierto "Cambiar cómo me siento" y no eligió, el widget se cierra y queda el ánimo anterior.
    setCambiandoAnimo(false)
    setPendiente(true)
    // Escribir sin contestar la invitación cuenta como "Ahora no"; sin contestar el cierre, como "Contarte algo más".
    // (Sin burbuja: la persona no tocó ningún botón.)
    if (invitacionRef.current.pendiente) cerrarInvitacion()
    setCierrePendiente(false)

    let respondioElChat = false
    try {
      if (objetivo) {
        await responderAlGuion(objetivo, texto)
      } else {
        const { respuesta, alarma } = await mutacion.mutateAsync({ mensaje: texto, conversacion })
        agregar({ rol: "ALMA", texto: respuesta })
        // Una alarma del chat se trata igual que una del guion: en esta visita no se ofrece seguir con la historia.
        if (alarma) marcarAlarma()
        respondioElChat = true
      }
    } catch (e) {
      setMensajes((actuales) => actuales.filter((mensaje) => mensaje.id !== idPersona))
      // El texto vuelve al campo solo si está vacío: si ya escribió otra cosa, no se pisa.
      setBorrador((actual) => (actual === "" ? texto : actual))
      // "Probar de nuevo" reenvía este mismo texto con el objetivo vigente: al guion si sigue activo, al chat si no.
      errorDelGuionOChat(e, objetivo !== null, () => void enviarRef.current(texto))
    } finally {
      setPendiente(false)
      enviando.current = false
    }
    // Invitación a pedido (2.8 b): el mensaje ya se mandó al chat como siempre; ahora se evalúa lo que escribió.
    if (respondioElChat && pideSeguirConLaHistoria(texto)) void invitarAPedido()
  }

  function errorDelGuionOChat(e: unknown, enModoGuion: boolean, reintento: () => void) {
    if (enModoGuion) errorDelGuion(e, reintento)
    else ponerError(textoDeError(e), reintento)
  }

  // El reintento guardado llama a la versión más nueva de `enviar` (con el ánimo y los mensajes de ahora).
  const enviarRef = useRef(enviar)
  useEffect(() => {
    enviarRef.current = enviar
  })

  // ── Botones del modo guion ──

  async function abrirPreguntaDelGuion(): Promise<ResultadoApertura> {
    if (objetivoRef.current || ocupadoRef.current || enviando.current || esperandoRef.current) {
      return { resultado: "YA_ACTIVA" }
    }
    ocupadoRef.current = true
    setOcupadoGuion(true)
    try {
      const proxima = await proximaPregunta()
      if (!proxima.pregunta || !proxima.quedanPendientes) {
        invalidarHistoria()
        return { resultado: "SIN_PENDIENTES", mensajeFinal: proxima.mensajeFinal ?? "" }
      }
      limpiarError()
      cerrarInvitacion()
      mostrarPregunta(proxima.pregunta, proxima.introduccion)
      return { resultado: "ABIERTA" }
    } finally {
      ocupadoRef.current = false
      setOcupadoGuion(false)
    }
  }

  /** "Otra pregunta" debajo de una pregunta: la saltea en el back y pide la siguiente. */
  async function saltearYPasarALaSiguiente() {
    const objetivo = objetivoRef.current
    if (objetivo?.tipo !== "PREGUNTA") return
    limpiarError()
    try {
      await saltearPregunta(objetivo.preguntaId)
    } catch (e) {
      errorDelGuion(e, () => void conBloqueo(saltearYPasarALaSiguiente))
      return
    }
    bloqueSinResponderRef.current = null
    fijarObjetivo(null)
    invalidarHistoria()
    await irAProxima()
  }

  function otraPregunta() {
    if (objetivoRef.current?.tipo !== "PREGUNTA" || hayAlgoEnCurso()) return
    agregarAccion(OTRA_PREGUNTA)
    void conBloqueo(saltearYPasarALaSiguiente)
  }

  async function saltearLaRepregunta() {
    const objetivo = objetivoRef.current
    if (objetivo?.tipo !== "REPREGUNTA") return
    limpiarError()
    try {
      await saltearRepreguntaEnElBack(objetivo.recuerdoId, objetivo.dato)
    } catch (e) {
      errorDelGuion(e, () => void conBloqueo(saltearLaRepregunta))
      return
    }
    bloqueSinResponderRef.current = null
    fijarObjetivo(null)
    invalidarHistoria()
    iniciarEspera()
  }

  function saltearRepregunta() {
    if (objetivoRef.current?.tipo !== "REPREGUNTA" || hayAlgoEnCurso()) return
    agregarAccion(SALTEAR)
    void conBloqueo(saltearLaRepregunta)
  }

  function otraPreguntaDespuesDelCierre() {
    if (hayAlgoEnCurso()) return
    limpiarError()
    agregarAccion(OTRA_PREGUNTA)
    setCierrePendiente(false)
    void conBloqueo(irAProxima)
  }

  function contarAlgoMas() {
    agregarAccion(CONTARTE_ALGO_MAS)
    responderConTextoFijo(RESPUESTAS_CONTARTE_ALGO_MAS, "contarAlgoMas")
    setCierrePendiente(false)
    preguntaOrigenRef.current = null
    recuerdoEnCursoRef.current = null
  }

  function aceptarInvitacion() {
    if (hayAlgoEnCurso()) return
    limpiarError()
    agregarAccion(INVITACION_SI)
    cerrarInvitacion()
    void conBloqueo(irAProxima)
  }

  function rechazarInvitacion() {
    if (hayAlgoEnCurso()) return
    agregarAccion(INVITACION_AHORA_NO)
    responderConTextoFijo(RESPUESTAS_AHORA_NO, "ahoraNo")
    cerrarInvitacion()
  }

  /** Botón fijo "Seguir mi historia de vida" (2.8 a): hace lo mismo que el "Sí" de la invitación. */
  function seguirMiHistoria() {
    if (objetivoRef.current !== null || hayAlgoEnCurso()) return
    limpiarError()
    agregarAccion(SEGUIR_MI_HISTORIA)
    cerrarInvitacion()
    void conBloqueo(irAProxima)
  }

  function volverAHistoria() {
    limpiarError()
    // La pregunta (o repregunta) sin responder sale del chat: sigue pendiente en el back y se vuelve a
    // ofrecer por la tarjeta o la invitación. Lo escrito en el campo no se toca.
    retirarBloqueSinResponder()
    fijarObjetivo(null)
    preguntaOrigenRef.current = null
    recuerdoEnCursoRef.current = null
    datosPreguntadosRef.current = []
    recuerdosAbiertosRef.current.clear()
    setCierrePendiente(false)
    // Al volver se ven la etapa, el año y los datos pendientes actualizados (2.7, punto 6).
    invalidarHistoria()
    navegar(RUTA_HISTORIA_DE_VIDA)
  }

  const valor: ChatContextoValor = {
    animo,
    cambiandoAnimo,
    yaEscribio,
    mensajes,
    pendiente: pendiente || esperandoRepregunta,
    error,
    errorReintentable,
    borrador,
    anuncioAnimo,
    cambiarBorrador: setBorrador,
    enviar: (texto) => void enviar(texto),
    reintentar: () => accionDeReintentoRef.current?.(),
    alRegistrarAnimo: (registro) => {
      // Si ya había un registro, la elección lo corrigió (PUT): se reemplaza, no se agrega nada.
      setAnuncioAnimo(
        animo ? anuncioCorregido(registro.nivelAnimo) : anuncioRegistrado(registro.nivelAnimo)
      )
      setAnimo(registro)
      setCambiandoAnimo(false)
    },
    empezarACambiarAnimo: () => setCambiandoAnimo(true),
    objetivoActivo,
    cierrePendiente,
    invitacionPendiente,
    ocupadoGuion,
    abrirPreguntaDelGuion,
    seguirCompletando,
    mostrarBotonSeguirHistoria: objetivoActivo === null && !huboAlarma && (estadoGuion.data?.pendientes ?? 0) > 0,
    seguirMiHistoria,
    aceptarInvitacion,
    rechazarInvitacion,
    otraPregunta,
    otraPreguntaDespuesDelCierre,
    contarAlgoMas,
    saltearRepregunta,
    volverAHistoria,
    campoDeMensaje,
    enfocarCampo: () => campoDeMensaje.current?.focus(),
  }

  return <ChatContexto.Provider value={valor}>{children}</ChatContexto.Provider>
}
