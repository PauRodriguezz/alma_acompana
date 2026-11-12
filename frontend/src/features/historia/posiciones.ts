// Posiciones de las paradas del mapa, en unidades de diseño de un ancho de 360 (contrato del mapa de vida, Anexo A).

/** Altura (en unidades) entre una parada y la siguiente, y posición de la primera. */
export const PRIMERA_PARADA_Y = 70
export const SEPARACION_ENTRE_PARADAS = 120

export function posicionDeParada(indice: number) {
  const aLaDerecha = indice % 2 === 0
  return {
    y: PRIMERA_PARADA_Y + indice * SEPARACION_ENTRE_PARADAS,
    /** Lado donde se ve la bandera: "der" = punto a la izquierda y bandera hacia la derecha. */
    lado: aLaDerecha ? ("der" as const) : ("izq" as const),
    xPunto: aLaDerecha ? 96 : 264,
    xIlustracion: aLaDerecha ? 46 : 314,
  }
}
