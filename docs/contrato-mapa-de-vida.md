# Contrato del mapa de vida (historia de vida, v5): mapa por etapas, descarga y mejoras del chat

**Versión:** 1 de este documento (es la v5 de la funcionalidad de historia de vida). Diseño aprobado. **Front implementado** (mapa, chat, "Seguir completando" y descarga); el back de 2.12 y 3.1 a 3.3 ya está en el repo. **Documentos de la sección 7 actualizados el 12/11/2026.** Falta: la prueba manual completa contra el back real (sección 6) y el Figma (sección 8).
**Rama:** `feature/historia-de-vida` (back y front)
**Complementa y reemplaza, en lo que se indica en la sección 0:** `docs/contrato-historia-de-vida.md` (v4), `docs/contrato-grafo.md` (extractor y endpoint de repreguntas) y la parte del chat que ya implementó el módulo de historia de vida.
**Casos de uso:** CU-02 (criterio de aceptación Nº 4), CU-03 y CU-04, en `docs/casos-de-uso.md`. Este contrato los sigue tal como están escritos: no hace falta modificarlos (ver 2.8 y 2.6 para los dos puntos que se interpretan).
**Referencias visuales:** `docs/referencias/mapa-de-vida/` (Anexo A). **Las imágenes y los SVG de esa carpeta son parte del contrato**: lo que no se pueda decir con palabras está ahí.

Al probar la versión anterior con el modelo real aparecieron problemas de diseño que no son errores de código: el camino de paradas numeradas no se parecía al diseño y quedaba largo; los títulos y las descripciones de los recuerdos eran poco atractivos; las repreguntas se sentían vacías; una repregunta pendiente se perdía al volver a Historia de vida; la invitación a seguir la historia aparecía en medio de la conversación; y lo que la persona tocaba en el chat no dejaba constancia. Este contrato lo resuelve con un **mapa de vida por etapas**, una **imagen descargable** y varios ajustes del chat.

---

## 0. Qué reemplaza

| Del contrato de historia de vida (v4) | Qué pasa |
|---|---|
| 2.2 La invitación es un mensaje de Alma con texto fijo | **Reemplazada** por 2.8 (botón fijo y invitación a pedido). Se elimina la invitación automática. |
| 2.3 Espera y repregunta | **Ajustada** por 2.10 (consulta más seguido) y 2.11 (repreguntas con contexto). Lo demás sigue igual. |
| 2.5 Botones del modo guion | **Ajustada** por 2.9 (cada botón tocado deja una burbuja de la persona). Los botones y sus textos siguen igual. |
| 2.6 Línea de tiempo: camino de paradas | **Reemplazada** por 2.1 a 2.4 (mapa por etapas). |
| 2.6.1 Cómo se muestra el año | **Se mantiene** (se usa en el panel de la etapa y en el detalle). |
| 2.7 Detalle del recuerdo | **Ampliada** por 2.6. |
| 5.4 Pregunta pendiente (B3) | **Se mantiene**, pero la tarjeta "Próxima parada" pasa arriba del mapa (2.1). |
| 6 Textos | **Ampliada** por la sección 4 de este documento. Se dejan de usar "Ver recuerdos anteriores (N)" y "Sección sin fecha". |
| 7.3 y 7.4 (`GET /recuerdos` y `GET /recuerdos/{id}`) | **Ampliados** por 3.1 y 3.2. |
| 11.2, 11.4, 11.7 y 11.8 | **Reemplazadas** por la sección 5. |

Del contrato del grafo cambian: el prompt del extractor (2.12) y el endpoint `GET /repreguntas/siguiente` (3.3).

---

## 1. Alcance

### Entra

| Pieza | Qué es |
|---|---|
| **M1. Mapa por etapas** | La pantalla `/historia-de-vida` pasa a ser un camino con una parada por etapa de la vida. Solo front (2.1 a 2.3). |
| **M2. Accesibilidad del mapa** | Con letra grande el mapa se transforma en una lista; con alto contraste cambian los colores (2.4). |
| **M3. Descarga del mapa** | Imagen cuadrada "La vida de {nombre}" generada en el dispositivo (2.5). |
| **M4. Detalle y "Seguir completando"** | El detalle muestra más y permite retomar las preguntas que faltan de un recuerdo (2.6 y 2.7). Back y front. |
| **M5. Chat** | Botón fijo, invitación a pedido, burbujas de lo que se toca, espera más ágil y repreguntas con contexto (2.8 a 2.11). Solo front, salvo un dato nuevo del back (3.3). |
| **M6. Descripción más larga** | El extractor escribe de dos a cuatro oraciones (2.12). Solo back. |

### No entra

- Editar o borrar recuerdos desde la app, ni cambiar la etapa o el año a mano.
- Mejorar los recuerdos ya guardados: las descripciones nuevas valen solo para los que se creen desde ahora.
- Ilustración por tema de cada recuerdo (escuela, viaje, etc.): las ilustraciones son una por etapa.
- Compartir la imagen con otras personas desde la app: se descarga el archivo y la persona lo comparte como quiera.
- Cambios en el endpoint `/api/chat/mensajes` ni en el contrato del chat.

---

## 2. Decisiones tomadas

### 2.1 Mapa por etapas

La pantalla `/historia-de-vida` muestra un **camino que serpentea** con **una parada por etapa de la vida**, en este orden, y numeradas solo en la imagen descargable:

| # | Etapa | Rango | `etapaDeVida` del back |
|---|---|---|---|
| 1 | Infancia | 0 a 11 años | `INFANCIA` |
| 2 | Adolescencia | 12 a 17 años | `ADOLESCENCIA` |
| 3 | Juventud | 18 a 29 años | `JUVENTUD` |
| 4 | Adultez | 30 a 59 años | `ADULTEZ` |
| 5 | Vejez | 60 años o más | `VEJEZ` |
| 6 | Otros momentos | Todavía sin etapa | `null` |

- Las cinco etapas se muestran **siempre**, tengan o no recuerdos. Una etapa vacía se dibuja con borde punteado y el texto "Todavía sin recuerdos".
- **"Otros momentos" solo aparece si hay recuerdos sin etapa** (`etapaDeVida` nulo). Si no hay, la parada 6 no se dibuja. Reemplaza a la sección "Sin fecha": no lleva la palabra "fecha" ni nada que parezca un error.
- Cada parada es **una bandera plantada al costado del camino**, no sobre él. En el camino queda solo un punto de parada. La parada tiene: el punto, el mástil, un **banderín con el nombre de la etapa**, una **tarjeta** debajo del banderín (separada por un pequeño espacio, ver Anexo A) y una **ilustración** del lado opuesto del camino.
- La tarjeta muestra, según la cantidad de recuerdos de la etapa:

| Recuerdos | La tarjeta muestra |
|---|---|
| 0 | "Todavía sin recuerdos" |
| 1 | el título |
| 2 | los dos títulos |
| 3 o más | el primer título y "y {N−1} más" |

  Los títulos van en una sola línea, cortados con "…" si no entran. El título completo se ve en el panel (2.3).
- **Toda la parada (banderín, tarjeta y punto) es un único elemento tocable.** Tocarlo abre el panel de la etapa (2.3).
- **Dibujo y texto no se mezclan.** El camino, las ilustraciones, los mástiles y la forma de los banderines son un dibujo decorativo (SVG) que los lectores de pantalla ignoran (`aria-hidden`). **Los nombres y los títulos son texto real** (HTML), posicionado sobre el dibujo y con tamaño en `rem`, para que crezcan con el tamaño de letra de la app y se puedan leer con un lector de pantalla. No se escriben textos dentro de un SVG.
- **Tarjeta de pregunta pendiente (CU-03, camino básico B).** La tarjeta "Próxima parada" ("Alma tiene una pregunta" / "Seguir escribiendo", 5.4 del contrato de historia de vida) **se mantiene, pero va arriba del mapa**, debajo del título, en formato compacto. Antes estaba al final del camino y la hoja del panel la tapaba. Su comportamiento no cambia.
- **Estado vacío.** Sin recuerdos, el mapa se muestra igual (las cinco etapas vacías) y arriba el texto de estado vacío de siempre ("Todavía no tenemos recuerdos. Respondé las preguntas de Alma y van a aparecer acá.").
- **Cabecera.** Título "Historia de vida", subtítulo "Tu camino, etapa por etapa" y, a la derecha, el botón "Descargar" (2.5).
- **Aviso de que hay que tocar una bandera.** Mientras la persona no haya tocado ninguna en esa visita: una pastilla fija abajo con el texto "Tocá una bandera para ver sus recuerdos" que sube y baja suavemente, y los puntos de parada que laten con suavidad. Ambos desaparecen al tocar la primera parada (el estado vive en memoria). Con "reducir movimiento" activado en el sistema (`prefers-reduced-motion`), no hay animación: la pastilla queda fija y los puntos quietos.
- **Móvil y escritorio.** Se mantiene el corte `lg` de Tailwind (1024 px). Por debajo de `lg`: el mapa ocupa el ancho y el panel es una hoja que sube desde abajo (2.3). Desde `lg`: dos columnas; el mapa a la izquierda (ancho máximo de unos 28 rem, centrado en su columna) y el panel de la etapa a la derecha, fijo mientras se desplaza el mapa. En escritorio **no hay hoja inferior**, y al entrar se deja seleccionada la primera etapa que tenga recuerdos.
- **Ancho mínimo.** Con letra Normal el mapa tiene que verse sin superposiciones desde 320 px de ancho.

### 2.2 Qué recuerdo va en cada etapa

- Por `etapaDeVida` del recuerdo, tal como lo devuelve `GET /api/historia/recuerdos`. El back ya envía la lista en orden cronológico (por `anio`, nulos al final); dentro de cada etapa el front conserva ese orden.
- `etapaDeVida` nulo → "Otros momentos".
- El año se muestra como en 2.6.1 del contrato de historia de vida (`formatearAnio`): "1957" o "Alrededor de 1957".

### 2.3 Panel de la etapa

Al tocar una parada se abre el panel de esa etapa:

- **Móvil:** hoja que sube desde abajo, con una barra para arrastrar, el título de la etapa con su rango, un botón para cerrarla y la lista de recuerdos. Se cierra con el botón, con la tecla Escape o tocando fuera. Al abrirla, el mapa se desplaza lo necesario para que la parada elegida quede visible por encima de la hoja. La parada elegida queda resaltada (borde más grueso en el color de la etapa).
- **Escritorio:** columna derecha con el mismo contenido (sin barra ni botón de cerrar).
- **Cada recuerdo** es una fila con el título, el año (2.2) y una flecha; tocarla lleva al detalle (`/historia-de-vida/:recuerdoId`). Las filas siguen el orden de 2.2.
- **Si el recuerdo tiene datos para completar** (`faltantes` no vacío, 3.1), la fila muestra además el botón de texto **"Seguir completando este recuerdo"** (2.7).
- **Etapa sin recuerdos:** "Cuando cuentes algo de esta etapa, aparece acá."
- **Accesibilidad de la hoja:** `role="dialog"`, `aria-modal="true"` y `aria-labelledby` con el título; el foco pasa al título al abrir, queda atrapado dentro mientras está abierta y vuelve a la parada al cerrar.

### 2.4 Accesibilidad: tamaño de letra y alto contraste

La app tiene dos preferencias (`useAccesibilidad`): tamaño de letra (Normal, Grande, Muy grande) y alto contraste (fondo blanco, texto negro, bordes de 2 px, sin sombras). El mapa las respeta así:

| Preferencia | Qué cambia |
|---|---|
| Letra **Normal** | Se muestra el mapa con el camino. |
| Letra **Grande** o **Muy grande** | **El mapa pasa a ser una lista vertical de etapas**, porque las tarjetas serían demasiado anchas para el camino. Cada etapa es una tarjeta con su ilustración, el banderín de color con el nombre, la cantidad de recuerdos ("3 recuerdos", "1 recuerdo" o "Sin recuerdos") y hasta dos títulos **completos, sin cortar**. Una línea de camino decorativa une las tarjetas por un costado. Tocar una tarjeta abre el mismo panel (2.3). El botón "Descargar" está en ambos modos. |
| **Alto contraste** | Los banderines pasan a negro con texto blanco; las tarjetas y los círculos de las ilustraciones, a blanco con borde negro de 2 px (4 px la etapa elegida); las ilustraciones, en negro; el camino, en gris oscuro con la línea punteada blanca. La etapa **nunca se reconoce solo por el color**: siempre está escrito el nombre. Los colores salen de los tokens del tema (`--foreground`, `--border`, `--background`) y de la variante `contraste-alto:` ya existente. |

Además:
- **Colores en modo normal.** Los seis colores de las etapas (Anexo A) tienen un contraste con el texto blanco del banderín de 6,2 a 7,0 y con la tarjeta blanca de 6,2 a 7,0 (se midieron con la fórmula de WCAG). Si se cambia alguno, hay que volver a medir y mantener al menos 4,5. El texto de apoyo (`#5B6B78` sobre el fondo crema) da 5,3.
- **Nombre accesible de cada parada:** "Infancia, 3 recuerdos" / "Infancia, sin recuerdos". El mapa es un grupo (`role="group"`, `aria-label="Mapa de tu historia de vida"`). Las paradas son botones reales, se recorren con la tecla Tab y se activan con Enter o espacio.
- **Área táctil:** como mínimo 44 × 44 px (2,75 rem). La parada completa cumple de sobra.
- La animación respeta `prefers-reduced-motion` (2.1).

### 2.5 Descarga del mapa (imagen cuadrada)

- El botón **"Descargar"** (arriba a la derecha) genera una imagen **PNG cuadrada de 1080 × 1080 px** y la descarga con el nombre `mapa-de-vida.png`. **Se crea en el dispositivo**: no pasa por el back ni se manda a ningún servicio.
- **Contenido** (ver la imagen de referencia, Anexo A):
  - Arriba, centrado, el título **"La vida de {Nombre}"**: "La vida de" en Poppins medium (la tipografía de los títulos de la app) y el nombre en **cursiva** con una tipografía que combine (Lora cursiva) y en el color violeta de la app. Si el nombre de la persona no está disponible, el título es "Mi vida" (sin cursiva).
  - Un camino en forma de S con tres filas: la primera de izquierda a derecha, la segunda de derecha a izquierda y la tercera de izquierda a derecha. Las seis paradas van **numeradas del 1 al 6** dentro de su punto, para que se entienda el recorrido. Cada parada tiene su banderín, su tarjeta y su ilustración. **La tarjeta de la imagen muestra más que la de la pantalla** (la de la pantalla sigue con la regla de 2.1): con 0 recuerdos, "Todavía sin recuerdos"; con 1 a 3, **todos los títulos**; con 4 o más, **los dos primeros y "y {N−2} más"**. Cada título va en una sola línea, cortado con "…" según el **ancho medido** del texto (no por una cantidad fija de caracteres). Con tres líneas la tarjeta es más alta (96 px en lugar de 84) y todo el grupo de la parada (mástil, banderín, ilustración y tarjeta) está 14 px más arriba que en la referencia, para que la tarjeta no se acerque al camino. Una prueba automática (`descarga/diseno.test.ts`) verifica, con 0, 1, 2, 3, 4 y 6 recuerdos por etapa, que tarjetas, banderines e ilustraciones queden al menos 6 px del borde del camino y que nada se pise entre filas ni con el título, el pie o el borde.
  - "Otros momentos" (la parada 6) solo si hay recuerdos sin etapa; si no, no se dibuja.
  - Abajo a la derecha, pequeño, el texto **"Alma Acompaña"** con un corazón.
- **La imagen no depende de las preferencias de accesibilidad** (tamaño de letra y contraste): siempre sale con el diseño de la referencia (fondo crema, colores de las etapas).
- **Tipografías.** Poppins medium y Source Sans 3 ya están en la app. Hay que sumar Lora cursiva (`@fontsource/lora`, peso 500, cursiva, latín; ver 5.3). **Antes de dibujar, las fuentes tienen que estar cargadas** (`document.fonts.load(...)`); si se dibuja antes, la imagen sale con otra letra. Se puede dibujar con Canvas 2D o convertir un SVG a imagen, con la condición anterior.
- Si falla, se muestra "No pudimos crear la imagen. Probá de nuevo." con el aviso existente de la app.
- La imagen incluye los **títulos de los recuerdos**, que son datos personales. Se genera y se guarda solo en el dispositivo de la persona; lo que haga después con el archivo es decisión suya.

### 2.6 Detalle del recuerdo

`RecuerdoDetallePage` agrega a lo de 2.7 del contrato de historia de vida:

- Debajo del título, en tono secundario: **"Respondiste a: «{preguntaTexto}»"** (3.2). Si `preguntaTexto` es nulo, no se muestra.
- La descripción es más larga a partir de 2.12 (no cambia nada en la pantalla).
- Si `faltantes` no está vacío, el botón **"Seguir completando este recuerdo"** (2.7).
- El detalle **sigue sin permitir editar** (CU-04): el botón no modifica el recuerdo, lleva a la conversación con Alma, que es donde se contestan las repreguntas.

### 2.7 "Seguir completando este recuerdo"

Desde el panel (2.3) o el detalle (2.6):

1. El front pide `GET /api/historia/repreguntas/siguiente?recuerdoId=<id>` (3.3).
2. `PROCESANDO`: espera como en 2.10 (hay una extracción en curso).
3. **Si el chat está ocupado** (esperando la respuesta del modelo o la extracción de una respuesta anterior), el pedido **espera a que se libere** (hasta 60 s) antes de consultar; mientras tanto el botón dice "Alma está pensando…". Pasado el tope no se navega: se muestra "Alma todavía está respondiendo. Probá de nuevo en un momento." (Antes se navegaba al chat sin pedir nada y no aparecía la repregunta ni su apertura.)
   `HAY`: navega al chat (`/`), muestra la apertura y la repregunta (2.11) como mensaje de Alma y activa el objetivo `REPREGUNTA`. En lugar de `preguntaOrigenId` se guarda el **`recuerdoEnCursoId`** (el chat usa uno u otro, nunca los dos): mientras exista, las repreguntas siguientes se piden con `recuerdoId=` y `excluir=` (2.4 del contrato de historia de vida).
4. `NINGUNA`: no se navega; se muestra "Por ahora no tengo más para preguntarte de este recuerdo." y se actualiza la lista (los datos que faltaban ya no hacen falta o se agotaron los intentos).
5. Al terminar las repreguntas se hace el cierre de siempre ("¿Querés contarme algo más sobre esto o te hago otra pregunta?", con "Contarte algo más" y "Otra pregunta"). Si la persona elige "Otra pregunta" se pide `/guion/proxima`.
6. Al volver a Historia de vida se **invalida** la consulta de recuerdos (TanStack Query) para que se vean la etapa, el año y los datos pendientes actualizados.
7. **Los intentos no cambian:** cada dato de cada recuerdo se pregunta como máximo 2 veces en total (5.7 del contrato del grafo). Una vez agotados, el botón deja de aparecer para ese dato (límite 9.1).

### 2.8 Botón fijo y invitación a pedido (reemplaza 2.2 del contrato de historia de vida)

La invitación automática ("¿Seguimos completando tu historia de vida?" después de la primera respuesta de Alma) **se elimina**, porque aparecía en medio de la conversación, a veces pegada a una pregunta de Alma. Para que Alma siga "ofreciendo" continuar con la historia (CU-02, criterio Nº 4) sin interrumpir, se usa:

**a) Botón fijo "Seguir mi historia de vida".**
- Vive en el chat, justo encima del campo de mensaje. **Desde `lg` (escritorio)** es un botón secundario de texto visible y de al menos 44 px de alto (no solo un ícono).
- **Por debajo de `lg` (celular)** se muestra **compacto**: un círculo de 56 px con el ícono de Historia de vida (el mismo de la navegación) y sin texto visible (para no ocupar espacio en pantalla), que flota arriba del campo de mensaje, a la derecha, sin taparlo. Su nombre accesible es el completo ("Seguir mi historia de vida"), con `aria-expanded`. **Al tocarlo se despliega a su izquierda el botón completo**, que hace lo de abajo; tocar de nuevo el círculo o la tecla Escape lo pliega. Se **oculta mientras el campo de mensaje tiene el foco** (el teclado está abierto) y vuelve al salir. Mientras se ve, el final de la conversación reserva el espacio del círculo para que no tape el último mensaje ni sus botones.
- Se muestra mientras **todas** estas condiciones se cumplan: no hay un objetivo activo, no hubo una alarma en esa visita y `GET /api/historia/guion/estado` informa preguntas pendientes. Sin pendientes, no se muestra.
- Al tocarlo hace lo mismo que el "Sí" de la invitación: pide `/guion/proxima`, muestra la introducción y la pregunta como mensaje de Alma, fija `preguntaOrigenId` y activa el objetivo `PREGUNTA`. Deja una burbuja de la persona con su texto (2.9).
- Como la persona lo pidió, no se bloquea por haber tocado "Ahora no" antes.

**b) Invitación a pedido, cuando la persona lo escribe.**
- Se evalúa el texto que la persona **envía al chat normal** (sin objetivo activo). El mensaje se manda al back **igual que siempre** (el detector de riesgo del chat sigue funcionando): el front **no** se salta la llamada.
- Cuando llega la respuesta de Alma y **no** trae `alarma`, si el texto cumple `pideSeguirConLaHistoria` (abajo), el front agrega a continuación la **invitación** de siempre: "¿Seguimos completando tu historia de vida?" con "Sí" y "Ahora no". Se muestra aunque ya se hubiera mostrado o rechazado antes en la visita. No se muestra si hay objetivo activo o si hubo alarma.
- **Sin preguntas pendientes:** en lugar de la invitación, un mensaje fijo de Alma: "Ya respondiste todas las preguntas de tu historia. Podés ver tus recuerdos en Historia de vida." con el botón "Ver mi historia de vida" (navega a `/historia-de-vida`).
- `pideSeguirConLaHistoria(texto)` es una función pura: normaliza el texto (minúsculas, sin tildes ni signos) y devuelve verdadero si contiene **"historia de vida"**, o si contiene un verbo de continuar (`seguir`, `seguimos`, `sigamos`, `sigo`, `continuar`, `continuemos`, `retomar`, `retomemos`, `completar`, `completemos`) seguido, a no más de 30 caracteres, de **"historia"** o **"preguntas"**. **No** se activa con la palabra "historia" sola ("me contó una historia"). Tiene pruebas con ejemplos que sí y que no (sección 6).
- Alma (el modelo) responde además según la regla 7 del prompt ("Podés retomarla desde la sección Historia de vida…"). Esa frase y la invitación conviven; si la repetición molesta, se ajusta la regla en una próxima versión (límite 9.5). El texto de la regla 7 **no cambia** en esta versión.

### 2.9 Burbujas con lo que toca la persona

Todo botón tocado en el chat deja **una burbuja de la persona** (la misma `MensajePersona` de siempre) con el texto del botón, antes de la respuesta de Alma:

| Botón | Burbuja |
|---|---|
| "Sí" (invitación) | "Sí" |
| "Ahora no" | "Ahora no" |
| Botón fijo | "Seguir mi historia de vida" |
| "Otra pregunta" (debajo de una pregunta o en el cierre) | "Otra pregunta" |
| "Saltear" (repregunta) | "Saltear" |
| "Contarte algo más" | "Contarte algo más" |

- **No se mandan al modelo.** Estas burbujas son locales: no se incluyen en la `conversacion` que se manda a `/api/chat/mensajes` ni en lo que se manda a `/api/historia`, y **no cuentan** como que la persona "ya escribió" (la regla `yaEscribio` del widget de ánimo no cambia). Se marcan con un origen propio (por ejemplo `origen: "ACCION"`).
- Al tocar un botón, los botones de ese mensaje se retiran; la burbuja deja constancia de qué se eligió.
- **Respuesta fija de Alma tras "Contarte algo más" y "Ahora no".** Después de la burbuja de la persona, Alma contesta con un mensaje fijo, **sin llamar al modelo**, que rota entre tres variantes (una por vez, y vuelve a empezar). Son mensajes del front como la invitación y el cierre: no viajan al modelo como historial ni cuentan como que la persona "ya escribió". Los otros botones no lo necesitan: "Sí", "Otra pregunta" y "Saltear" ya reciben una pregunta, una repregunta o el indicador de espera. Escribir sin contestar la invitación (el "Ahora no" implícito) no tiene respuesta fija: contesta el modelo. Los textos están en la sección 4.
- "Volver a Historia de vida" y "Ver mi historia de vida" **navegan**: no dejan burbuja. "Volver a Historia de vida" sigue retirando el bloque de pregunta sin responder (como hoy).

### 2.10 Espera de la extracción más ágil

El `ChatProvider` consulta `GET /repreguntas/siguiente` **cada 1 s durante los primeros 10 s y cada 2 s después**, con el mismo tope de 60 s (2.3 del contrato de historia de vida). Las constantes pasan a ser tres: `INTERVALO_ESPERA_INICIAL_MS = 1000`, `DURACION_ESPERA_INICIAL_MS = 10000` y `INTERVALO_ESPERA_MS = 2000`. El indicador "Alma está pensando…" no cambia.

### 2.11 Repreguntas con contexto

Para que las repreguntas no suenen vacías, **antes de la primera repregunta de cada recuerdo en la charla** Alma muestra una frase de apertura fija (sin llamar al modelo) que nombra el recuerdo, y a continuación la repregunta de siempre (`texto` del back). Las dos van en el mismo mensaje de Alma, en párrafos separados.

Se rotan estas tres, una por recuerdo y por charla:

1. "Gracias por contármelo. Quiero saber un poco más de «{titulo}»."
2. "Me gustó lo que contaste. Sigamos con «{titulo}»."
3. "Gracias por compartirlo. Un detalle más sobre «{titulo}»."

- `{titulo}` es el `repregunta.tituloRecuerdo` que ahora trae la respuesta `HAY` (3.3).
- Si el título está vacío o parece una pregunta (empieza con "¿" o termina en "?"), que es lo que pasa cuando la extracción falló y se guardó el recuerdo de respaldo, **no se cita el título** y la apertura es solo "Gracias por contármelo."
- Las repreguntas siguientes del mismo recuerdo (año, lugares, personas) no repiten la apertura.
- Estos textos son una propuesta para probar con el modelo real y se ajustan en `mensajes.ts` sin tocar nada más.

### 2.12 Descripción de recuerdo más larga (extractor)

- `historia/prompt-extractor.txt`: la `descripcion` pasa de "una o dos oraciones" a **de dos a cuatro oraciones**, en tercera persona, **solo con lo que la persona dijo** (sin inventar ni agregar interpretaciones). El `titulo` sigue siendo un texto corto que nombra el recuerdo y **nunca una pregunta ni empieza con "¿"**.
- No cambia el esquema del JSON ni las validaciones ("no inventar" y las demás). Si la respuesta de la persona es muy corta, la descripción puede tener menos oraciones y **nunca se rellena**. El tope de tokens de la extracción con Gemini (mínimo 1500) alcanza; el de Ollama (`extraccion-max-tokens`) sube de 600 a 800 para que el JSON no se corte (si se corta, se crea el recuerdo de respaldo).
- Se prueba con el modelo real (sección 6) porque una descripción más larga puede aumentar las invenciones.
- Vale para los recuerdos que se creen desde ahora. Los que ya están guardados no se modifican.

---

## 3. Back: cambios en endpoints

Todos bajo `/api/historia`, rol `PERSONA_MAYOR`, con las mismas reglas de acceso y aislamiento de siempre (5.2 del contrato de historia de vida).

### 3.1 `GET /recuerdos` (amplía 7.3)

Cada elemento agrega **`faltantes`**: lista con los datos que se pueden seguir completando en ese recuerdo, entre `"ANIO"`, `"LUGARES"` y `"PERSONAS"`, en el orden de la lista `repreguntas` de su pregunta de origen. Un dato está en la lista cuando es **candidato** según 5.7 del contrato del grafo, sin considerar `excluir`: el dato está en `repreguntas` de la pregunta de origen, falta (año: sin año o con precisión `ESTIMADA`; lugares: sin `OCURRIO_EN`; personas: sin `PARTICIPA_EN`) **y** su contador de intentos es menor que 2. Si no hay ninguno, `[]`.

```json
{
  "id": "6f0c0c1e-2b8a-4f3e-9d1a-0c4f5a7e1b22",
  "titulo": "Mi maestra Elena",
  "anio": 1957,
  "precision": "ESTIMADA",
  "etapaDeVida": "INFANCIA",
  "resumen": "Su maestra de primer grado, que le enseñó a leer.",
  "faltantes": ["ANIO", "LUGARES"]
}
```

El resto no cambia (incluido `resumen`, que el mapa no usa). El listado se arma sin consultas por recuerdo (una sola lectura para toda la lista).

### 3.2 `GET /recuerdos/{recuerdoId}` (amplía 7.4)

Agrega:
- **`faltantes`**: igual que en 3.1.
- **`preguntaTexto`**: el texto de la pregunta del guion de la que surgió el recuerdo (relación `SURGIO_DE`); `null` si no se encuentra.

### 3.3 `GET /repreguntas/siguiente` (amplía 7.5 del contrato del grafo)

- Nuevo parámetro opcional **`recuerdoId`** (UUID). Se puede mandar `preguntaId` **o** `recuerdoId`, no los dos (400). Con `recuerdoId`, los candidatos son solo los datos de **ese** recuerdo (las mismas reglas de 5.7, con `excluir`). Si no hay candidatos, o el recuerdo no existe o es de otra persona, responde `NINGUNA` (como con un `preguntaId` desconocido). Un UUID mal formado da 400. `PROCESANDO` conserva su prioridad (es por persona).
- La respuesta `HAY` agrega **`tituloRecuerdo`** dentro de `repregunta`, junto a `recuerdoId`, `dato` y `texto`: el título del recuerdo al que corresponde (puede ser `null`). Vale con `preguntaId` y con `recuerdoId`. Ejemplo: `{"estado":"HAY","repregunta":{"recuerdoId":"…","dato":"ANIO","texto":"…","tituloRecuerdo":"Mi maestra Elena"}}`.

### 3.4 Sin cambios

`POST /guion/{preguntaId}/respuesta`, `POST /repreguntas/{recuerdoId}/{dato}/respuesta` y los dos `salteo` no cambian, ni sus códigos ni el límite de 2 intentos por dato.

---

## 4. Textos

### Nuevos (fijos, del front)

| Dónde | Texto |
|---|---|
| Subtítulo de la pantalla | "Tu camino, etapa por etapa" |
| Botón de descarga | "Descargar" (en la imagen: "Descargar mi mapa" como texto accesible) |
| Pastilla inicial | "Tocá una bandera para ver sus recuerdos" |
| Nombres y rangos de etapa | Ver 2.1 |
| Tarjeta de etapa vacía | "Todavía sin recuerdos" |
| Cantidad de la tarjeta | "y {N} más" |
| Cantidad en la lista accesible | "3 recuerdos" / "1 recuerdo" / "Sin recuerdos" |
| Panel de etapa vacía | "Cuando cuentes algo de esta etapa, aparece acá." |
| Cerrar el panel | "Cerrar" (texto accesible del botón) |
| Botón de datos pendientes | "Seguir completando este recuerdo" |
| Pregunta de origen en el detalle | "Respondiste a: «{preguntaTexto}»" |
| Sin más repreguntas de un recuerdo | "Por ahora no tengo más para preguntarte de este recuerdo." |
| Botón fijo del chat | "Seguir mi historia de vida" |
| Sin preguntas pendientes (a pedido) | "Ya respondiste todas las preguntas de tu historia. Podés ver tus recuerdos en Historia de vida." |
| Botón de ese mensaje | "Ver mi historia de vida" |
| Aperturas de repregunta | Ver 2.11 |
| Título de la imagen | "La vida de {Nombre}" / "Mi vida" |
| Pie de la imagen | "Alma Acompaña" |
| Error al descargar | "No pudimos crear la imagen. Probá de nuevo." |
| Respuestas a "Contarte algo más" (rotan) | "Dale, contame lo que quieras. Te escucho." / "Claro, te escucho. Contame con calma." / "Con gusto. Contame lo que tengas ganas." |
| Respuestas a "Ahora no" (rotan) | "Está bien, cuando quieras seguimos." / "Sin problema. Acá estoy para lo que necesites." / "Está bien. Cuando tengas ganas, lo retomamos." |
| "Seguir completando" con el chat ocupado más de 60 s | "Alma todavía está respondiendo. Probá de nuevo en un momento." |

### Que se dejan de usar

"Ver recuerdos anteriores (N)" y "Sin fecha" (el prefijo "Alrededor de" y el resto de los textos del contrato de historia de vida siguen).

---

## 5. Front

Todo en `frontend/src`.

### 5.1 Estructura

```
features/historia/
  etapas.ts                  // ETAPAS (id, nombre, rango, colores normal y alto contraste, ilustración)
  agruparPorEtapa.ts         // agruparPorEtapa(recuerdos), resumenDeTarjeta(recuerdos)
  pideSeguirConLaHistoria.ts // 2.8 b (función pura, con pruebas)
  descarga/
    dibujarMapa.ts           // dibuja la imagen cuadrada (2.5)
    descargarMapa.ts         // carga fuentes, dibuja, baja el PNG
  components/
    MapaDeVida.tsx           // camino (SVG decorativo) + paradas (HTML)
    ParadaDeEtapa.tsx
    IlustracionDeEtapa.tsx   // una por etapa, SVG decorativo
    ListaDeEtapas.tsx        // modo letra grande (2.4)
    PanelDeEtapa.tsx         // hoja en móvil, columna en escritorio (2.3)
    BotonDescargarMapa.tsx
    (se mantienen TarjetaProximaParada.tsx e InvitacionHistoria.tsx)
    (se eliminan CaminoDeRecuerdos.tsx, ParadaDeRecuerdo.tsx y SeccionSinFecha.tsx)
features/chat/
  components/BotonSeguirHistoria.tsx
```

### 5.2 Pantallas y chat

- **`HistoriaDeVidaPage`:** cabecera (título, subtítulo, "Descargar"), tarjeta compacta de pregunta pendiente (si corresponde), estado vacío (si corresponde) y, según 2.4, `MapaDeVida` (letra Normal) o `ListaDeEtapas` (Grande o Muy grande), más el `PanelDeEtapa`. Usa `useAccesibilidad` para el tamaño de letra y `useEsEscritorio` para el diseño de dos columnas.
- **`RecuerdoDetallePage`:** 2.6. **`PanelDeEtapa`:** 2.3 y 2.7.
- **`ChatProvider` y `ChatPage`:** botón fijo (2.8 a), invitación a pedido (2.8 b), burbujas de las acciones (2.9), consulta más ágil (2.10), apertura de la repregunta (2.11) y `recuerdoEnCursoId` (2.7). Se elimina la invitación automática (sus banderas `invitacionMostrada` e `invitacionRechazada` dejan de decidir si se ofrece sola; la invitación a pedido las ignora).
- **Después de cambiar algo** que modifique recuerdos (una repregunta contestada, un recuerdo nuevo), se invalida la consulta de recuerdos (2.7, punto 6).

### 5.3 Cambios en archivos compartidos

- `frontend/package.json` y el lockfile: agregar `@fontsource/lora` (para la cursiva del título de la imagen) y en `index.css` importar `@fontsource/lora/latin-500-italic.css`.
- Textos de la sección 4: en `features/historia/mensajes.ts` (y `features/chat/mensajes` si corresponde). No se agregan textos a `lib/mensajes.ts`.
- No se tocan `lib/api.ts`, `SecurityConfig` ni `application.properties`.

---

## 6. Pruebas y criterio de listo

### Back (automáticas)

- 3.1 y 3.2: `faltantes` correcto con cada combinación (sin año, año `ESTIMADA`, sin lugares, sin personas, contador en 2, dato que no está en `repreguntas` de la pregunta), `[]` cuando no falta nada, y `preguntaTexto` con y sin pregunta de origen.
- 3.3: `recuerdoId` válido con candidatos, sin candidatos (`NINGUNA`), ajeno e inexistente (`NINGUNA`), mal formado (400), junto con `preguntaId` (400), con `excluir`, y `PROCESANDO` con prioridad; `tituloRecuerdo` en `HAY`.
- Un recuerdo sin pregunta de origen devuelve `faltantes: []`.
- Aislamiento: una persona no ve `faltantes` ni títulos de otra.
- Las pruebas anteriores siguen pasando. Swagger actualizado.

### Extractor (manual, con el modelo real)

- Cinco respuestas de distinta longitud: la descripción sale de dos a cuatro oraciones, sin datos inventados; el título es corto y no es una pregunta; el JSON no se corta.
- Una respuesta de una sola palabra: no se inventa contenido.

### Front (manual)

- **Año estimado:** un recuerdo con precisión `ESTIMADA` se ve como "Alrededor de {año}" en el panel y en el detalle.
- **Mapa:** las cinco etapas vacías; con recuerdos en cada etapa; "Otros momentos" con y sin recuerdos; títulos largos; 1, 2 y 3 o más recuerdos por etapa; a 320, 360 y 430 px de ancho y en escritorio.
- **Panel:** abrir, cerrar (botón, Escape, tocar fuera), foco, desplazamiento del mapa en móvil, columna en escritorio.
- **Accesibilidad:** letra Grande y Muy grande (lista), alto contraste, lector de pantalla (nombre de cada parada), teclado, "reducir movimiento".
- **Descarga:** la imagen sale cuadrada, con las fuentes correctas, las seis paradas numeradas, el nombre en cursiva, "Mi vida" cuando no hay nombre, y sin la parada 6 si no hay "Otros momentos"; igual con letra grande y alto contraste activos.
- **Botón fijo:** aparece con pendientes, desaparece sin pendientes, en modo guion y después de una alarma.
- **Invitación a pedido:** frases que **sí** activan ("quiero seguir con mi historia de vida", "sigamos con mi historia", "podemos continuar con las preguntas", "retomemos la historia de vida") y que **no** ("me contó una historia", "la historia de mi barrio", "seguí derecho por esa calle"); con alarma no aparece.
- **Burbujas:** cada botón tocado deja su burbuja y no viaja al modelo ni cambia `yaEscribio`.
- **Espera y repreguntas:** apertura con título, apertura sin título cuando el título es una pregunta, rotación de las tres frases.
- **Seguir completando:** desde el panel y desde el detalle; `NINGUNA` con el aviso; al volver, la lista se actualiza.

### Listo cuando

El mapa se ve como la referencia en móvil y escritorio; con letra grande se ve la lista; la imagen descargada coincide con la referencia; se puede completar un recuerdo desde la lista y desde el detalle; el chat deja constancia de lo que se toca y no ofrece la historia en medio de la conversación; la suite de back pasa; y la sección 7 está hecha.

---

## 7. Documentos a actualizar al cerrar

1. `docs/contrato-historia-de-vida.md`: pasar a v5, marcar las secciones de la sección 0 como reemplazadas (con un puntero a este documento) e integrar lo que corresponda.
2. `docs/contrato-grafo.md`: 7.5 (`recuerdoId` y `tituloRecuerdo`) y 5.4 (prompt del extractor).
3. `docs/casos-de-uso.md`, tabla "Estado de implementación": columna Front de CU-02, CU-03 y CU-04. Sin cambiar los casos de uso: en CU-02 (criterio Nº 4) el ofrecimiento de seguir con la historia lo cubre el botón fijo y la invitación a pedido; en CU-04 el detalle sigue sin editar (el botón lleva al chat).
4. `docs/contrato-chat.md` 7.2 (alarma, Swagger y `yaEscribio`) y `docs/contrato-historia-de-vida.md` 2.5 y 11.4: cerrar lo que quedó pendiente de la versión anterior.
5. `CLAUDE.md`: el enlace a este documento donde corresponda.

**Hecho el 12/11/2026:** (1) `contrato-historia-de-vida.md` pasó a la v5, con las secciones reemplazadas marcadas y un puntero a este documento; (2) `contrato-grafo.md` ya tenía 7.5 y 5.4 al día (v17); (3) `casos-de-uso.md`: columna Front de CU-02, CU-03 y CU-04, sin tocar el texto de los casos; (4) `contrato-chat.md` 7.2, 11.1 y 11.4 (`alarma`, Swagger y `yaEscribio`) y `contrato-historia-de-vida.md` 2.5 y 11.4; (5) `CLAUDE.md` enlaza este documento y el de historia de vida.

## 8. Pendiente fuera del código: el Figma

**Al final**, cuando todo esté probado, hay que actualizar el Figma para que coincida con lo implementado: las pantallas móvil y escritorio de Historia de vida (mapa por etapas, panel o columna, lista para letra grande, etapas vacías y "Otros momentos"), el chat (botón fijo, burbujas de las acciones, invitación a pedido, apertura de la repregunta), el detalle del recuerdo (con "Respondiste a" y "Seguir completando") y la imagen descargable. Hasta entonces **el Figma ya no es la referencia de la pantalla de Historia de vida**: lo son este contrato y las imágenes del Anexo A.

## 9. Límites conocidos y riesgos

1. **Datos agotados:** tras 2 intentos por dato, "Seguir completando" deja de aparecer; no hay otra forma de corregir un recuerdo (no se edita).
2. **Recuerdos de respaldo:** si el modelo falla al crear un recuerdo, su título es el texto de la pregunta. El mapa lo muestra tal cual (la apertura de 2.11 no lo cita). Con el modelo funcionando es excepcional.
3. **Año estimado:** un recuerdo con año estimado cae en la etapa que corresponde a esa estimación; puede cambiar de etapa si después se completa el año.
4. **Una ilustración por etapa:** no refleja el tema del recuerdo.
5. **Frase de la regla 7 y la invitación:** pueden decir casi lo mismo (2.8 b).
6. **Detección de la frase:** es una heurística; hay falsos negativos. Para eso está el botón fijo.
7. **Descripciones más largas:** más riesgo de que el modelo agregue algo que la persona no dijo; lo cubre la validación existente y la prueba manual (sección 6). Los recuerdos ya guardados mantienen su descripción corta.
8. **Nombre en la imagen:** depende de que la app tenga el nombre de pila disponible; si no, sale "Mi vida".
9. **Privacidad de la imagen:** contiene títulos de recuerdos (2.5).

---

## Historial

- **v1 (12/11/2026):** primera versión. Mapa por etapas con panel, lista para letra grande y alto contraste, imagen descargable cuadrada, "Seguir completando", botón fijo e invitación a pedido (se elimina la automática), burbujas de lo que se toca, espera más ágil, repreguntas con contexto y descripción más larga del recuerdo.

- **v1, ajustes (12/11/2026):** ver la sección 2.5 (descarga directa; hasta 3 títulos por tarjeta), 2.8 a (botón fijo compacto en pantallas chicas) y 2.9 (respuesta fija de Alma tras "Contarte algo más" y "Ahora no").

---

## Anexo A. Referencias visuales

Carpeta `docs/referencias/mapa-de-vida/`:

| Archivo | Qué muestra |
|---|---|
| `mapa-pantalla-movil.png` | Pantalla móvil, con el panel cerrado (pastilla de aviso) y con el panel abierto en la etapa Infancia (tarjeta resaltada, fila con "Seguir completando este recuerdo"). |
| `mapa-pantalla-movil.svg` | Fuente de la anterior, con las medidas exactas. |
| `mapa-descarga-cuadrada.png` | La imagen descargable de 1080 × 1080 con el título "La vida de Ana", las seis paradas numeradas y el pie "Alma Acompaña" con corazón. |
| `mapa-descarga-cuadrada.svg` | Fuente de la anterior. |

**Los nombres, títulos y años de las imágenes son de ejemplo.** Las tipografías de las imágenes son equivalentes a las de la app (Poppins; el texto en Inter en lugar de Source Sans 3). En la app se usan las de la app; Source Sans 3 es más angosta, así que los títulos entran con más caracteres.

### Colores e ilustración por etapa (modo normal)

| Etapa | Banderín y texto fuerte | Fondo claro (círculo y borde de la tarjeta) | Ilustración |
|---|---|---|---|
| Infancia | `#993C1D` | `#F5C4B3` | barrilete |
| Adolescencia | `#534AB7` | `#CECBF6` | libro abierto |
| Juventud | `#185FA5` | `#B5D4F4` | corazón |
| Adultez | `#0F6E56` | `#9FE1CB` | casita |
| Vejez | `#854F0B` | `#FAC775` | sol |
| Otros momentos | `#5F5E5A` | `#D3D1C7` | estrella de cuatro puntas |

Otros colores: camino `#F4C0D1` con línea central punteada `#FFFBF4`; fondo crema `#FFFBF4`; título `#0F4C6B`; texto de apoyo `#5B6B78`; texto de las tarjetas `#2F3E4A`; violeta de la app y de la pastilla `#8E1B9B`; mástil `#3F4F5C`; corazón del pie `#D4537E`.

### Medidas del mapa en pantalla (unidades de diseño, ancho 360)

- **Camino:** `M96 70 C96 130 264 130 264 190 C264 250 96 250 96 310 C96 370 264 370 264 430 C264 490 96 490 96 550 C96 610 264 610 264 670`, ancho 28, extremos redondeados.
- **Paradas** en y = 70, 190, 310, 430, 550, 670. Las de índice par (Infancia, Juventud, Vejez): punto en x = 96, mástil en x = 156, banderín y tarjeta hacia la derecha (tarjeta de x = 156 a 332), ilustración en x = 46. Las de índice impar (Adolescencia, Adultez, Otros momentos): punto en x = 264, mástil en x = 204, banderín y tarjeta hacia la izquierda (tarjeta de x = 28 a 204), ilustración en x = 314.
- **Punto:** círculo de radio 9, fondo blanco, borde de 3 en el color fuerte. **Mástil:** línea de 2,5 de y − 38 a y + 40, con una bolita de radio 3 arriba. **Banderín:** de y − 34 a y − 12 (alto 22), ancho 116 (138 para "Otros momentos"), con una muesca de 12 en el extremo libre, texto blanco medium de unos 12,5. **Tarjeta:** 176 × 42, radio 8, de y − 6 a y + 36 (**6 de separación con el banderín**), borde de 1,5 en el color claro (3 en el color fuerte si está seleccionada; punteado si está vacía). **Ilustración:** círculo de radio 26 en el color claro.
- Los arbolitos de adorno están en (300, 128), (60, 248), (300, 368), (60, 488) y (300, 608).

### Medidas de la imagen descargable (1080 × 1080)

- Fondo `#FFFBF4`, borde rosa de 4 px a 22 px del límite, con radio 36. Título centrado con la línea de base en y = 140 ("La vida de" Poppins medium de 58; el nombre, Lora cursiva de 78, en violeta).
- Camino de 44 de ancho con línea central punteada. Puntos en (200, 400), (600, 400), (600, 680), (200, 680), (200, 960) y (600, 960), con el número de la parada adentro. El camino sale por la izquierda, da la vuelta por la derecha entre las filas 1 y 2 y por la izquierda entre las filas 2 y 3.
- Cada parada: mástil 40 px a la derecha de su punto, banderín de 214 × 40 sobre la tarjeta de 290 × 84, ilustración en un círculo de radio 30 a la derecha del banderín. Pie "Alma Acompaña" con corazón abajo a la derecha.
- Las medidas exactas están en el SVG de referencia, **salvo las de la tarjeta y el grupo de la parada, que cambiaron con la regla de 2.5**: mástil, banderín, ilustración y tarjeta están 14 px más arriba (el punto no se mueve); la tarjeta mide 84 px con 0 a 2 líneas y 96 px con 3, con las líneas de base a 28, 54 y 80 px del borde de arriba. Las imágenes de `docs/referencias/` ilustran la regla anterior (hasta 2 títulos).
