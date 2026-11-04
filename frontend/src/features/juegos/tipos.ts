// Tipos según las respuestas del contrato (docs/contrato-juegos.md, sección 6).
// Nunca llegan esCorrecta, ordenCorrecto ni el nivel: la respuesta la valida el back.
import type { CategoriaEjercicio, FormatoEjercicio } from "@/features/admin/etiquetas"

export type { CategoriaEjercicio, FormatoEjercicio }

/** Opción (o paso, en ORDENAR). imagen: null o una ruta "/imagenes/…". */
export type OpcionJuego = {
  id: number
  texto: string
  imagen: string | null
}

export type EjercicioJuego = {
  id: number
  categoria: CategoriaEjercicio
  formato: FormatoEjercicio
  consigna: string
  /** Solo en MEMORIA; en el resto, null. */
  textoAMemorizar: string | null
  /** Ya vienen mezcladas; en ORDENAR, nunca en el orden correcto. */
  opciones: OpcionJuego[]
}

/** 6.1: GET /api/juegos/siguiente. posicion va de 1 a totalRonda. */
export type SiguienteEjercicio = {
  ejercicio: EjercicioJuego
  posicion: number
  totalRonda: number
}

/** 6.2: cuerpo de POST /api/juegos/respuesta, según el formato (5.6). */
export type RespuestaCuerpo =
  | { ejercicioId: number; opcionId: number }
  | { ejercicioId: number; ordenOpcionIds: number[] }

/** 6.2: resultado, tanto en acierto como en error. */
export type ResultadoRespuesta = {
  acierto: boolean
  /** Opcional en el catálogo. */
  explicacion: string | null
  /** Un texto en opción múltiple; los pasos en el orden correcto en ORDENAR. */
  respuestaCorrecta: string[]
  rondaCompleta: boolean
  jugadosHoy: number
}
