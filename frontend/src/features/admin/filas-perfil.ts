// Filas del Perfil del administrador: los mismos estilos que el Perfil de la persona mayor
// (features/perfil/PerfilPage.tsx), copiados para no tocar ese módulo. Borde inferior, 12 px
// arriba y abajo; las filas que son botón miden al menos 56 px.
// PENDIENTE: unificar las filas de Perfil de la persona mayor y del administrador en un
// componente compartido.

export const FILA = "flex w-full items-center gap-3 border-b-2 border-border py-3"

export const FILA_BOTON = `${FILA} min-h-[calc(56rem/17)] text-left font-semibold transition-colors hover:bg-secondary`
