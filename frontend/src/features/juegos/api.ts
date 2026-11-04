// Depende de /api/juegos del contrato (docs/contrato-juegos.md, sección 6).
import { api } from "@/lib/api"

import type { RespuestaCuerpo, ResultadoRespuesta, SiguienteEjercicio } from "./tipos"

const BASE = "/juegos"

/** 6.1: el próximo ejercicio y la posición en la ronda. 404 si no hay ningún ejercicio activo. */
export function obtenerSiguiente(): Promise<SiguienteEjercicio> {
  return api<SiguienteEjercicio>(`${BASE}/siguiente`)
}

/** 6.2: valida la respuesta, registra el intento y devuelve el resultado. */
export function enviarRespuesta(cuerpo: RespuestaCuerpo): Promise<ResultadoRespuesta> {
  return api<ResultadoRespuesta>(`${BASE}/respuesta`, { method: "POST", cuerpo })
}
