import {
  BookOpen02Icon,
  Heading01Icon,
  Heading02Icon,
  LeftToRightListBulletIcon,
  LeftToRightListNumberIcon,
  Location01Icon,
  PencilEdit01Icon,
  QuoteDownIcon,
  TextBoldIcon,
  TextItalicIcon,
  UniversalAccessIcon,
  UserGroupIcon,
} from "@hugeicons/core-free-icons"

// Íconos que se usan en más de un archivo, con el nombre de Hugeicons que usa el Figma (página
// Componentes, "Íconos Hugeicons (selección)"). Los de los destinos del menú están en destinos-menu.ts.
// Los íconos que usa un solo archivo se importan ahí directamente.
//   - universal-access: Accesibilidad (botones de accesibilidad y filas de los dos Perfil)
//   - pencil-edit-01: Lápiz / Editar (listas de Ejercicios y Microcápsulas)
//   - book-open-02: Historia de vida (menú y botón "Seguir mi historia de vida")
// Volver y Siguiente usan ArrowLeft01Icon y ArrowRight01Icon: el Figma los llama arrow-left-01-round y
// arrow-right-01-round, y ese nombre no existe en el paquete gratuito.
export const ICONO_ACCESIBILIDAD = UniversalAccessIcon
export const ICONO_EDITAR = PencilEdit01Icon
export const ICONO_HISTORIA_DE_VIDA = BookOpen02Icon

// Barra de formato del texto del panel de operador (features/microcapsulas/BarraFormato.tsx). Es la
// única barra de solo íconos del panel: cada botón lleva aria-label y una etiqueta al pasar el mouse o
// enfocar (excepción documentada en contrato-panel-operador.md). Nombres del paquete gratuito:
//   - heading-01 / heading-02: Título (##) y Subtítulo (###)
//   - text-bold / text-italic: Negrita y Cursiva
//   - left-to-right-list-bullet / left-to-right-list-number: Lista con viñetas y numerada
//   - quote-down: Destacado (cita)
export const ICONO_FORMATO_TITULO = Heading01Icon
export const ICONO_FORMATO_SUBTITULO = Heading02Icon
export const ICONO_FORMATO_NEGRITA = TextBoldIcon
export const ICONO_FORMATO_CURSIVA = TextItalicIcon
export const ICONO_FORMATO_LISTA = LeftToRightListBulletIcon
export const ICONO_FORMATO_LISTA_NUMERADA = LeftToRightListNumberIcon
export const ICONO_FORMATO_DESTACADO = QuoteDownIcon

// Detalle de un recuerdo (features/historia/pages/RecuerdoDetallePage.tsx): chips de "Con quién" y "Dónde".
//   - user-group: personas del recuerdo
//   - location-01: lugares del recuerdo
export const ICONO_PERSONAS = UserGroupIcon
export const ICONO_LUGAR = Location01Icon
