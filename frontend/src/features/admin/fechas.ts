// "Hoy" con la hora de Argentina, la misma que usa el back (Fechas.hoy(), contrato del panel
// sección 3.3), sin importar la zona horaria de la computadora.
const formatoArgentina = new Intl.DateTimeFormat("es-AR", {
  timeZone: "America/Argentina/Buenos_Aires",
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
})

/** Fecha de hoy en Argentina como "yyyy-MM-dd" (el formato de la API y de <input type="date">). */
export function hoyEnArgentina(): string {
  const partes = formatoArgentina.formatToParts(new Date())
  const parte = (tipo: Intl.DateTimeFormatPartTypes) =>
    partes.find((p) => p.type === tipo)?.value ?? ""
  return `${parte("year")}-${parte("month")}-${parte("day")}`
}
