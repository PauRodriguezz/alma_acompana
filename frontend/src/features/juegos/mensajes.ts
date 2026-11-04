// Textos del juego (contrato de juegos, 10.2 y 10.5; Figma 30:424, 30:464, 134:5064, 134:5074
// y 30:521). Los generales (errores, "Probar de nuevo", "Cargando…") están en lib/mensajes.ts.
// "Volver a Entretenimiento" usa el nombre de la navegación (lib/mensajes.ts) y los nombres de
// las categorías son los del panel (features/admin/etiquetas.ts).

export const TITULO_JUEGOS = "Juegos"

// Inicio de Juegos (Figma 30:424 y 44:1080). "Empecemos" es el texto del Figma (el contrato decía
// "Empezar"; se corrige en su sección 12).
export const INICIO_TITULO = "Hoy te traje diez juegos cortos"
export const INICIO_TEXTO = "Vamos a ejercitar la memoria. Podés dejar cuando quieras."
export const BOTON_EMPECEMOS = "Empecemos"

// Barra superior del ejercicio
export const VOLVER = "Volver"
export const PROGRESO_ETIQUETA = "Juegos de esta ronda"
export const textoProgreso = (posicion: number, total: number) => `${posicion} de ${total}`

// Ejercicio
export const BOTON_YA_LO_RECORDE = "Ya lo recordé"
export const BOTON_LISTO = "Listo"
export const BOTON_EMPEZAR_DE_NUEVO = "Empezar de nuevo"
// PROPUESTO (el Figma no dibuja ordenar): cómo se juega, arriba de los pasos.
export const ORDENAR_INDICACION =
  "Tocá los pasos en el orden que elijas. Si te equivocás, tocalo de nuevo para quitarlo."
// Solo para el lector de pantalla: va pegado al texto del paso ("Almuerzo, paso 2").
export const textoPasoNumero = (numero: number) => `, paso ${numero}`
export const TU_RESPUESTA = "Tu respuesta"
// Solo para el lector de pantalla: acompaña a la tilde de la opción correcta.
export const RESPUESTA_CORRECTA = "respuesta correcta"

// Panel de respuesta (Figma 134:5064 y 134:5074)
export const RESULTADO_ACIERTO = "¡Muy bien!"
export const RESULTADO_ERROR = "¡Casi!"
export const RESPUESTA_ERA = "La respuesta era:"
export const respuestaEra = (texto: string) => `${RESPUESTA_ERA} «${texto}».`
export const BOTON_CONTINUAR = "Continuar"

/** Lo que se anuncia al lector de pantalla cuando llega el resultado. */
export function anuncioResultado(acierto: boolean, respuestaCorrecta: string[]) {
  const titulo = acierto ? RESULTADO_ACIERTO : RESULTADO_ERROR
  return `${titulo} ${RESPUESTA_ERA} ${respuestaCorrecta.join(", ")}.`
}

// Final de la ronda (Figma 30:521). N es jugadosHoy: puede haber varias rondas en el día.
export const jugasteHoy = (n: number) => `Jugaste ${n} ${n === 1 ? "juego" : "juegos"} hoy`
export const FINAL_TEXTO = "¡Muy buen trabajo! Cuando quieras, seguimos."
export const BOTON_SEGUIR_JUGANDO = "Seguir jugando"
export const BOTON_SALIR = "Salir"

// Errores al pedir el ejercicio (contrato, sección 8 y 10.5)
export const ERROR_CARGAR_JUEGO = "No pudimos cargar el juego. Probá de nuevo."
export const SIN_JUEGOS = "Por ahora no hay juegos disponibles. Probá más tarde."
