# Contrato de historia de vida (rama B): línea de tiempo, guion en el chat y recuerdos en el prompt

**Versión:** 4
**Rama:** `feature/historia-de-vida` (back y front)
**Parte de:** `feature/grafo` (ver 2.12 y 15)
**Complementa a:** `docs/contrato-grafo.md` (grafo, guion, extracción y repreguntas, endpoints 7.1 a 7.7) y `docs/contrato-chat.md` (chat).
**Casos de uso:** CU-02 (criterio de aceptación Nº 4), CU-03 (caminos A y B) y CU-04, en `docs/casos-de-uso.md`. Este contrato los sigue tal como están escritos: no hace falta modificarlos.

Este contrato cierra la última funcionalidad: lo que la persona mayor **ve y hace** con la historia de vida. El grafo, el guion, la extracción de recuerdos y las repreguntas ya existen en el back (rama `feature/grafo`). Acá se agregan: la línea de tiempo y el detalle de un recuerdo (back y front), el guion y las repreguntas dentro del chat (front), la tarjeta de pregunta pendiente, y los recuerdos y el progreso dentro del prompt del chat (back).

---

## 1. Alcance

### Entra

| Pieza | Qué es |
|---|---|
| **B1. Línea de tiempo** | Pantalla `/historia-de-vida` con los recuerdos como camino de paradas en orden cronológico y la sección "Sin fecha". Dos endpoints nuevos de lectura. |
| **B2. Detalle del recuerdo** | Pantalla `/historia-de-vida/:recuerdoId` con título, descripción, fecha, personas y lugares. |
| **B3. Pregunta pendiente** | Tarjeta "Próxima parada" al final del camino. Al tocarla se abre el chat con la pregunta como mensaje de Alma (CU-03, camino básico B). |
| **B4. Guion en el chat** | "Modo guion" en el chat: la invitación (CU-02, criterio Nº 4), las respuestas, los botones "Otra pregunta" y "Volver a Historia de vida", la espera de la extracción y las repreguntas, todo en la conversación. |
| **B5. Recuerdos en el prompt** | `{progreso}` y `{recuerdos}` del prompt del chat dejan de decir "No hay información." cuando hay datos. |
| **B6. Alarmas en el guion** | Las respuestas del guion pasan por el detector de riesgo del chat antes de extraer (ver 2.9). Ajusta 7.3 y 7.6 del contrato del grafo. |
| **B7. Cierre** | Accesibilidad, pruebas, actualización de documentos (ver 14). |

### No entra

- Editar o borrar recuerdos desde la app (el borrado de toda la historia ya existe en el back; no se expone en el front en esta rama).
- Búsqueda o filtros en la línea de tiempo. Las consultas por persona, lugar y etapa (P13 a P15) se usan **solo** para armar el prompt del chat (B5).
- Fotos, audio o dictado por voz.
- Vista del familiar/cuidador sobre la historia de vida.
- Cambios en el contrato del chat (el front del guion no usa `/api/chat/mensajes`; ver 2.1).

---

## 2. Decisiones tomadas

### 2.1 El guion se responde desde el chat, sin tocar el endpoint de chat

El chat es la pantalla donde la persona conversa; el guion se responde ahí. Pero el back **no** mezcla los dos mundos: `/api/chat/mensajes` sigue igual. Es el front el que sabe en qué modo está.

- El `ChatProvider` guarda en memoria el **objetivo activo** del guion:
  - `{ tipo: "PREGUNTA", preguntaId }` cuando Alma hizo una pregunta del guion.
  - `{ tipo: "REPREGUNTA", recuerdoId, dato }` cuando Alma hizo una repregunta (`dato` es `ANIO`, `LUGARES` o `PERSONAS`).
  - `null` cuando se conversa normal.
- Además guarda el **`preguntaOrigenId`**: el id de la pregunta del guion que dio origen a la charla actual. Hace falta para pedir la siguiente repregunta (`GET /repreguntas/siguiente?preguntaId=`): sin ese id, el back busca en todos los recuerdos y puede devolver uno de otra pregunta. Se fija al mostrar una pregunta del guion y se mantiene mientras dure la charla (preguntas y repreguntas de ese recuerdo).
- Con un objetivo activo, el campo de texto envía a `/api/historia/...` (7.1 o 7.2) en lugar de `/api/chat/mensajes`.
- El estado vive solo en memoria del `ChatProvider` (igual que los mensajes del chat): si se recarga la página, se pierde y el chat vuelve a modo normal. La pregunta no se pierde en el back: sigue pendiente y vuelve a ofrecerse.

### 2.2 La invitación es un mensaje de Alma con texto fijo

CU-02 (9.3 y camino alterno Nº 4) pide que Alma ofrezca continuar con la historia de vida cuando hay preguntas pendientes, y da un ejemplo de texto. Para que sea predecible y no gaste una llamada al modelo, la invitación **no la genera el modelo**: el front la muestra como un mensaje de Alma **a continuación de su respuesta**, con botones.

Se muestra cuando se cumple todo esto:

1. La persona ya recibió la primera respuesta de Alma en esta visita al chat.
2. `GET /api/historia/guion/estado` informa que hay preguntas pendientes.
3. No hay un objetivo activo.
4. La persona no tocó "Ahora no" en esta visita.
5. La invitación todavía no se mostró en esta visita.
6. No hubo una alarma en esta visita (ver 2.9).

La invitación aparece **una sola vez por visita** (una visita = mientras el `ChatProvider` siga montado). El texto es el del CU: "¿Seguimos completando tu historia de vida?". Tiene dos botones: **Sí** (pide `/guion/proxima`, muestra la introducción y la pregunta como mensaje de Alma y activa el objetivo `PREGUNTA`) y **Ahora no** (la descarta y no se vuelve a mostrar en esta visita; la pregunta sigue pendiente, CU-03 camino alterno Nº 1).

### 2.3 Qué pasa después de responder (espera y repregunta)

Al responder una pregunta (7.1) el back devuelve enseguida y la extracción corre en segundo plano (ver 5.4 del contrato del grafo). El front:

1. Muestra el indicador de espera existente ("Alma está pensando…", `ALMA_PENSANDO`). No dice nada del grafo ni de la extracción: es la misma espera de cualquier respuesta de Alma.
2. Consulta `GET /api/historia/repreguntas/siguiente?preguntaId=<preguntaOrigenId>` **cada 2 segundos** mientras la respuesta sea `PROCESANDO`.
3. Si responde `HAY`: muestra la repregunta como mensaje de Alma y activa el objetivo `REPREGUNTA`.
4. Si responde `NINGUNA`: muestra el texto de cierre (ver 6) y desactiva el objetivo. La persona elige "Contarte algo más" (vuelve a conversar normal) u "Otra pregunta" (se pide `/guion/proxima`).
5. Tope de espera: **60 segundos**. La extracción puede hacer hasta 2 llamadas al modelo de 25 s cada una, y comparte el modelo con el chat, así que el peor caso ronda los 50 s. Si se cumple el tope sin salir de `PROCESANDO`, se sigue como si fuera `NINGUNA` (sin repregunta). El recuerdo igual se guarda en el back cuando la extracción termina.

Los valores de 2 s y 60 s son constantes del front (ver 11.4) y se ajustan en las pruebas.

### 2.4 "Una repregunta por charla" lo resuelve el front

Igual que en 5.7 del contrato del grafo: el front manda `excluir` con los datos ya preguntados para ese recuerdo en la charla actual. Después de responder o saltear una repregunta, se vuelve a esperar y a pedir la siguiente, **una vez por dato**, hasta que el back responda `NINGUNA`. No se agregan reglas nuevas en el back.

### 2.5 Botones del modo guion (siguen el Figma)

Con una **pregunta** del guion en pantalla, debajo del mensaje de Alma aparecen dos botones grandes con texto:

- **Otra pregunta**: saltea la pregunta (7.4, `204`) y pide la siguiente (`/guion/proxima`). La introducción que trae el back ("Está bien, pasemos a otra.") ya contempla el salteo. Equivale a CU-03, camino alterno Nº 2.
- **Volver a Historia de vida**: navega a `/historia-de-vida`, desactiva el objetivo y no cambia nada en el back (la pregunta sigue pendiente). Equivale a CU-03, camino alterno Nº 3.

Con una **repregunta** en pantalla (no existe en el Figma) hay un botón **Saltear** (7.7) y el mismo "Volver a Historia de vida".

Encima de los botones va el texto de ayuda del Figma: "Contame con calma. Si hoy no tenés ganas de esa, probamos con otra." (solo con una pregunta, no con una repregunta).

Si no quedan preguntas (`quedanPendientes: false` en `/guion/proxima`), se muestra el `mensajeFinal` del back y se desactiva el objetivo.

### 2.6 Línea de tiempo: camino de paradas (sigue el Figma)

Los datos son una lista plana cronológica; el Figma la dibuja como un camino con paradas numeradas.

- Orden: de más antiguo a más reciente por `anio`. Los recuerdos con el mismo año se ordenan por `fechaRegistro`.
- Cada recuerdo con año es una **parada numerada** (1, 2, 3…, según su posición cronológica) con año y título.
- Al final del camino, la parada "?" es la tarjeta de la próxima pregunta (B3).
- **Móvil y escritorio:** se usa el mismo punto de corte que el resto de la app, `lg` de Tailwind (`64rem`, 1024 px). Por debajo de `lg` es móvil; desde `lg`, escritorio. No se define otro.
- **Escritorio:** se muestran todas las paradas.
- **Móvil:** se muestran las **3 más recientes** y, arriba, el botón "Ver recuerdos anteriores (N)", que despliega el resto. La numeración no cambia (en el Figma las visibles son 3, 4 y 5).
- Los recuerdos sin año no entran al camino: van debajo, en una sección **"Sin fecha"**, ordenados por `fechaRegistro`.

### 2.6.1 Cómo se muestra el año

Según la `precision` del recuerdo:

| Precisión | Se muestra |
|---|---|
| `EXACTA` | `1957` |
| `CALCULADA_POR_EDAD` | `1957` |
| `APROXIMADA` | `Alrededor de 1957` |
| `ESTIMADA` | `Alrededor de 1957` |
| (sin año) | Sección "Sin fecha"; no se muestra año |

(`CALCULADA_POR_EDAD` se muestra como año simple porque sale de una cuenta con la fecha de nacimiento y la edad que dijo la persona.)

### 2.7 Detalle del recuerdo: ruta propia

El detalle es la ruta `/historia-de-vida/:recuerdoId`, con botón "Volver" (`EnlaceVolver`). El id es un UUID (String), no numérico: hace falta un hook de ruta nuevo (ver 11.3), porque `useIdDeRuta` solo acepta números.

### 2.8 Dos endpoints de lectura nuevos

- `GET /api/historia/recuerdos`: listado para la línea de tiempo (P10; la ausencia de recuerdos sale de P10, ver 5.3).
- `GET /api/historia/recuerdos/{recuerdoId}`: detalle (P12).

Quedan separados para que el listado sea liviano (sin descripción completa de cada recuerdo) y el detalle traiga personas y lugares.

### 2.9 Alarmas en las respuestas del guion (ajuste a 7.3 y 7.6)

Una respuesta del guion es texto libre de la persona, igual que un mensaje del chat. Si dice que se siente muy mal o que necesita una ambulancia, no puede ser "procesada como recuerdo" en silencio.

- 7.1 y 7.2 de este contrato (7.3 y 7.6 del contrato del grafo) pasan el texto por `DetectorDeRiesgo.detectar(...)` (el mismo del chat) **antes** de encolar.
- Si detecta riesgo (`MALESTAR_GRAVE` o `URGENCIA_MEDICA`): se responde con el texto predefinido del chat (`ConfiguracionChat`, el mismo que usa el chat para ese riesgo), **no se encola la extracción** y **no se guarda ningún recuerdo** de esa respuesta. La pregunta sigue pendiente.
- Código de respuesta: `202` con cuerpo `{"estado":"ACEPTADA"}` cuando se acepta (sigue siendo 202, así que los tests actuales y el contrato del grafo no cambian en ese caso) y `200` con `{"estado":"ALARMA","respuesta":"..."}` cuando salta la alarma.
- En el front, después de una alarma **no se vuelve a ofrecer la invitación en esa visita** (2.2, condición 6).
- Esto contradice la regla 5.9 del contrato del grafo ("no se clasifica ni se generan alertas a partir del contenido"). Esa regla se ajusta: sigue sin loguearse el texto y sin clasificar el contenido por tema, pero las respuestas del guion pasan por el detector de riesgo del chat (ver 14).

### 2.10 Los recuerdos llegan al prompt sin llamar al modelo

Para elegir qué recuerdos agregar a `{recuerdos}` no se hace ninguna llamada extra al modelo: se compara el mensaje de la persona (normalizado) con los nombres de personas y lugares del grafo y con una lista fija de palabras de etapa de vida (ver 5.5). Máximo 3 recuerdos.

### 2.11 `ChatService` y `ContextoDelChat` no leen Neo4j

La lectura del grafo para el prompt pasa por un servicio de historia (ver 10.3). El chat solo recibe texto ya armado.

### 2.12 Base de la rama y riesgo

La rama `feature/historia-de-vida` sale de `feature/grafo`, **no** de `main` con el grafo ya integrado. Consecuencia: si la revisión de `feature/grafo` pide cambios en el back de historia (endpoints, repositorios, textos), este contrato se reabre y se ajusta. Mientras tanto, este contrato asume el back de `feature/grafo` tal como está.

### 2.13 Privacidad: los recuerdos se mandan al modelo

`{recuerdos}` lleva al modelo nombres de personas y lugares, títulos y descripciones que la persona contó. Es justamente lo que permite que Alma personalice la charla. El contrato del chat ya preveía "progreso y recuerdos" en el prompt, y el de grafo (5.9) ya establece que con el modelo en la nube lo que se cuenta sale del equipo y que para la PPS se usan datos sintéticos. Se mantiene esa postura: con usuarios reales corresponde el perfil local del modelo.

Lo que el chat no recibe es el nombre, el usuario ni la fecha de nacimiento **de la cuenta**. Lo que la persona cuenta en sus respuestas sí viaja al modelo, tanto en el prompt del chat como en la extracción de recuerdos (que recibe el texto de la respuesta y puede incluir nombres propios y cualquier dato personal que la persona haya dicho).

---

## 3. Convenciones

- Todas las rutas de historia viven bajo `/api/historia` y solo las usa el rol `PERSONA_MAYOR` (ya configurado en `SecurityConfig`).
- La persona se identifica por el token, nunca por un id en la URL. Todas las consultas arrancan del nodo `PersonaMayor` de la persona autenticada (aislamiento, ver 5.6 del contrato del grafo).
- Los ids de `Recuerdo`, `Persona` y `Lugar` son UUID (String). El de `PreguntaGuion` es Integer.
- Textos de cara a la persona: español rioplatense, trato de "vos", frases cortas (mismo criterio que el resto de la app).
- El front no loguea ni guarda en `localStorage` el texto de las respuestas ni de los recuerdos.
- El chat vive en la ruta `/` (no existe `/chat`).

---

## 4. Datos

No hay cambios en el modelo del grafo. Se leen los nodos y relaciones existentes:

```
(PersonaMayor)-[:TIENE_RECUERDO]->(Recuerdo)
(Recuerdo)-[:OCURRIO_EN]->(Lugar)
(Persona)-[:PARTICIPA_EN]->(Recuerdo)
(PersonaMayor)-[:CONOCE {vinculo}]->(Persona)
```

Campos usados de `Recuerdo`: `id`, `titulo`, `descripcion`, `anio` (puede ser nulo), `precision`, `etapaDeVida`, `fechaRegistro`.
Campos usados de `Persona`: `id`, `nombre`, `nombreNormalizado`, y `vinculo` (de la relación `CONOCE`, puede ser nulo).
Campos usados de `Lugar`: `id`, `nombre`, `nombreNormalizado`.

Consultas a implementar (en `LecturaDelGrafo`, con `Neo4jClient`, siguiendo `docs/Modelo_de_grafo/consultas.cypher`):

| Consulta | Uso |
|---|---|
| P10 | Línea de tiempo: recuerdos de la persona ordenados por `anio` (nulos al final). |
| P11 | **No se implementa.** ¿Tiene recuerdos? (para el estado vacío): el estado vacío sale de P10, que devuelve una lista vacía (ver 5.3). |
| P12 | Detalle de un recuerdo con personas y lugares. |
| P13 | Recuerdos donde participa una persona (por `nombreNormalizado`). |
| P14 | Recuerdos que ocurrieron en un lugar (por `nombreNormalizado`). |
| P15 | Recuerdos de una etapa de vida. |
| P16 / P17 | Nombres de personas y lugares de la persona mayor (para detectar menciones en el mensaje). |

---

## 5. Reglas

### 5.1 Orden de la línea de tiempo

1. Con año: por `anio` ascendente; empate por `fechaRegistro` ascendente.
2. Sin año: al final, por `fechaRegistro` ascendente.

### 5.2 Un recuerdo ajeno no existe

`GET /recuerdos/{id}` busca el recuerdo **a partir del nodo de la persona autenticada**. Si el id no existe o pertenece a otra persona, responde 404 (no 403): no se revela que exista.

### 5.3 Estado vacío

Si P10 no devuelve ningún recuerdo (P11 no se implementa: una consulta más para saber lo que P10 ya dice), el listado devuelve lista vacía (200) y el front muestra el estado vacío (ver 6), que invita a empezar a responder preguntas (CU-04, camino alterno Nº 1), junto con la tarjeta de pregunta pendiente si corresponde.

### 5.4 Pregunta pendiente (B3)

La tarjeta "Próxima parada" aparece al final del camino cuando `GET /api/historia/guion/estado` informa preguntas pendientes. No muestra el texto de la pregunta (como en el Figma). Al tocar "Seguir escribiendo":

1. Se pide `GET /api/historia/guion/proxima`.
2. Se navega al chat (`/`).
3. El chat agrega la introducción y la pregunta como mensaje de Alma, fija el `preguntaOrigenId` y activa el objetivo `PREGUNTA`.
4. Si ya hay un objetivo activo, se muestra el mensaje ya existente y no se duplica.
5. Si `quedanPendientes` es `false` (otra pestaña la respondió), se muestra el `mensajeFinal` y se refresca la pantalla.

### 5.5 Recuerdos y progreso en el prompt (B5)

**Progreso (`{progreso}`)** — solo cantidades, del mismo estado de las preguntas que informa `/guion/estado` (P4), pero **leído en solo lectura con `LecturaDelGrafo.estadoDeTodas`, sin pasar por `/guion/estado`** (`HistoriaDeVidaService.estado` hace el `MERGE` del nodo de la persona y abre una transacción de escritura; leer para el chat no puede crear nada):

- Con alguna respuesta o salteo: `Respondió 4, salteó 1, quedan 16.`
- Sin ninguna respuesta ni salteo: `No hay información.` (`MensajesChat.SIN_INFORMACION`, como hoy).
- Una persona sin nodo en el grafo (todavía no usó el módulo de historia): `No hay información.`, tanto en `{progreso}` como en `{recuerdos}`, **y el chat no le crea el nodo**.

**Recuerdos (`{recuerdos}`)** — hasta **3** recuerdos relacionados con el mensaje actual:

1. Se normaliza el mensaje con `DetectorDeRiesgo.normalizar`.
2. Se busca cada nombre de persona y de lugar del grafo (P16/P17) como palabra completa dentro del mensaje normalizado. **El nombre guardado se vuelve a normalizar con la misma función `DetectorDeRiesgo.normalizar` antes de comparar**: `NombreNormalizado.de` conserva la puntuación ("escuela n° 12") y `normalizar` la elimina ("escuela n 12"), así que comparar uno contra otro no coincidiría. Cada coincidencia pide sus recuerdos (P13/P14, que siguen buscando por el `nombreNormalizado` guardado).
3. Se buscan frases de etapa de vida de una lista fija (ver 5.5.1). Cada coincidencia pide los recuerdos de esa etapa (P15).
4. Se juntan los resultados sin repetir, y se toman 3 como máximo: primero los de coincidencia por persona o lugar, después los de etapa; dentro de cada grupo, los más recientes por `fechaRegistro`.
5. Cada recuerdo se escribe en una línea: `- <título> (<año>): <descripción>. Con <persona> (<vínculo>), en <lugar>.` Las partes que no existen se omiten. Detalle del formato:
   - Sin año: `- <título>: <descripción>.`
   - Sin lugar ni personas: se corta tras la descripción.
   - Con lugar y sin personas, el lugar va con mayúscula: `... <descripción>. En <lugar>.`
   - Varias personas o varios lugares se separan con `, ` (`Con Marta (hermana), Rosa (madre), en Cipolletti, la costa.`).
   - Una persona sin nombre se escribe solo con el vínculo (`Con marido`); una con nombre y sin vínculo, solo con el nombre (`Con Elena`).
   - **Sin punto doble:** si la descripción ya termina en `.` o en `…` (por el corte de 5.5.2), no se le agrega otro punto; si no termina en ninguno de los dos, se le agrega uno.
   - **Una sola línea por recuerdo:** los saltos de línea y los grupos de espacios del título, la descripción, las personas y los lugares pasan a un solo espacio.
   - Sin descripción pero con personas o lugares: `- <título>. Con <persona>.`
6. Si no hay ninguna coincidencia, `{recuerdos}` queda en `No hay información.`

Ejemplo de línea:

```
- Mi maestra Elena (1957): Su maestra de primer grado, que le enseñó a leer. Con Elena (maestra), en Escuela N° 12.
```

#### 5.5.1 Frases de etapa

Lista fija, configurable en `historia.properties`, normalizada:

| Etapa | Frases |
|---|---|
| `INFANCIA` | "de chica", "de chico", "cuando era chica", "cuando era chico", "de nina", "de nino", "cuando era nina", "cuando era nino" |
| `ADOLESCENCIA` | "de adolescente", "cuando era adolescente" |
| `JUVENTUD` | "de joven", "cuando era joven", "cuando era jovencita", "cuando era jovencito" |
| `ADULTEZ` | "de adulta", "de adulto", "cuando era adulta", "cuando era adulto" |

(La etapa `VEJEZ` no tiene frases: es la etapa actual de la persona y no aporta contexto de "recuerdo".)

#### 5.5.2 Límites del prompt

- **Corte de textos** (la descripción de cada recuerdo en el prompt, a **300** caracteres, y el `resumen` del listado, a **140**): se toman los primeros N caracteres, se quitan los espacios del final y se agrega "…" si se cortó. Un texto cortado puede medir entonces N + 1 caracteres. Un texto que mide N o menos queda igual.
- Los reemplazos de `{progreso}` y `{recuerdos}` se hacen en la misma pasada que los demás marcadores (como hoy en `ContextoDelChat.instrucciones`): un recuerdo que contenga el texto `{animo}` o `{progreso}` **no** se vuelve a reemplazar.
- **Las fallas no rompen el chat.** Una falla al leer el grafo para el prompt se registra (solo el tipo de error, sin el texto del mensaje ni de los recuerdos) y **la parte que falló** queda en `No hay información.`: el progreso y los recuerdos fallan por separado (si falla uno, el otro se arma igual).
- **Presupuesto de tiempo propio.** Toda la lectura del grafo para el prompt (progreso y recuerdos juntos) tiene un tope de **2 s** (`alma.historia.lectura-chat-timeout-ms`, ver 10.5). Corre en un ejecutor chico y acotado, de **4 hilos y sin cola**: si el tope se cumple, o si los 4 hilos están ocupados (se rechaza de inmediato, sin hacer esperar al mensaje), el chat sigue con `No hay información.` en las dos partes y se registra un aviso sin texto de la persona. El mensaje no espera más que el tope, aunque Neo4j esté caído o la red descarte paquetes (el driver tardaría hasta unos 21 s por consulta). No cambia la configuración del driver, así que las escrituras no se ven afectadas.
- **Una lectura cancelada por el tope no es un error (v4).** `conTope` marca una bandera propia de esa lectura (`AtomicBoolean`) **antes** de llamar a `cancel(true)`, y los `catch` de `progreso()` y `recuerdos()` la consultan además de la marca del hilo: el driver o Spring Data pueden limpiar la marca antes de que la lectura llegue al `catch`, y la excepción que sale de una consulta cortada a la mitad (por ejemplo `InvalidDataAccessResourceUsageException`) no sirve para distinguirla. Una lectura cancelada se registra en INFO (solo el tipo de la excepción) y el WARN del tope ya avisó; un fallo real de Neo4j sigue en ERROR.
- **Una lectura cancelada deja de ejecutar consultas.** Cuando el tope cancela la lectura, el hilo queda interrumpido; antes de cada consulta (por cada persona y cada lugar mencionados, por cada etapa y entre el progreso y los recuerdos) se mira la marca de interrupción y, si está puesta, no se hace ninguna más.
- **Lectura tolerante de la precisión y la etapa** (listado y detalle, 7.3 y 7.4): si el valor de `precision` o de `etapaDeVida` guardado en el grafo no es uno de los enums (por ejemplo, cargado a mano en Neo4j), se devuelve `null` en ese campo; no se lanza error y el recuerdo aparece igual (200).

### 5.6 Respuestas del guion y alarmas (B6)

7.1 y 7.2, en este orden:

1. Validar el texto: no vacío (400 con `RESPUESTA_OBLIGATORIA`) y largo máximo igual al de 7.3 del contrato del grafo (1000 caracteres).
2. `DetectorDeRiesgo.detectar(texto)`.
3. Si hay riesgo: responder `200` con `ALARMA` y el texto predefinido; **no** encolar, **no** marcar la pregunta como respondida y **no** contar intentos. La pregunta sigue pendiente. **No se valida que la pregunta (o el recuerdo y el dato, en las repreguntas) exista ni que esté sin responder:** con riesgo, una pregunta inexistente o ya respondida da `200 ALARMA` y no `404` ni `409` (la seguridad va primero). El texto sale de `ConfiguracionChat.Respuestas.para(TipoDeRiesgo)`, el mismo método que usa `ChatService`.
4. Si no hay riesgo: continuar como hoy (guardar respuesta, encolar extracción) y responder `202` con `ACEPTADA`.

### 5.7 Cuando algo falla en la espera

- Si `repreguntas/siguiente` devuelve un error de red o 5xx durante la espera, el front reintenta una vez; si vuelve a fallar, sigue sin repregunta (como en el tope de 60 s) y muestra el aviso de error genérico.
- Un 409 (ver 9) en cualquier llamada de historia muestra el aviso correspondiente y refresca el estado (`/guion/estado`).

### 5.8 Respuesta vacía en modo guion

El campo de texto no envía un mensaje vacío. En modo guion, si la persona intenta enviar vacío, el front muestra el texto "Escribí una respuesta o elegí saltear la pregunta." (el mismo de `RESPUESTA_OBLIGATORIA` del back, CU-03 camino alterno Nº 5). Fuera del modo guion, el campo sigue como hoy.

### 5.9 Largo del texto en modo guion

El campo del chat corta en 500 caracteres (`LIMITE_MENSAJE`). En modo guion el campo admite **1000**, que es lo que acepta el back para una respuesta del guion; los recuerdos se cuentan con más palabras que un mensaje de chat.

---

## 6. Textos

### Del front (fijos)

| Dónde | Texto |
|---|---|
| Invitación en el chat | "¿Seguimos completando tu historia de vida?" |
| Botón de la invitación (acepta) | "Sí" |
| Botón de la invitación (rechaza) | "Ahora no" |
| Introducción y pregunta del guion | Las del back (`introduccion` y `pregunta.texto` de `/guion/proxima`) |
| Ayuda debajo de una pregunta | "Contame con calma. Si hoy no tenés ganas de esa, probamos con otra." |
| Botón con una pregunta (saltea y sigue) | "Otra pregunta" |
| Botón con una pregunta o repregunta (navega) | "Volver a Historia de vida" |
| Botón con una repregunta | "Saltear" |
| Respuesta vacía en modo guion | "Escribí una respuesta o elegí saltear la pregunta." |
| Cierre cuando no hay repregunta | "¿Querés contarme algo más sobre esto o te hago otra pregunta?" |
| Botones del cierre | "Contarte algo más" / "Otra pregunta" |
| Cuando ya no quedan preguntas | El `mensajeFinal` del back |
| Título de la pantalla | "Historia de vida" |
| Botón de recuerdos ocultos (móvil) | "Ver recuerdos anteriores (N)" |
| Sección sin año | "Sin fecha" |
| Prefijo de año aproximado | "Alrededor de" |
| Estado vacío de la línea de tiempo | "Todavía no tenemos recuerdos. Respondé las preguntas de Alma y van a aparecer acá." |
| Tarjeta de pregunta pendiente | "Próxima parada" / "Alma tiene una pregunta" / botón "Seguir escribiendo" |
| Detalle: etiqueta de personas | "Con quién" |
| Detalle: etiqueta de lugares | "Dónde" |
| Recuerdo no encontrado | `MENSAJE_NO_ENCONTRADO` (existente) |
| Error al cargar | `ACCION_PROBAR_DE_NUEVO` y mensaje genérico (existentes) |

**Ojo con "Otra pregunta":** es el mismo texto en dos botones que hacen cosas distintas. Debajo de una pregunta **sin responder**, saltea esa pregunta (`/salteo`) y pide la siguiente. En el **cierre** (después de responder), la pregunta anterior ya está respondida: solo pide la siguiente (`/guion/proxima`), sin llamar a ningún salteo. Se implementan como dos acciones distintas (`otraPregunta()` y la del cierre).

### Del back

- Respuestas de alarma de 7.1 y 7.2: las mismas de `ConfiguracionChat` para `MALESTAR_GRAVE` y `URGENCIA_MEDICA` (no se duplican textos), que se eligen con `ConfiguracionChat.Respuestas.para(TipoDeRiesgo)`.
- Introducciones, preguntas, repreguntas y `mensajeFinal`: los de `MensajesHistoria` y `guion.json`. No cambian.

---

## 7. Endpoints

Base: `/api/historia`. Todos requieren rol `PERSONA_MAYOR`.

### Cambios en endpoints existentes

#### 7.1 `POST /guion/{preguntaId}/respuesta` (ajusta 7.3 del contrato del grafo)

Antes: `202 Accepted` sin cuerpo. Ahora, con cuerpo:

- `202 Accepted`: la respuesta se guardó y la extracción quedó encolada. El front empieza la espera (2.3).

```json
{ "estado": "ACEPTADA" }
```

- `200 OK`: el detector de riesgo se activó (ver 5.6). No se guardó ni se encoló nada; la pregunta sigue pendiente. **No se validó que la pregunta exista ni que siga sin responder:** con riesgo no hay `404` ni `409`. El front muestra `respuesta` como mensaje de Alma y **desactiva** el objetivo.

```json
{ "estado": "ALARMA", "respuesta": "<texto predefinido>" }
```

Errores: los de 7.3 del contrato del grafo (400 por texto vacío o demasiado largo, 404 pregunta inexistente, 409 pregunta ya respondida o salteada). El `400` por largo va **antes** de detectar el riesgo (un texto de más de 1000 caracteres da `400` aunque tenga una frase de riesgo); el `404` y el `409` solo se dan si no hay riesgo.

#### 7.2 `POST /repreguntas/{recuerdoId}/{dato}/respuesta` (ajusta 7.6 del contrato del grafo)

Igual que 7.1: `202` con `{ "estado": "ACEPTADA" }` o `200` con `{ "estado": "ALARMA", "respuesta": "..." }`. Con `ALARMA` no se cuenta el intento ni se completa el dato, y **no se valida que el recuerdo exista ni que el dato aplique** (no hay `404` ni `409`). El `400` por largo va antes de detectar el riesgo.

#### Sin cambios

- `POST /guion/{preguntaId}/salteo` → `204` (7.4 del contrato del grafo).
- `POST /repreguntas/{recuerdoId}/{dato}/salteo` → `204` (7.7 del contrato del grafo).
- `GET /guion/proxima` (7.1 del grafo), `GET /guion/estado` (7.2) y `GET /repreguntas/siguiente` (7.5): sin cambios.

### Endpoints nuevos

#### 7.3 `GET /recuerdos`

Listado para la línea de tiempo. Ordenado según 5.1.

```json
{
  "recuerdos": [
    {
      "id": "6f0c0c1e-2b8a-4f3e-9d1a-0c4f5a7e1b22",
      "titulo": "Mi maestra Elena",
      "anio": 1957,
      "precision": "APROXIMADA",
      "etapaDeVida": "INFANCIA",
      "resumen": "Su maestra de primer grado, que le enseñó a leer."
    },
    {
      "id": "…",
      "titulo": "El día que conocí a Carlos",
      "anio": null,
      "precision": null,
      "etapaDeVida": null,
      "resumen": "…"
    }
  ]
}
```

- `resumen` es la `descripcion` cortada a 140 caracteres (con "…" si se cortó).
- Lista vacía → `{ "recuerdos": [] }` con 200.
- El back ya los devuelve en el orden correcto; el front separa los que tienen `anio` (camino) de los que no (sección "Sin fecha").

#### 7.4 `GET /recuerdos/{recuerdoId}`

```json
{
  "id": "6f0c0c1e-2b8a-4f3e-9d1a-0c4f5a7e1b22",
  "titulo": "Mi maestra Elena",
  "descripcion": "Su maestra de primer grado, que le enseñó a leer.",
  "anio": 1957,
  "precision": "APROXIMADA",
  "etapaDeVida": "INFANCIA",
  "personas": [
    { "id": "…", "nombre": "Elena", "vinculo": "maestra" }
  ],
  "lugares": [
    { "id": "…", "nombre": "Escuela N° 12" }
  ]
}
```

- `personas` y `lugares`: listas, vacías si no hay.
- `vinculo`: puede ser `null`.
- 404 si el recuerdo no existe o no es de la persona (5.2).

---

## 8. Acceso

| Quién | Qué puede |
|---|---|
| `PERSONA_MAYOR` | Todo lo de este contrato, solo sobre su propia historia. |
| Otros roles | 403, por la regla `/api/historia/**` ya configurada. |
| Sin sesión | 401. |

En el front, `/historia-de-vida` y `/historia-de-vida/:recuerdoId` quedan dentro del layout de la persona mayor (como el resto de las pantallas de esa persona).

---

## 9. Errores

Formato ya existente de la app. Nuevos o relevantes:

| Código | Cuándo |
|---|---|
| 400 | Texto vacío o demasiado largo en 7.1 y 7.2. |
| 401 / 403 | Sin sesión / rol incorrecto. |
| 404 | Pregunta, recuerdo o dato inexistente, o recuerdo de otra persona (5.2). **No se da en 7.1 y 7.2 si el texto activa el detector de riesgo** (5.6). |
| 409 | `ConflictoException`: pregunta ya respondida o salteada; la repregunta ya no corresponde (dato que no aplica, que ya no falta o que ya se preguntó el máximo de veces). **No se da en 7.1 y 7.2 si el texto activa el detector de riesgo** (5.6). |

La alarma (`estado: "ALARMA"`) **no** es un error: es una respuesta válida con 200. Con riesgo detectado no se valida que la pregunta o el recuerdo existan ni que estén sin responder: la seguridad va primero.

---

## 10. Back

Paquete `ar.edu.utn.frm.gelab.alma.historia` (y un cambio chico en `chat`).

### 10.1 Controller

`HistoriaController`:

- Agregar `GET /recuerdos` y `GET /recuerdos/{recuerdoId}` (7.3 y 7.4).
- Ajustar `POST .../respuesta` de pregunta y de repregunta: devolver `202` con `ACEPTADA` o `200` con `ALARMA` (7.1 y 7.2).

DTOs nuevos (records) en `historia/dto`: `ListaDeRecuerdosResponse`, `RecuerdoResumenResponse`, `RecuerdoDetalleResponse`, `PersonaDeRecuerdoResponse`, `LugarDeRecuerdoResponse`, `RespuestaDelGuionResponse` (`estado`, `respuesta` opcional).

### 10.2 Repositorio

`LecturaDelGrafo`: agregar las consultas P10 y P12 a P17 (sección 4; P11 no se implementa), con `Neo4jClient` y records de lectura, sin entidades. Todas arrancan del nodo `PersonaMayor` (Long id = id de PostgreSQL de la persona).

### 10.3 Servicios

- `ConsultaDeRecuerdos` (nuevo): arma el listado (5.1), el detalle (5.2) y el resumen de 140 caracteres.
- `RecuerdosParaElChat` (nuevo, en `historia/service`): recibe `cuentaId` (como los demás servicios de historia) y el mensaje; devuelve dos textos listos: `progreso` y `recuerdos` (5.5). Es lo único de historia que usa el chat.
- `HistoriaDeVidaService` y `RepreguntaService`: en el registro de la respuesta, agregar el chequeo de riesgo (5.6) antes de guardar o encolar. Devuelven el resultado (`ACEPTADA` o `ALARMA` con el texto).
- Transacciones: las lecturas no necesitan transacción de escritura. Las escrituras siguen con `@Transactional("neo4jTransactionManager")`.

### 10.4 Chat

- `ContextoDelChat.instrucciones(cuentaId)`: reemplazar los dos valores fijos `MensajesChat.SIN_INFORMACION` de `progreso` y `recuerdos` por lo que devuelva `RecuerdosParaElChat`. Para eso `instrucciones` recibe también el mensaje actual: se agrega ese parámetro y `ChatService.responder` lo pasa.
- `ChatService` no importa nada de `neo4j` ni de `LecturaDelGrafo`.
- Falla de lectura → `No hay información.` (5.5.2).

### 10.5 Configuración

En `historia.properties` / `ConfiguracionHistoria`:

- Frases de etapa de vida (5.5.1).
- Máximo de recuerdos en el prompt: 3.
- Largo máximo de la descripción en el prompt: 300; del resumen del listado: 140.
- Tope de tiempo de la lectura del grafo para el prompt del chat: **2000 ms** (`alma.historia.lectura-chat-timeout-ms`, `getLecturaChatTimeoutMs`).
- Máximo de lecturas del grafo para el chat a la vez: **4 hilos**, sin cola (`alma.historia.lectura-chat-hilos-max`, `getLecturaChatHilosMax`). Con los 4 ocupados, la lectura se rechaza de inmediato (5.5.2).
- Las claves de `historia.properties` son `alma.historia.max-recuerdos-prompt`, `alma.historia.descripcion-prompt-max`, `alma.historia.resumen-max` y `alma.historia.frases-etapa.<etapa>` (frases separadas por coma).

---

## 11. Front

Todo en `frontend/src`.

### 11.1 Estructura

```
features/historia/
  index.ts
  rutas.ts                 // HISTORIA_DE_VIDA, DETALLE_DE_RECUERDO
  api.ts                   // llamadas a /api/historia
  tipos.ts
  mensajes.ts              // textos de la sección 6
  anio.ts                  // formatearAnio(precision, anio)
  useIdDeRecuerdo.ts       // lee :recuerdoId (String)
  useRecuerdos.ts / useRecuerdo.ts / useEstadoDelGuion.ts   // TanStack Query
  pages/
    HistoriaDeVidaPage.tsx
    RecuerdoDetallePage.tsx
  components/
    CaminoDeRecuerdos.tsx
    ParadaDeRecuerdo.tsx
    TarjetaProximaParada.tsx
    SeccionSinFecha.tsx
    InvitacionHistoria.tsx
```

Se sigue el patrón de `features/lecturas/` y el modelo de `features/entretenimiento/rutas.ts` para el archivo de rutas.

### 11.2 Pantallas

**`HistoriaDeVidaPage`** (`/historia-de-vida`, reemplaza a `PaginaProvisoria`):

1. Título "Historia de vida".
2. En móvil (por debajo de `lg`, ver 2.6), botón "Ver recuerdos anteriores (N)" si hay más de 3 recuerdos con año.
3. `CaminoDeRecuerdos`: una `ParadaDeRecuerdo` numerada por cada recuerdo con año, en orden (2.6), y al final `TarjetaProximaParada` si hay pregunta pendiente (5.4).
4. `SeccionSinFecha` debajo, si hay recuerdos sin año.
5. Estado vacío (6) si no hay recuerdos.
6. Estados de carga y de error con las constantes existentes (`ESTADO_CARGANDO`, `ACCION_PROBAR_DE_NUEVO`).
7. Cada parada es un enlace a `/historia-de-vida/:recuerdoId`.

**`RecuerdoDetallePage`** (`/historia-de-vida/:recuerdoId`): `EnlaceVolver`, título, año (con `formatearAnio`), descripción, "Con quién" y "Dónde" (solo si hay). 404 → `MENSAJE_NO_ENCONTRADO`.

### 11.3 Hook de id de ruta

`useIdDeRuta` solo acepta ids numéricos. Se agrega `useIdDeRecuerdo` que lee `:recuerdoId` como texto y valida el formato UUID; si no es válido, trata el recuerdo como no encontrado (sin llamar al back).

### 11.4 Chat: modo guion

Cambios en `features/chat`:

- `contexto.ts` / `ChatProvider`: agregar al estado el **objetivo activo** y el `preguntaOrigenId` (2.1), `invitacionMostrada`, `invitacionRechazada`, `huboAlarma` y el estado de espera (`esperandoRepregunta`). Exponer: `objetivoActivo`, `responderGuion(texto)`, `otraPregunta()`, `saltearRepregunta()`, `volverAHistoria()`, `abrirPreguntaDelGuion()` (la usa la tarjeta de la pantalla de historia), `aceptarInvitacion()`, `rechazarInvitacion()`.
- `enviar(texto)`: si hay objetivo activo, llama a 7.1 o 7.2 en lugar de `/chat/mensajes`; si no, igual que hoy.
- Si la respuesta es `ALARMA` (200): se muestra `respuesta` como mensaje de Alma, se limpia el objetivo y se marca `huboAlarma`.
- Si es `ACEPTADA` (202): se muestra el mensaje de la persona y se arranca la espera (2.3) con constantes `INTERVALO_ESPERA_MS = 2000` y `TOPE_ESPERA_MS = 60000`.
- `CampoMensaje`: en modo guion `maxLength` es 1000 (5.9) y un envío vacío muestra el texto de 5.8.
- `ChatPage`: en modo guion muestra, debajo del mensaje de Alma, los botones de 2.5. La pregunta del guion se muestra como mensaje de Alma con la introducción y la pregunta destacada, como en el Figma.
- `InvitacionHistoria`: se muestra según 2.2.
- Se mantiene el límite de 45 s de `enviarMensaje` solo para el chat; la espera de la extracción tiene su propio tope.

Los mensajes del modo guion se agregan a la misma lista de mensajes del chat, con el mismo formato (`MensajeAlma` para Alma).

### 11.5 `lib/api.ts`

No se toca. 7.1 y 7.2 devuelven siempre cuerpo (`202` o `200`) y los salteos devuelven `204`, que `api()` ya maneja.

### 11.6 Rutas y navegación

- `App.tsx`: reemplazar `PaginaProvisoria` de `/historia-de-vida` por `HistoriaDeVidaPage` y agregar `/historia-de-vida/:recuerdoId`.
- `NAVEGACION_HISTORIA_DE_VIDA` ("Historia de vida") ya existe en `lib/mensajes.ts`; se reutiliza para el ítem de navegación.
- La navegación principal marca "Historia de vida" como activa también en el detalle de un recuerdo.
- El chat está en `/`: tanto la tarjeta "Próxima parada" como "Volver a Historia de vida" navegan entre `/` y `/historia-de-vida`.

### 11.7 Accesibilidad

- Botones grandes y con texto (nada de solo íconos) en "Sí", "Ahora no", "Otra pregunta", "Volver a Historia de vida", "Saltear" y las opciones de cierre.
- Las paradas del camino se navegan con teclado y tienen nombre accesible (título + año). El número de la parada y la línea punteada son decorativos.
- El indicador "Alma está pensando…" se anuncia a lectores de pantalla (`aria-live="polite"`).
- Los mensajes nuevos de Alma (invitación, repregunta, cierre) se anuncian igual que los del chat.

### 11.8 Diferencias con el Figma

El contrato sigue el Figma en la línea de tiempo (camino con paradas, "Ver recuerdos anteriores", parada "?"), en la tarjeta de pregunta pendiente y en los botones del chat. Estas son las únicas diferencias:

| Figma | Contrato | Por qué |
|---|---|---|
| El chat con pregunta abre con "Seguimos con tu historia, Laura. Me quedé pensando en una cosa." | Se usa la introducción fija del back (primera vez, "Está bien, pasemos a otra." o una de tres frases) | CU-03 define esas introducciones como texto fijo sin consultar al modelo. |
| No hay lugar para recuerdos sin año | Sección "Sin fecha" debajo del camino | El back puede devolver recuerdos sin año. |

Pantallas y estados que **no están en el Figma** y se resuelven con los componentes existentes del chat y de la app: detalle de un recuerdo, estado vacío de la línea de tiempo, invitación del chat, indicador de espera de la repregunta, repreguntas (con su botón "Saltear") y cierre ("¿Querés contarme algo más…?").

---

## 12. Cambios en archivos compartidos

| Archivo | Cambio |
|---|---|
| `HistoriaController.java` | Endpoints 7.3 y 7.4; cuerpo en 7.1 y 7.2. |
| `HistoriaDeVidaService.java`, `RepreguntaService.java` | Chequeo de riesgo en respuestas del guion. |
| `LecturaDelGrafo.java` | Consultas P10 a P17. |
| `ContextoDelChat.java`, `ChatService.java` | `{progreso}` y `{recuerdos}` con datos reales; parámetro del mensaje. |
| `ConfiguracionChat.java`, `ChatService.java` | `ConfiguracionChat.Respuestas.para(TipoDeRiesgo)`: el texto predefinido de cada riesgo. `ChatService` lo usa en lugar de elegir el texto por su cuenta, y `HistoriaDeVidaService` y `RepreguntaService` lo comparten (5.6). El comportamiento del chat no cambia. |
| `historia.properties`, `ConfiguracionHistoria.java` | Frases de etapa, límites, tope de tiempo y hilos de la lectura para el chat (10.5). |
| `ChatIntegrationTests.java` | **No se tocó.** Las aserciones de las líneas 297 y 298 siguen válidas (sin historia → `No hay información.`). Las pruebas nuevas del prompt con historia están en archivos aparte (ver 13). |
| `HistoriaGuionIntegrationTests.java` | Las aserciones `isAccepted` siguen igual (el 202 se mantiene). La que comparaba el cuerpo vacío (`content().string("")`) pasó a comprobar el cuerpo `{"estado":"ACEPTADA"}`. Ahora inyecta `ModeloDePrueba` y lo reinicia en el `@BeforeEach` (para comprobar que con alarma no se encola nada), y agrega las pruebas de alarma. |
| `RepreguntaEndpointsIntegrationTests.java` | Igual: `isAccepted` sin cambios y el cuerpo `ACEPTADA` en lugar del cuerpo vacío. La aserción de OpenAPI (`losTresEndpointsEstanEnLosEsquemas...`) antes exigía que el `POST .../respuesta` **no** documentara un `200`; ahora exige que lo documente (es la `ALARMA`), y lo mismo para el del guion. |
| `RecuerdosDePrueba.java` (test) | Helpers nuevos: `descripcion`, `etapa` y `agregarPersona` (con `CONOCE {vinculo}` y `PARTICIPA_EN`), para armar recuerdos con datos distintos. |
| `frontend/src/App.tsx` | Rutas nuevas. |
| `frontend/src/features/chat/*` | Modo guion (11.4). |
| `docs/contrato-grafo.md` | Ajuste de 5.9 y nota en 7.3/7.6 (ver 14). |
| `docs/casos-de-uso.md` | Estado de CU-04 (ver 14). |
| `CLAUDE.md` | Enlace a este contrato (ver 14). |

---

## 13. Pruebas y criterio de listo

### Back (automáticas, integración)

**Convención de nombres.** Los métodos de prueba van en camelCase, como en el resto del código (`sinTokenDevuelve401`), y no con el prefijo `test_historia_...`: ese prefijo es el de los **usuarios** de prueba (`test_historia_a`, `test_historia_chat_a`...), que cada prueba crea y borra con `eliminarHistoria`. Se usa `ModeloDePrueba` (extractor simulado) y `GrafoDePrueba`/`RecuerdosDePrueba`.

**Dónde viven.** Las pruebas del prompt con historia viven en el paquete `historia` (`RecuerdosEnElChatIntegrationTests`, `RecuerdosEnElChatFallaTest`, `RecuerdosEnElChatLecturaLentaTest`, `RecuerdosParaElChatTopeTest`, `TextoDeRecuerdosTest`), y no en `chat`: los ayudantes `ModeloDePrueba`, `RecuerdosDePrueba` y `PersonasDeHistoria` son privados de ese paquete y `ModeloDePrueba` guarda el prompt que recibe. Se leen del `POST /api/chat/mensajes`. `ChatIntegrationTests` no se tocó; la prueba de `Respuestas.para` está en `chat` (`RespuestasParaTest`).

- Listado: orden por año, empate por `fechaRegistro`, sin año al final; lista vacía.
- Detalle: personas y lugares; 404 de recuerdo inexistente; 404 de recuerdo de **otra** persona.
- Aislamiento: una persona no ve los recuerdos de otra en listado ni detalle.
- Respuesta del guion: `ACEPTADA` (202, con cuerpo) encola la extracción; `ALARMA` (200; malestar grave y urgencia médica) devuelve el texto predefinido, **no** encola, **no** guarda y la pregunta sigue pendiente.
- Repregunta con alarma: no cuenta intento ni completa el dato.
- Prompt: `{progreso}` con cantidades; sin respuestas queda `No hay información.`; `{recuerdos}` por nombre de persona, por lugar y por frase de etapa; un nombre con puntuación ("Escuela N° 12") se detecta si la persona escribe "escuela n 12" o "Escuela N° 12"; máximo 3; descripción cortada a 300; un recuerdo con `{animo}` en el texto no se reemplaza; falla del grafo → `No hay información.` y el chat responde igual.

### Front (manual, como en los otros módulos)

1. Línea de tiempo con recuerdos con y sin año; "Alrededor de" solo en aproximada y estimada; numeración de las paradas.
2. Móvil: solo las 3 últimas paradas y "Ver recuerdos anteriores (N)" despliega el resto; escritorio: todas.
3. Estado vacío.
4. Tarjeta "Próxima parada" → chat (`/`) con la introducción y la pregunta como mensaje de Alma.
5. Invitación: aparece una vez, después de la primera respuesta de Alma; "Ahora no" no vuelve en la visita; "Sí" muestra la pregunta.
6. Responder una pregunta → espera → repregunta (con el `preguntaId` correcto) → responder → siguiente → cierre.
7. "Otra pregunta" (saltea y muestra "Está bien, pasemos a otra.") y "Volver a Historia de vida"; "Saltear" en una repregunta.
8. Enviar vacío en modo guion → texto de 5.8.
9. Respuesta de 1000 caracteres en modo guion; en el chat normal sigue el tope de 500.
10. Respuesta con alarma ("me siento muy mal") → texto de ayuda, sin repregunta y sin reofrecer la invitación en esa visita.
11. Tope de espera de 60 s (con el extractor demorado) → sigue sin repregunta.
12. Cuando no quedan preguntas → `mensajeFinal` del back.
13. Detalle de un recuerdo, volver, y un id inválido.
14. Recarga del chat en modo guion → vuelve a modo normal y la pregunta sigue pendiente.
15. Teclado y lector de pantalla en lo nuevo (11.7).

### Listo cuando

- Todas las pruebas de back pasan, incluidas las existentes del grafo y del chat.
- El front compila sin errores de tipos ni de lint.
- Los 15 puntos manuales pasan.
- Ningún log del back ni del front contiene el texto de respuestas o recuerdos.

---

## 14. Documentos a actualizar al cerrar la rama

- `docs/contrato-grafo.md`: en 5.9, aclarar que las respuestas del guion pasan por el detector de riesgo del chat; en 7.3 y 7.6, agregar que la respuesta aceptada lleva cuerpo `{ "estado": "ACEPTADA" }` y que con alarma devuelve `200` (referencia a este contrato); subir la versión y agregar la línea al historial.
- `docs/casos-de-uso.md`: marcar lo implementado de CU-02 (criterio Nº 4), CU-03 y CU-04. No se cambia el texto de los casos.
- `CLAUDE.md`: enlazar este contrato.
- Este contrato: pasar a "implementado" con la versión final.

---

## 15. Límites conocidos y riesgos

1. **Base `feature/grafo`.** Si la revisión de esa rama cambia el back de historia, este contrato se reabre (2.12).
2. **Estado en memoria.** El objetivo activo, el `preguntaOrigenId`, la invitación y la espera viven en el `ChatProvider`; recargar la página los pierde (la pregunta sigue pendiente en el back).
3. **Extracción lenta.** Si el modelo tarda más de 60 s, la repregunta se pierde para esa charla (el recuerdo sí se guarda). Los 2 s y 60 s son ajustables.
4. **Detección de recuerdos por nombre.** Puede no encontrar un recuerdo si la persona no dice el nombre tal cual lo guardó el extractor ("la seño" vs "Elena"), o traer uno que no es el pedido si el nombre es común. Es un límite aceptado de no usar el modelo (2.10).
5. **Alarma por palabras clave.** `DetectorDeRiesgo` usa listas de frases: puede no detectar formulaciones raras. Es el mismo límite que ya tiene el chat.
6. **Una sola extracción a la vez.** La cola de extracción es en memoria y de un solo hilo (como en el grafo): si el back se reinicia, las extracciones encoladas se pierden y la pregunta queda como respondida sin recuerdo.
7. **Sin edición.** Si la extracción se equivoca, la persona no puede corregir el recuerdo desde la app en esta rama.
8. **Datos en la nube.** Los recuerdos viajan al modelo en la nube dentro del prompt (2.13); con usuarios reales corresponde el perfil local.
9. **El detector de riesgo es el del chat, con sus mismos límites.** Es el mismo `DetectorDeRiesgo` con las mismas listas de `chat.properties`. Una frase de duelo en pasado como "cuando se murió mi mamá me quería morir de la tristeza" **no activa la alarma** (las listas detectan "me quiero morir" y "quisiera/quería morirme", no "me quería morir"): la respuesta se guarda y se encola como cualquier otra (`limiteConocido_unaFraseDeDueloConMeQueriaMorirNoActivaLaAlarma` lo documenta; si se ajusta el detector, hay que actualizar la prueba). En cambio, una frase de duelo con "se suicidó" **sí activa la alarma** (la lista incluye `suicid\w+`), aunque la persona hable de otra persona, y esa respuesta no se guarda: el recuerdo se pierde y la pregunta sigue pendiente. Es el mismo comportamiento que ya tiene el chat.
10. **Los paquetes `historia` y `chat` quedan acoplados en las dos direcciones.** `historia.service` importa `DetectorDeRiesgo` y `ConfiguracionChat` (alarmas), y `chat.service` importa `RecuerdosParaElChat` (prompt). No hay ciclo de beans (Spring los resuelve sin problema), pero un cambio en uno puede afectar al otro. Es lo que pide este contrato (2.9 y 2.11).
11. **Una lectura colgada sigue hasta que el driver la corta.** El tope de 2 s no mata la consulta: la cancela (interrumpe su hilo) y el chat sigue, pero un socket bloqueado puede no responder a la interrupción y el hilo queda ocupado hasta que el driver corte la conexión (unos 21 s por consulta si la red descarta paquetes; 0,3 s si el puerto está cerrado). Por eso el ejecutor tiene un máximo de 4 hilos y, con los 4 ocupados, las lecturas se rechazan de inmediato y los mensajes salen sin recuerdos.
12. **El primer mensaje tras un rato inactivo puede salir sin recuerdos.** Con el pool de conexiones del driver frío (primera conexión, o una conexión vencida), la primera lectura puede tardar más de 2 s y pasar el tope: ese mensaje va con `No hay información.` en `{progreso}` y `{recuerdos}`, aunque la persona tenga historia. Los siguientes ya van con datos. El tope es ajustable (10.5). **Mitigación (v4):** al arrancar, `CalentamientoDeNeo4j` abre el pool y ejecuta las dos lecturas del chat con un id de persona inexistente (solo `MATCH`/`RETURN`, no escribe ni crea nada), en un hilo aparte y sin romper el arranque; se apaga con `alma.historia.calentar-neo4j=false`. Cubre el primer mensaje después de arrancar, no el que sigue a una pausa larga.
13. **Sin cortacircuitos (decisión v4).** Con Neo4j caído o con la red descartando paquetes, cada mensaje del chat pierde hasta el tope de 2 s antes de seguir sin recuerdos. No se salta el grafo durante un tiempo tras una falla, porque eso haría perder los recuerdos del prompt aunque Neo4j ya hubiera vuelto. Si hiciera falta (por ejemplo, con un Neo4j inestable en producción), se agrega un enfriamiento de unos 30 s solo ante `ServiceUnavailable` o timeout.

---

## Historial

| Versión | Cambio |
|---|---|
| 1 | Primer borrador para revisión. |
| 2 | Correcciones tras la revisión contra el código, los casos de uso y el Figma. **Código:** el chat está en `/`; el `preguntaOrigenId` se guarda para pedir la repregunta correcta; el mensaje de Alma lleva la introducción del back y el cierre sin preguntas usa el `mensajeFinal` del back; la respuesta aceptada sigue siendo 202 (ahora con cuerpo) y solo la alarma devuelve 200; `RecuerdosParaElChat` recibe `cuentaId`; el tope de espera pasa a 60 s; nombre y mensaje se normalizan con la misma función; texto de respuesta vacía en modo guion; tope de 1000 caracteres en modo guion; se quitó un 409 que no existe. **Casos de uso:** la invitación usa el texto del CU-02 y se muestra como mensaje de Alma a continuación de su respuesta; no se reofrece tras una alarma. **Figma:** camino de paradas con "Ver recuerdos anteriores", tarjeta "Próxima parada", botones "Otra pregunta" y "Volver a Historia de vida" (reemplazan al "Saltear" junto al campo), sección 11.8. **Privacidad:** decisión explícita en 2.13 (aclarando que lo que la persona cuenta viaja al modelo, en el chat y en la extracción). **Otros:** referencia cruzada del encabezado corregida, punto de corte móvil/escritorio definido (`lg`) y nota sobre los dos botones "Otra pregunta". Se quitó la sección "Cómo usar con Claude Code". |
| 3 | Ajustes para que el contrato refleje lo que hizo el código al armar el back (sin cambiar el comportamiento pedido). **P11 no se implementa:** el estado vacío sale de P10 (secciones 2.8, 4, 5.3 y 10.2). **5.5:** el progreso se lee en solo lectura con `estadoDeTodas`, sin pasar por `/guion/estado`; una persona sin nodo queda en `No hay información.` sin que se le cree el nodo; formato de la línea del recuerdo (`En <lugar>` con lugar y sin personas, separadores `, `, persona sin nombre solo con el vínculo, sin punto doble, saltos de línea a espacios). **5.5.2:** corte a N más "…"; progreso y recuerdos fallan por separado; tope de 2 s y ejecutor de 4 hilos sin cola con rechazo inmediato; una lectura cancelada deja de ejecutar consultas al ser interrumpida; precisión y etapa tolerantes (`null` si el valor del grafo no es válido). **5.6, 6, 7.1, 7.2 y 9:** con riesgo detectado no se valida que la pregunta o el recuerdo existan ni que estén sin responder (`200 ALARMA`, la seguridad va primero; el `400` por largo va antes) y el texto sale de `ConfiguracionChat.Respuestas.para(TipoDeRiesgo)`. **10.5:** tope de 2000 ms y 4 hilos. **12:** `ConfiguracionChat` y `ChatService`, `RecuerdosDePrueba`, `HistoriaGuionIntegrationTests`, `RepreguntaEndpointsIntegrationTests` (la aserción de OpenAPI pasó a exigir el `200`) y que `ChatIntegrationTests` no se tocó. **13:** convención de nombres de pruebas y dónde viven las del prompt. **15:** límites nuevos (9 a 12). |
| 4 | Lectura del grafo para el chat: una lectura cancelada por el tope se detecta con una bandera propia y no se registra como ERROR (5.5.2); calentamiento de Neo4j al arrancar (límite 12); sin cortacircuitos, anotado como límite 13. |

---

## Anexo A. Flujo del modo guion en el chat

```
Persona entra al chat (/)
  └─ Alma responde (primera respuesta de la visita)
       └─ estado.pendientes > 0, sin "Ahora no" y sin alarma  →  Invitación (Sí / Ahora no)
            └─ Sí → GET /guion/proxima → introducción + pregunta → objetivo = PREGUNTA, preguntaOrigenId = id
                 ├─ Otra pregunta → POST .../salteo (204) → GET /guion/proxima (introducción de salteo)
                 ├─ Volver a Historia de vida → navega a /historia-de-vida (objetivo = null)
                 └─ Persona responde → POST .../respuesta
                      ├─ 200 ALARMA   → texto predefinido, objetivo = null (pregunta sigue pendiente)
                      └─ 202 ACEPTADA → "Alma está pensando…" → GET /repreguntas/siguiente?preguntaId=<origen> cada 2 s (tope 60 s)
                           ├─ HAY     → repregunta → objetivo = REPREGUNTA
                           │            ├─ Saltear → POST .../salteo → siguiente
                           │            └─ Responde → POST .../respuesta → espera → siguiente (una por dato)
                           └─ NINGUNA / tope → "¿Querés contarme algo más sobre esto o te hago otra pregunta?"
                                ├─ Contarte algo más → chat normal
                                └─ Otra pregunta → GET /guion/proxima (o mensajeFinal si no quedan)
```

## Anexo B. Ejemplo de prompt con datos

Mensaje de la persona: "Hoy me acordé de Elena, de cuando era chica."

```
Historia de vida: Respondió 4, salteó 1, quedan 16.
Recuerdos relacionados:
- Mi maestra Elena (1957): Su maestra de primer grado, que le enseñó a leer. Con Elena (maestra), en Escuela N° 12.
- Las siestas en el patio (1955): Dormía la siesta bajo la parra con su abuela. Con Rosa (abuela), en Casa de la abuela.
```

(El primero entra por coincidencia con el nombre "Elena"; el segundo por la frase "de chica", que trae recuerdos de `INFANCIA`.)
