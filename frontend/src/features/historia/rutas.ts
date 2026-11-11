// Rutas de Historia de vida (contrato de historia de vida, 11.1). El chat está en "/" (sección 3).
export const RUTA_HISTORIA_DE_VIDA = "/historia-de-vida"
export const RUTA_DETALLE_DE_RECUERDO = "/historia-de-vida/:recuerdoId"
export const rutaDeRecuerdo = (id: string) => `${RUTA_HISTORIA_DE_VIDA}/${id}`
export const RUTA_CHAT = "/"
