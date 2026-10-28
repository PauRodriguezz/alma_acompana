const CLAVE = "alma.token"

// Si el navegador no deja usar storage (modo privado, datos bloqueados), la sesión sigue en memoria.
let tokenEnMemoria: string | null = null

function leerDe(almacen: () => Storage): string | null {
  try {
    // Una clave con valor vacío no es un token: se trata como si no hubiera sesión.
    return almacen().getItem(CLAVE) || null
  } catch {
    return null
  }
}

export function leerToken(): string | null {
  return (
    leerDe(() => localStorage) ??
    leerDe(() => sessionStorage) ??
    tokenEnMemoria
  )
}

/** Con "Mantener sesión iniciada" el token va a localStorage; si no, a sessionStorage. */
export function guardarToken(token: string, persistente: boolean): void {
  borrarToken()
  tokenEnMemoria = token
  try {
    const almacen = persistente ? localStorage : sessionStorage
    almacen.setItem(CLAVE, token)
  } catch {
    // sigue en memoria
  }
}

export function borrarToken(): void {
  tokenEnMemoria = null
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    // nada que borrar
  }
  try {
    sessionStorage.removeItem(CLAVE)
  } catch {
    // nada que borrar
  }
}
