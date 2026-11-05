// La pestaña de Leer viaja en la URL (contrato, sección 2): "?ver=guardadas" abre "Guardadas".
// El detalle lleva el mismo parámetro para que "‹ Leer" vuelva a la pestaña de origen.

export const PARAMETRO_PESTANIA = "ver"

export type Pestania = "todas" | "guardadas"

/** La pestaña según la URL: cualquier otro valor (o ninguno) es "todas". */
export function pestaniaDe(parametros: URLSearchParams): Pestania {
  return parametros.get(PARAMETRO_PESTANIA) === "guardadas" ? "guardadas" : "todas"
}

/** La ruta con "?ver=guardadas" si corresponde; "todas" no lleva parámetro. */
export function conPestania(ruta: string, pestania: Pestania): string {
  return pestania === "guardadas" ? `${ruta}?${PARAMETRO_PESTANIA}=guardadas` : ruta
}
