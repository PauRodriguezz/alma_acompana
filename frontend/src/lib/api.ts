import { borrarToken, leerToken } from "@/lib/token"

const BASE = "/api"
const RUTA_LOGIN = "/auth/login"

// Tiempo máximo de cada llamada (incluye leer la respuesta). Al vencerse se trata como
// "sin conexión": ApiError con status 0.
const TIEMPO_MAXIMO_MS = 10_000

export const MENSAJE_SESION_VENCIDA = "Tu sesión terminó. Ingresá de nuevo."

type Problema = {
  title?: string
  detail?: string
  errores?: { campo: string; mensaje: string }[]
}

/** Error de la API en formato Problem Details (RFC 9457). status 0 = no hubo respuesta. */
export class ApiError extends Error {
  readonly status: number
  readonly titulo?: string
  readonly detalle?: string
  readonly errores?: { campo: string; mensaje: string }[]

  constructor(status: number, problema: Problema = {}) {
    super(problema.detail ?? problema.title ?? `Error ${status}`)
    this.name = "ApiError"
    this.status = status
    this.titulo = problema.title
    this.detalle = problema.detail
    this.errores = problema.errores
  }
}

// El AuthProvider se registra acá para limpiar la sesión y mandar a /login cuando un 401 la corta.
let alVencerSesion: (() => void) | null = null

export function registrarSesionVencida(fn: () => void): () => void {
  alVencerSesion = fn
  return () => {
    if (alVencerSesion === fn) alVencerSesion = null
  }
}

async function leerProblema(respuesta: Response): Promise<Problema> {
  try {
    return (await respuesta.json()) as Problema
  } catch {
    return {}
  }
}

type Opciones = {
  method?: "GET" | "POST" | "PUT"
  cuerpo?: unknown
}

export async function api<T>(
  ruta: string,
  { method = "GET", cuerpo }: Opciones = {}
): Promise<T> {
  // El login es público: no se le manda un token viejo (el back lo rechazaría con 401).
  const token = ruta === RUTA_LOGIN ? null : leerToken()

  const headers = new Headers({ Accept: "application/json" })
  if (cuerpo !== undefined) headers.set("Content-Type", "application/json")
  if (token) headers.set("Authorization", `Bearer ${token}`)

  let respuesta: Response
  try {
    respuesta = await fetch(`${BASE}${ruta}`, {
      method,
      headers,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
      signal: AbortSignal.timeout(TIEMPO_MAXIMO_MS),
    })
  } catch {
    throw new ApiError(0)
  }

  if (!respuesta.ok) {
    if (respuesta.status === 401 && token) {
      borrarToken()
      alVencerSesion?.()
    }
    throw new ApiError(respuesta.status, await leerProblema(respuesta))
  }

  if (respuesta.status === 204) return undefined as T
  try {
    return (await respuesta.json()) as T
  } catch {
    // Se cortó o venció el tiempo mientras llegaba el cuerpo
    throw new ApiError(0)
  }
}
