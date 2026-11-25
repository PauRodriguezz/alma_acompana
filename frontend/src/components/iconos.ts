import { BookOpen02Icon, PencilEdit01Icon, UniversalAccessIcon } from "@hugeicons/core-free-icons"

// Íconos que se usan en más de un archivo, con el nombre de Hugeicons que usa el Figma (página
// Componentes, "Íconos Hugeicons (selección)"). Los de los destinos del menú están en layout/destinosMenu.ts.
// Los íconos que usa un solo archivo se importan ahí directamente.
//   - universal-access: Accesibilidad (botones de accesibilidad y filas de los dos Perfil)
//   - pencil-edit-01: Lápiz / Editar (listas de Ejercicios y Microcápsulas)
//   - book-open-02: Historia de vida (menú y botón "Seguir mi historia de vida")
// Volver y Siguiente usan ArrowLeft01Icon y ArrowRight01Icon: el Figma los llama arrow-left-01-round y
// arrow-right-01-round, y ese nombre no existe en el paquete gratuito.
export const ICONO_ACCESIBILIDAD = UniversalAccessIcon
export const ICONO_EDITAR = PencilEdit01Icon
export const ICONO_HISTORIA_DE_VIDA = BookOpen02Icon
