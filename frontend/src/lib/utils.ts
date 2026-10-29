import { createCn } from "cn/config"

// Tamaños de letra propios del Figma (--text-* en index.css). Sin declararlos, cn los toma por
// colores de texto: en la misma llamada con un color (text-foreground, text-heading...) descarta
// uno de los dos, y el botón, por ejemplo, perdía text-button. Si se agrega un --text-* nuevo en
// index.css, hay que sumarlo acá.
const TAMANIOS_PROPIOS = [
  "h1",
  "h2",
  "h3",
  "h1-desktop",
  "h2-desktop",
  "body",
  "label",
  "caption",
  "button",
]

export const cn = createCn({
  extend: { classGroups: { "font-size": [{ text: TAMANIOS_PROPIOS }] } },
})
