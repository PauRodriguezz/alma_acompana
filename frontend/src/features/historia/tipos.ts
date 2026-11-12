// Depende de /api/historia (docs/contrato-historia-de-vida.md, sección 7, y docs/contrato-grafo.md, sección 7).

/** Cuán segura es la fecha de un recuerdo (2.6.1). Puede llegar null si el grafo trae un valor desconocido. */
export type PrecisionAnio = "EXACTA" | "CALCULADA_POR_EDAD" | "APROXIMADA" | "ESTIMADA"

export type EtapaDeVida = "INFANCIA" | "ADOLESCENCIA" | "JUVENTUD" | "ADULTEZ" | "VEJEZ"

/** Una fila de GET /api/historia/recuerdos (7.3). */
export type RecuerdoResumen = {
  /** UUID. */
  id: string
  titulo: string
  anio: number | null
  precision: PrecisionAnio | null
  etapaDeVida: EtapaDeVida | null
  resumen: string
  /** Datos que se pueden seguir completando (contrato del mapa de vida, 3.1); vacío si no hay ninguno. */
  faltantes: DatoFaltante[]
}

export type ListaDeRecuerdos = {
  recuerdos: RecuerdoResumen[]
}

export type PersonaDeRecuerdo = {
  id: string
  nombre: string
  vinculo: string | null
}

export type LugarDeRecuerdo = {
  id: string
  nombre: string
}

/** GET /api/historia/recuerdos/{recuerdoId} (7.4). */
export type RecuerdoDetalle = {
  id: string
  titulo: string
  descripcion: string
  anio: number | null
  precision: PrecisionAnio | null
  etapaDeVida: EtapaDeVida | null
  personas: PersonaDeRecuerdo[]
  lugares: LugarDeRecuerdo[]
  /** Igual que en el listado (3.2). */
  faltantes: DatoFaltante[]
  /** Texto de la pregunta del guion de la que surgió el recuerdo; null si no se encuentra (3.2). */
  preguntaTexto: string | null
}

/** GET /api/historia/guion/estado (7.2 del contrato del grafo). Acá solo se usan los totales. */
export type EstadoDelGuion = {
  respondidas: number
  salteadas: number
  pendientes: number
}

// ── Guion y repreguntas (contrato del grafo, sección 7; contrato de historia de vida, 7.1 y 7.2) ──

/** El dato de un recuerdo por el que se repregunta. */
export type DatoFaltante = "ANIO" | "LUGARES" | "PERSONAS"

export type PreguntaDelGuion = {
  id: number
  orden: number
  texto: string
}

/** GET /guion/proxima. Sin pregunta pendiente, `pregunta` e `introduccion` vienen null y `mensajeFinal` trae el cierre. */
export type ProximaPregunta = {
  pregunta: PreguntaDelGuion | null
  introduccion: string | null
  mensajeFinal: string | null
  quedanPendientes: boolean
}

/** POST .../respuesta: 202 con ACEPTADA o 200 con ALARMA (el texto predefinido del chat). */
export type RespuestaDelGuion =
  | { estado: "ACEPTADA" }
  | { estado: "ALARMA"; respuesta: string }

export type EstadoDeRepregunta = "PROCESANDO" | "HAY" | "NINGUNA"

export type Repregunta = {
  recuerdoId: string
  dato: DatoFaltante
  texto: string
  /** Título del recuerdo al que corresponde (contrato del mapa de vida, 3.3); puede ser null. */
  tituloRecuerdo?: string | null
}

/** GET /repreguntas/siguiente. `repregunta` solo viene con HAY. */
export type RepreguntaSiguiente = {
  estado: EstadoDeRepregunta
  repregunta: Repregunta | null
}
