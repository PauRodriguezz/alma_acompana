// Rutas de Entretenimiento y de lo que cuelga de ella (contrato de juegos, 10.1).
export const RUTA_ENTRETENIMIENTO = "/entretenimiento"
export const RUTA_JUEGOS = "/entretenimiento/juegos"
export const RUTA_JUGAR = "/entretenimiento/juegos/jugar"
export const RUTA_LECTURAS = "/entretenimiento/lecturas"
// Detalle de una lectura (contrato de microcápsulas, 10.1)
export const RUTA_LECTURA = "/entretenimiento/lecturas/:id"
export const rutaLectura = (id: number) => `${RUTA_LECTURAS}/${id}`
