// Tipos según las respuestas del contrato (docs/contrato-recordatorios.md, sección 6).
// Fechas como texto tal como llegan: "yyyy-MM-dd", "yyyy-MM-ddTHH:mm:ss" y "HH:mm:ss".

export type TipoRecordatorio = "MEDICACION" | "CITA"
export type FrecuenciaToma = "DIARIA" | "DIA_POR_MEDIO" | "SEMANAL"
export type EstadoNotificacion = "PENDIENTE" | "TOMADA" | "VISTA"

export type Medicacion = {
  id: number
  tipo: "MEDICACION"
  fechaCreacion: string
  nombreMedicamento: string
  dosis: string
  horaToma: string
  frecuencia: FrecuenciaToma
  fechaInicio: string
  duracionDias: number
}

/** Una cita tiene una sola notificación: su id y su estado vienen con ella. */
export type Cita = {
  id: number
  tipo: "CITA"
  fechaCreacion: string
  titulo: string
  especialidad: string
  medico: string
  lugar: string
  fechaHora: string
  indicacionesPrevias: string | null
  notificacionId: number
  estado: EstadoNotificacion
}

export type Recordatorio = Medicacion | Cita

/** Una notificación de la agenda con los datos de su recordatorio (6.1). */
export type ItemAgenda = {
  notificacionId: number
  fechaHoraProgramada: string
  estado: EstadoNotificacion
  fechaHoraToma: string | null
  atrasada: boolean
  recordatorio: Recordatorio
}

/** Lo que se envía al crear o editar una medicación (5.8). */
export type DatosMedicacion = {
  nombreMedicamento: string
  dosis: string
  horaToma: string
  frecuencia: FrecuenciaToma
  fechaInicio?: string
  duracionDias: number
}

/** Lo que se envía al crear o editar una cita (5.8). */
export type DatosCita = {
  titulo: string
  especialidad: string
  medico: string
  lugar: string
  fechaHora: string
  indicacionesPrevias?: string
}
