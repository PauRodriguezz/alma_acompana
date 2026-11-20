// Textos generales que ve la persona (contrato de login, sección 7; contrato de recordatorios,
// sección 8). Un texto se escribe una sola vez, acá, y el resto del código lo importa.
// Pendiente (tarea aparte): el login todavía tiene escritos a mano los suyos.

export const MENSAJE_SESION_VENCIDA = "Tu sesión terminó. Ingresá de nuevo."
export const MENSAJE_SIN_CONEXION = "No pudimos conectarnos. Probá de nuevo en un rato."
export const MENSAJE_ALGO_SALIO_MAL = "Algo salió mal. Probá de nuevo en un rato."
export const MENSAJE_NO_ENCONTRADO = "No encontramos lo que buscás."
export const MENSAJE_SIN_PERMISO = "No tenés permiso para ver esta pantalla."
export const MENSAJE_DATOS_INVALIDOS = "Revisá los datos ingresados."

// Calendario (components/ui/calendar.tsx y el calendario de recordatorios)
export const CALENDARIO_HOY = "Hoy"
export const CALENDARIO_SELECCIONADO = "seleccionado"
export const CALENDARIO_MES_ANTERIOR = "Mes anterior"
export const CALENDARIO_MES_SIGUIENTE = "Mes siguiente"

// Navegación de la persona mayor
export const NAVEGACION_PRINCIPAL = "Principal"
export const NAVEGACION_ENLACE_ALMA = "Alma: ir al inicio"
export const NAVEGACION_NOMBRE_ALMA = "Alma"
export const NAVEGACION_CHAT = "Chat"
export const NAVEGACION_ENTRETENIMIENTO = "Entretenimiento"
export const NAVEGACION_HISTORIA_DE_VIDA = "Historia de vida"
export const NAVEGACION_RECORDATORIOS = "Recordatorios"
export const NAVEGACION_PERFIL = "Perfil"
// Primer elemento de Tab, en la persona mayor y en el panel (components/accesibilidad/SaltarAlContenido.tsx)
export const SALTAR_AL_CONTENIDO = "Saltar al contenido"

// Aviso flotante de éxito (components/AvisoFlotante.tsx)
export const AVISO_CERRAR = "Cerrar"

// Acciones, estados y textos comunes a los formularios y a las listas (panel de operador y
// subida de imágenes)
export const ACCION_CANCELAR = "Cancelar"
export const ACCION_EDITAR = "Editar"
export const ACCION_ELIMINAR = "Eliminar"
export const ACCION_PROBAR_DE_NUEVO = "Probar de nuevo"
export const ACCION_GUARDANDO = "Guardando…"
export const ESTADO_CARGANDO = "Cargando…"
export const CERRAR_SESION = "Cerrar sesión"
// Confirmación de cierre de sesión (Figma, Perfil; CU-14, camino alterno Nº3), la misma para la
// persona mayor y para el panel de operador
export const CERRAR_SESION_PREGUNTA = "¿Querés cerrar sesión?"
export const CERRAR_SESION_AVISO = "Para volver a entrar vas a necesitar tu usuario y tu contraseña."
export const CERRAR_SESION_CONFIRMAR = "Sí, cerrar sesión"
// Va con el espacio adelante: se escribe pegado a la etiqueta del campo.
export const CAMPO_OPCIONAL = " (opcional)"
export const ETIQUETA_CATEGORIA = "Categoría"
export const COLUMNA_ACCIONES = "Acciones"

// Subida de imagen (components/formularios/SubirImagen.tsx)
export const IMAGEN_TIPO_INVALIDO = "La imagen tiene que ser JPG, PNG o WEBP."
export const IMAGEN_MUY_PESADA = "La imagen no puede pesar más de 2 MB."
export const IMAGEN_ELEGIR = "Elegir imagen"
export const IMAGEN_CAMBIAR = "Cambiar imagen"
export const IMAGEN_QUITAR = "Quitar imagen"
export const IMAGEN_SUBIENDO = "Subiendo imagen…"

// Accesibilidad (botón y panel de tamaño de letra y contraste)
export const ACCESIBILIDAD_OPCIONES = "Opciones de accesibilidad"
export const ACCESIBILIDAD_TITULO = "Accesibilidad"
export const ACCESIBILIDAD_TAMANIO = "Tamaño de la letra"
export const ACCESIBILIDAD_AHORA = "Ahora:"
export const ACCESIBILIDAD_TAMANIO_NORMAL = "Tamaño normal, 100 %"
export const ACCESIBILIDAD_TAMANIO_GRANDE = "Tamaño grande, 125 %"
export const ACCESIBILIDAD_TAMANIO_MUY_GRANDE = "Tamaño muy grande, 150 %"
export const ACCESIBILIDAD_CONTRASTE = "Contraste"
export const ACCESIBILIDAD_NORMAL = "Normal"
export const ACCESIBILIDAD_NORMAL_AYUDA = "Los colores de siempre"
export const ACCESIBILIDAD_ALTO_CONTRASTE = "Alto contraste"
export const ACCESIBILIDAD_ALTO_CONTRASTE_AYUDA = "Letras negras y bordes marcados"
export const ACCESIBILIDAD_MUESTRA_TITULO = "Así se va a ver"
export const ACCESIBILIDAD_MUESTRA = "Hoy tenés una pastilla para tomar a las 21:00."
export const ACCESIBILIDAD_VOLVER_A_LO_ORIGINAL = "Volver a lo original"
export const ACCESIBILIDAD_LISTO = "Listo"
