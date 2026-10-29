// Textos del panel de operador que comparten sus pantallas (y las de ejercicios y
// microcápsulas). Un texto se escribe una sola vez, acá, y el resto del código lo importa.
// Los generales (cargando, cancelar, errores...) están en lib/mensajes.ts.

export const ADMIN_PANEL = "Panel de operador"
export const ADMIN_EJERCICIOS = "Ejercicios"
export const ADMIN_MICROCAPSULAS = "Microcápsulas"
export const ADMIN_PERFIL = "Perfil"

// Layout y menú
export const ADMIN_SALTAR_AL_CONTENIDO = "Saltar al contenido"
export const ADMIN_MARCA = "Alma"
export const ADMIN_MENU = "Menú"
export const ADMIN_CERRAR_MENU = "Cerrar menú"
export const ADMIN_MENU_NAVEGACION = "Menú del panel"
export const ADMIN_MENU_PANEL = "Panel"

// Panel (conteos)
export const ADMIN_VER_EJERCICIOS = "Ver ejercicios"
export const ADMIN_NUEVO_EJERCICIO = "Nuevo ejercicio"
export const ADMIN_VER_MICROCAPSULAS = "Ver microcápsulas"
export const ADMIN_NUEVA_MICROCAPSULA = "Nueva microcápsula"

// Perfil de la persona administradora
export const ADMIN_PERFIL_NOMBRE = "Nombre"
export const ADMIN_PERFIL_APELLIDO = "Apellido"
export const ADMIN_PERFIL_NOMBRE_USUARIO = "Nombre de usuario"
export const ADMIN_PERFIL_ROL = "Rol"
export const ADMIN_ROL_ADMINISTRADOR = "Administrador"
export const ADMIN_ROL_PERSONA_MAYOR = "Persona mayor"

// Vista previa de los formularios de ejercicio y de microcápsula
export const ADMIN_VISTA_PREVIA = "Así lo ve la persona"

// Título de la pestaña del navegador
export const ADMIN_TITULO_APP = "Alma Acompaña"
export const ADMIN_TITULO_PESTANIA = (titulo: string) => `${titulo} · Alma`
