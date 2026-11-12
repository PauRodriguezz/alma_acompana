// Textos de Historia de vida (contrato, sección 6). Los generales (cargando, probar de nuevo,
// no encontrado, nombre de la navegación) están en lib/mensajes.ts.

export const TITULO_HISTORIA_DE_VIDA = "Historia de vida"

// Mapa de vida (contrato del mapa de vida, sección 4)
export const SUBTITULO_HISTORIA = "Tu camino, etapa por etapa"
export const NOMBRE_DEL_MAPA = "Mapa de tu historia de vida"
export const AVISO_TOCAR_BANDERA = "Tocá una bandera para ver sus recuerdos"
export const ETAPA_SIN_RECUERDOS = "Todavía sin recuerdos"
export const masRecuerdos = (cantidad: number) => `y ${cantidad} más`
export const cantidadDeRecuerdos = (cantidad: number) =>
  cantidad === 0 ? "Sin recuerdos" : cantidad === 1 ? "1 recuerdo" : `${cantidad} recuerdos`
/** Nombre accesible de cada parada: "Infancia, 3 recuerdos" / "Infancia, sin recuerdos". */
export const nombreDeParada = (etapa: string, cantidad: number) =>
  `${etapa}, ${cantidad === 0 ? "sin recuerdos" : cantidadDeRecuerdos(cantidad)}`
export const PANEL_SIN_RECUERDOS = "Cuando cuentes algo de esta etapa, aparece acá."
export const CERRAR_PANEL = "Cerrar"

// Línea de tiempo
export const SIN_RECUERDOS =
  "Todavía no tenemos recuerdos. Respondé las preguntas de Alma y van a aparecer acá."
export const PREFIJO_ANIO_APROXIMADO = "Alrededor de"

// Tarjeta de pregunta pendiente (B3)
export const PROXIMA_PARADA = "Próxima parada"
export const ALMA_TIENE_UNA_PREGUNTA = "Alma tiene una pregunta"
export const SEGUIR_ESCRIBIENDO = "Seguir escribiendo"

// Detalle
export const CON_QUIEN = "Con quién"
export const DONDE = "Dónde"

// Modo guion en el chat (contrato, sección 6)
export const INVITACION_TEXTO = "¿Seguimos completando tu historia de vida?"
export const INVITACION_SI = "Sí"
export const INVITACION_AHORA_NO = "Ahora no"
export const AYUDA_PREGUNTA = "Contame con calma. Si hoy no tenés ganas de esa, probamos con otra."
export const OTRA_PREGUNTA = "Otra pregunta"
export const VOLVER_A_HISTORIA = "Volver a Historia de vida"
export const SALTEAR = "Saltear"
export const RESPUESTA_VACIA = "Escribí una respuesta o elegí saltear la pregunta."
export const CIERRE_SIN_REPREGUNTA = "¿Querés contarme algo más sobre esto o te hago otra pregunta?"
export const CONTARTE_ALGO_MAS = "Contarte algo más"
/** Texto de ayuda del campo en modo guion (Figma 29:399). */
export const ESCRIBI_TU_RESPUESTA = "Escribí tu respuesta"

// Chat: botón fijo, invitación a pedido y aperturas de repregunta (contrato del mapa de vida, 2.8 y 2.11)
export const SEGUIR_MI_HISTORIA = "Seguir mi historia de vida"
export const YA_RESPONDISTE_TODO =
  "Ya respondiste todas las preguntas de tu historia. Podés ver tus recuerdos en Historia de vida."
export const VER_MI_HISTORIA = "Ver mi historia de vida"
/** Apertura cuando no se puede citar el título del recuerdo (vacío o con forma de pregunta). */
export const APERTURA_SIN_TITULO = "Gracias por contármelo."
/** Se rotan, una por recuerdo y por charla. Se ajustan acá sin tocar nada más. */
export const APERTURAS_DE_REPREGUNTA: readonly ((titulo: string) => string)[] = [
  (titulo) => `Gracias por contármelo. Quiero saber un poco más de «${titulo}».`,
  (titulo) => `Me gustó lo que contaste. Sigamos con «${titulo}».`,
  (titulo) => `Gracias por compartirlo. Un detalle más sobre «${titulo}».`,
]

// Seguir completando un recuerdo (contrato del mapa de vida, 2.6 y 2.7)
export const SEGUIR_COMPLETANDO = "Seguir completando este recuerdo"
export const SIN_MAS_PARA_PREGUNTAR = "Por ahora no tengo más para preguntarte de este recuerdo."
export const respondisteA = (pregunta: string) => `Respondiste a: «${pregunta}»`

// Descarga del mapa (contrato del mapa de vida, 2.5)
export const DESCARGAR = "Descargar"
/** Nombre accesible del botón: contiene el texto visible. */
export const DESCARGAR_MI_MAPA = "Descargar mi mapa"
export const ERROR_AL_CREAR_LA_IMAGEN = "No pudimos crear la imagen. Probá de nuevo."
export const TITULO_IMAGEN_PREFIJO = "La vida de"
/** Título de la imagen cuando no se conoce el nombre de la persona (sin cursiva). */
export const TITULO_IMAGEN_SIN_NOMBRE = "Mi vida"
export const PIE_DE_LA_IMAGEN = "Alma Acompaña"
export const ALMA_TODAVIA_RESPONDE = "Alma todavía está respondiendo. Probá de nuevo en un momento."

// Respuestas fijas de Alma a un botón (contrato del mapa de vida, 2.9): sin llamar al modelo, rotan una por vez.
export const RESPUESTAS_CONTARTE_ALGO_MAS: readonly string[] = [
  "Dale, contame lo que quieras. Te escucho.",
  "Claro, te escucho. Contame con calma.",
  "Con gusto. Contame lo que tengas ganas.",
]
export const RESPUESTAS_AHORA_NO: readonly string[] = [
  "Está bien, cuando quieras seguimos.",
  "Sin problema. Acá estoy para lo que necesites.",
  "Está bien. Cuando tengas ganas, lo retomamos.",
]
