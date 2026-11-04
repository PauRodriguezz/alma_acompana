// Las caritas del Figma ("Carita de ánimo"), como SVG. Se usan con alt="": la etiqueta de texto
// ya las nombra. Las comparten CaritaAnimo (widget) y BurbujaAnimo.
import bien from "@/assets/animo/bien.svg"
import mal from "@/assets/animo/mal.svg"
import muyBien from "@/assets/animo/muy-bien.svg"
import muyMal from "@/assets/animo/muy-mal.svg"
import neutro from "@/assets/animo/neutro.svg"

import type { NivelAnimo } from "./tipos"

export const ILUSTRACIONES_ANIMO: Record<NivelAnimo, string> = {
  MUY_MAL: muyMal,
  MAL: mal,
  NEUTRO: neutro,
  BIEN: bien,
  MUY_BIEN: muyBien,
}
