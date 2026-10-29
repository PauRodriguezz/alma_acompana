// Textos de las pantallas de ejercicios del panel (CU-10). Los del panel en general están en
// features/admin/mensajes.ts y los comunes en lib/mensajes.ts.

// Lista
export const NUEVO_EJERCICIO = "Nuevo ejercicio"
export const FILTRO_TODAS = "Todas"
export const FILTRO_ETIQUETA = "Filtrar por categoría"
export const VACIO_SIN_EJERCICIOS = "Todavía no hay ejercicios."
export const vacioDeCategoria = (categoria: string) => `No hay ejercicios de ${categoria}.`
export const CONTEO_UNO = "Se muestra 1 ejercicio."
export const conteoVarios = (cantidad: number) => `Se muestran ${cantidad} ejercicios.`
export const TABLA_DESCRIPCION = "Ejercicios del catálogo"
export const COLUMNA_CONSIGNA = "Consigna"
export const COLUMNA_TIPO = "Tipo"
export const COLUMNA_DIFICULTAD = "Dificultad"
export const TARJETA_TIPO = "Tipo:"
export const TARJETA_DIFICULTAD = "Dificultad:"
export const ariaEditar = (consigna: string) => `Editar ejercicio: ${consigna}`
export const ariaEliminar = (consigna: string) => `Eliminar ejercicio: ${consigna}`

// Baja (confirmación y aviso)
export const BAJA_TITULO = "¿Eliminar este ejercicio?"
export const BAJA_DESCRIPCION = "Se va a eliminar del catálogo. Esta acción no se puede deshacer."
export const BAJA_CONFIRMAR = "Sí, eliminar"
export const AVISO_ELIMINADO = "El ejercicio fue eliminado."
export const AVISO_CREADO = "El ejercicio fue creado."
export const AVISO_ACTUALIZADO = "El ejercicio fue actualizado."

// Formulario
export const TITULO_EDITAR = "Editar ejercicio"
export const MIGA_EDITAR = "Editar"
export const MIGA_NUEVO = "Nuevo"
export const VOLVER_A_EJERCICIOS = "Volver a ejercicios"
export const BOTON_GUARDAR = "Guardar ejercicio"
export const ETIQUETA_CONSIGNA = "Consigna"
export const EJEMPLO_CONSIGNA = "Por ejemplo: Completá el refrán: «Más vale pájaro en mano…»"
export const ETIQUETA_TEXTO_A_MEMORIZAR = "Texto a memorizar"
export const AYUDA_TEXTO_A_MEMORIZAR = "Es lo que la persona lee antes de responder."
export const ERROR_TEXTO_A_MEMORIZAR = "Ingresá el texto a memorizar."
export const ETIQUETA_FORMATO = "Formato del ejercicio"
export const ETIQUETA_NIVEL = "Nivel de dificultad"
export const ETIQUETA_EXPLICACION = "Explicación"
export const AYUDA_EXPLICACION =
  "Se muestra después de responder. Ejemplo: Este ejercicio trata sobre completar con la palabra correcta el refrán que te aparece."

// Editor de opciones o pasos
export const OPCIONES_TITULO = "Opciones y respuesta correcta"
export const PASOS_TITULO = "Pasos y orden correcto"
export const OPCIONES_AYUDA = "Cargá de 3 a 4 opciones y marcá cuál es la correcta."
export const PASOS_AYUDA =
  "Cargá de 3 a 5 pasos. En «Posición» poné el lugar de cada paso en el orden correcto (1 es el primero)."
export const OPCION_CORRECTA = "Correcta"
export const ariaOpcionCorrecta = (n: number) => `Correcta: opción ${n}`
export const etiquetaPosicion = (n: number) => `Posición del paso ${n}`
export const IMAGEN_AGREGAR = "Agregar imagen"
export const maximoCargado = (maximo: number, plural: string) =>
  `Ya cargaste el máximo: ${maximo} ${plural}.`

/** Cómo se nombra cada elemento del editor según el formato (pasos u opciones). */
export const TEXTOS_ELEMENTO = {
  paso: { minuscula: "paso", mayuscula: "Paso", plural: "pasos", imagenDe: "del paso" },
  opcion: { minuscula: "opción", mayuscula: "Opción", plural: "opciones", imagenDe: "de la opción" },
} as const
export const etiquetaImagen = (imagenDe: string, n: number) => `Imagen ${imagenDe} ${n}`
export const quitarElemento = (minuscula: string, n: number) => `Quitar ${minuscula} ${n}`
export const agregarElemento = (minuscula: string) => `Agregar ${minuscula}`

// Vista previa
export const PREVIA_CONSIGNA = "Acá aparece la consigna que escribas."
export const PREVIA_TEXTO_A_MEMORIZAR = "Acá aparece el texto a memorizar."
export const PREVIA_ORDENAR = "Ordená los pasos."
export const PREVIA_CORRECTA = "(correcta)"
