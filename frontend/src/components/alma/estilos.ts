// PENDIENTE (arreglo de cn, coordinarlo aparte): cn descarta text-button en <Button> porque en
// la misma llamada hay un color de texto, y el botón queda a 17 px en lugar de 18,1 px
// (alma/button del Figma). Mientras tanto, los <Button> del panel suman esta clase: el
// tamaño escrito como text-(length:…) no se descarta. Los enlaces con buttonVariants() no la
// necesitan (no pasan por cn). Cuando se arregle cn, se puede quitar.
export const tamanioTextoBoton =
  "text-(length:--text-button) leading-(--text-button--line-height)"
