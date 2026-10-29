// Textos del módulo de recordatorios (contrato, secciones 5.8 y 8.1, y los aprobados para el front).
// Los generales (sesión terminada, sin conexión, etc.) están en lib/mensajes.ts.

import { CALENDARIO_HOY } from "@/lib/mensajes"

import type { FrecuenciaToma } from "./tipos"

// --- Pantallas y secciones ---
export const TITULO_RECORDATORIOS = "Recordatorios"
export const PESTANIA_LISTA = "Lista"
export const PESTANIA_CALENDARIO = "Calendario"
export const BOTON_NUEVO = "Nuevo recordatorio"
export const TITULO_MEDICACION_DE_HOY = "Medicación de hoy"
export const TITULO_CITAS = "Próximas citas médicas"
export const TITULO_MIS_MEDICACIONES = "Medicaciones cargadas"
export const SIN_MEDICACION_HOY = "Hoy no tenés medicación para tomar."
export const SIN_CITAS = "No tenés citas próximas."
export const SIN_MEDICACIONES = "Todavía no tenés medicaciones cargadas."
export const CARGANDO = "Cargando…"

// --- Etiquetas de estado y de tipo ---
export const CHIP_MEDICACION = "Medicación"
export const CHIP_CITA = "Cita médica"
export const CHIP_PENDIENTE = "Pendiente"
export const CHIP_VISTA = "Vista"
export const CHIP_TOMADA = "Tomada"
export const BOTON_TOMADA = "Ya la tomé"
export const BOTON_DESHACER = "Deshacer"
export const BOTON_EDITAR = "Editar"

export const ETIQUETA_FRECUENCIA: Record<FrecuenciaToma, string> = {
  DIARIA: "Todos los días",
  DIA_POR_MEDIO: "Un día por medio",
  SEMANAL: "Una vez por semana",
}

export function tomadaALas(hora: string): string {
  return `Tomada a las ${hora}`
}

export function pasoHace(tiempo: string): string {
  return `Pasó hace ${tiempo}`
}

// --- Para el lector de pantalla ---
export function nombreBotonTomada(nombre: string): string {
  return `${BOTON_TOMADA}: ${nombre}`
}

export function nombreBotonDeshacer(nombre: string): string {
  return `${BOTON_DESHACER}: ${nombre}`
}

export function anuncioTomada(nombre: string): string {
  return `Marcaste «${nombre}» como tomada.`
}

export function anuncioDeshecha(nombre: string): string {
  return `Deshiciste la toma de «${nombre}».`
}

// --- Tarjeta "Para hoy" ---
export const PARA_HOY = "Para hoy"
export const CERRAR_AVISO = "Cerrar aviso"

export function verLos(cantidad: number): string {
  return `Ver los ${cantidad}`
}

// --- Calendario ---
export const LEYENDA_PUNTO = "El punto marca un día con una cita."
export const SIN_AVISOS_DEL_DIA = "Sin avisos para este día."
export const NOMBRE_DIA_CON_CITA = "tiene una cita"
export const VER_EL_MES = "Ver el mes"
export const SEMANA_ANTERIOR = "Semana anterior"
export const SEMANA_SIGUIENTE = "Semana siguiente"
export const NOMBRE_TIRA_SEMANA = "Días de la semana"
export const NOMBRE_LINEA_DE_TIEMPO = "Avisos del día, por horario"

/** "Hoy, jueves 29 de octubre" si es hoy; si no, solo la fecha ("Jueves 29 de octubre"). */
export function tituloDelDia(esHoy: boolean, fecha: string): string {
  return esHoy ? `${CALENDARIO_HOY}, ${fecha.toLowerCase()}` : fecha
}

/** Para el lector de pantalla, al plegar el mes a la semana o al cambiar de semana. */
export function anuncioSemana(desde: string, hasta: string, dia: string): string {
  return `Semana del ${desde} al ${hasta}. Mostrando el ${dia.toLowerCase()}.`
}

/** Para el lector de pantalla, al volver al mes completo. */
export function anuncioMes(mes: string): string {
  return `Mostrando el mes de ${mes.toLowerCase()}.`
}

// --- Formularios (alta y edición) ---
export const VOLVER_A_RECORDATORIOS = "Recordatorios"
export const TITULO_EDITAR = "Editar recordatorio"
export const TIPO_DE_RECORDATORIO = "Tipo de recordatorio"
export const OPCION_MEDICACION = "Medicación"
export const OPCION_CITA = "Cita médica"
export const GUARDAR = "Guardar recordatorio"
export const GUARDANDO = "Guardando…"

export const ETIQUETA_NOMBRE_MEDICAMENTO = "Nombre del medicamento"
export const ETIQUETA_DOSIS = "Dosis"
export const ETIQUETA_HORA = "Hora"
export const ETIQUETA_FRECUENCIA_TITULO = "¿Cada cuánto la tomás?"
export const ETIQUETA_DURACION = "Duración"
export const UNIDAD_DIAS = "días"
export const MENOS_DIAS = "Menos días"
export const MAS_DIAS = "Más días"
export const ETIQUETA_TITULO = "Título"
export const ETIQUETA_ESPECIALIDAD = "Especialidad"
export const ETIQUETA_MEDICO = "Médico"
export const ETIQUETA_LUGAR = "Lugar"
export const ETIQUETA_FECHA = "Fecha"
// El "(opcional)" lo agrega CampoAreaTexto con la propiedad `opcional`.
export const ETIQUETA_INDICACIONES = "Indicaciones previas"

export const EJEMPLO_NOMBRE_MEDICAMENTO = "Ej.: Enalapril"
export const EJEMPLO_DOSIS = "Ej.: 1 pastilla"
export const EJEMPLO_TITULO = "Ej.: Control anual"
export const EJEMPLO_ESPECIALIDAD = "Ej.: Cardiología"
export const EJEMPLO_MEDICO = "Ej.: Dra. Pérez"
export const EJEMPLO_LUGAR = "Ej.: Consultorio, centro"

// Avisos de arriba del formulario cuando la API da 400 (contrato, 8.1)
export const AVISO_MEDICACION_INVALIDA = "Revisá los datos del recordatorio e intentá nuevamente."
export const AVISO_CITA_INVALIDA = "Revisá los datos de la cita e intentá nuevamente."

// Confirmaciones (contrato, 8.1)
export const AVISO_CREADO = "Tu recordatorio quedó creado."
export const AVISO_ACTUALIZADO = "Tu recordatorio quedó actualizado."
export const AVISO_ELIMINADO = "Tu recordatorio fue eliminado."

// Eliminar (contrato, 8.1)
export const BOTON_ELIMINAR = "Eliminar"
export const BOTON_ELIMINAR_RECORDATORIO = "Eliminar recordatorio"
export const ELIMINAR_PREGUNTA = "¿Querés eliminar este recordatorio?"
export const ELIMINAR_AVISO = "También se van a borrar sus avisos."
export const ELIMINAR_CONFIRMAR = "Sí, eliminar"

// Detalle de la cita
export const ETIQUETA_INDICACIONES_DETALLE = "Indicaciones previas"

// --- Validaciones (contrato, 5.8): los mismos textos que da la API ---
export const NOMBRE_OBLIGATORIO = "Escribí el nombre del medicamento."
export const NOMBRE_LARGO = "El nombre es demasiado largo."
export const DOSIS_OBLIGATORIA = "Indicá la dosis."
export const DOSIS_LARGA = "La dosis es demasiado larga."
export const HORA_OBLIGATORIA = "Elegí la hora."
export const FRECUENCIA_OBLIGATORIA = "Elegí cada cuánto la tomás."
export const DURACION_OBLIGATORIA = "Indicá cuántos días."
export const DURACION_FUERA_DE_RANGO = "Los días tienen que ser entre 1 y 365."
export const TITULO_OBLIGATORIO = "Escribí un título."
export const TITULO_LARGO = "El título es demasiado largo."
export const ESPECIALIDAD_OBLIGATORIA = "Escribí la especialidad."
export const ESPECIALIDAD_LARGA = "La especialidad es demasiado larga."
export const MEDICO_OBLIGATORIO = "Escribí el nombre del médico."
export const MEDICO_LARGO = "El nombre del médico es demasiado largo."
export const LUGAR_OBLIGATORIO = "Escribí el lugar."
export const LUGAR_LARGO = "El lugar es demasiado largo."
export const FECHA_HORA_OBLIGATORIA = "Elegí la fecha y la hora."
export const FECHA_HORA_PASADA = "La fecha y la hora tienen que ser posteriores a este momento."
export const INDICACIONES_LARGAS = "Las indicaciones son demasiado largas."
