# Contrato del Grafo: guion, progreso, extractor y repreguntas · Alma Acompaña

**Estado:** implementado. Es la referencia única para `feature/grafo` (solo back). Si algo cambia, se cambia primero acá.

**Fuente de verdad:** los casos de uso CU-03 y CU-04 (`docs/casos-de-uso.md`) y el modelo de grafo (`docs/Modelo_de_grafo/modelo-de-grafo.md`, con `restricciones.cypher`, `consultas.cypher` y `datos-de-ejemplo.cypher`). Si este contrato contradice esos documentos, ganan los documentos y se corrige el contrato, salvo en lo que este contrato declara expresamente como ajuste al modelo (sección 4.1).

**Dónde va:** `docs/contrato-grafo.md`. El `CLAUDE.md` lo enlaza en la sección "Documentos".

**Última revisión contra el repo:** commit `3815f04` (rama `feature/grafo`, hasta el paso 6 del checklist) más los cambios del paso 7 (v14), sobre el commit `d35f1a9` de `main`.

**Contratos relacionados:** `docs/contrato-login.md` (token, roles, errores, convenciones), `docs/contrato-chat.md` (el modelo de IA y su límite de un pedido a la vez) y `docs/contrato-fecha-nacimiento.md` (precondición, ver sección 2). Todo lo que no se redefine acá sigue como está en esos documentos.

**Responsable:** Paula (módulo Historia de vida). Rama: `feature/grafo`, a partir de `main`. La segunda rama (`feature/historia-de-vida`: línea de tiempo, pregunta pendiente y chat) tendrá su propio contrato y va después.

---

## 1. Alcance

**Incluye:**
- El grafo en Neo4j con sus cinco nodos y siete relaciones (`docs/Modelo_de_grafo/modelo-de-grafo.md`), sus restricciones de unicidad y la conexión transaccional.
- El guion de 21 preguntas de historia de vida y su carga automática al arrancar.
- El progreso de cada persona: próxima pregunta, responder, saltear y estado del guion (consultas P1 a P5).
- El extractor: convierte la respuesta de la persona en un recuerdo con personas y lugares, en segundo plano, con validación, un reintento y un respaldo.
- La cola de extracciones y la prioridad del chat sobre ellas.
- Las repreguntas, que completan el año, los lugares y las personas de un recuerdo.
- La baja del subgrafo de una persona y los datos de ejemplo para `ana`.
- Pruebas automáticas y la prueba manual con el modelo real.

**No incluye (segunda rama):**
- La línea de tiempo y el detalle de un recuerdo (P10 a P12) y todo el front.
- Que Alma ofrezca continuar la historia de vida y que las respuestas viajen por el chat (CU-02, camino alterno Nº4). En esta rama la persona responde por los endpoints de la sección 7, que se prueban con Swagger.
- Los recuerdos en el prompt del chat (P13 a P15): `{progreso}` y `{recuerdos}` siguen valiendo "No hay información."
- Editar o borrar recuerdos desde la aplicación.
- Crear, editar o borrar preguntas del guion desde el panel de operador (ver "Límites conocidos", sección 12, para saber cómo se haría).
- Que lo que la persona cuente de más, en la charla libre, se sume al recuerdo (mejora futura).
- Guardar las mascotas como parte del grafo: el modelo no tiene un tipo de nodo para ellas (mejora futura, sección 12).

---

## 2. Decisiones

| Tema | Decisión | Motivo |
|---|---|---|
| Precondición | La fecha de nacimiento de `PersonaMayor` es **obligatoria** (`docs/contrato-fecha-nacimiento.md`). Ese cambio se hace en esta misma rama, en un commit propio, antes de empezar con el resto. Este contrato no trata el caso de una persona sin fecha | El año por edad, el año estimado y la etapa de vida dependen de ella |
| Estilo de acceso | **Imperativo** (`Neo4jRepository`, `Neo4jClient`, `List` y `Optional`). Nada reactivo (`Mono`, `Flux`, WebFlux). No se mezclan los dos estilos | El back usa `spring-boot-starter-webmvc`. Buena parte de la documentación de Spring Data Neo4j usa ejemplos reactivos: se ignoran |
| Cómo se lee | Cada consulta de `docs/Modelo_de_grafo/consultas.cypher` que haga falta va como un método con Cypher explícito que devuelve un DTO (un `record`), leído con `Neo4jClient` y un mapeo propio (clase `LecturaDelGrafo`). Un `@Query` de un repositorio con columnas sueltas no sirve con Spring Data Neo4j 8.1.1: la proyección se resuelve contra la entidad del repositorio y falla. Los `Neo4jRepository` quedan para leer un nodo como entidad. **No** se confía en que Spring cargue el grafo de una persona a través de las entidades | El mapeo ingenuo de relaciones carga mucho más del grafo de lo previsto. Con Cypher explícito el ancla en `PersonaMayor` (aislamiento entre personas) se ve en el código |
| Cómo se escribe | Con Cypher explícito (`MERGE` y `CREATE`) por `Neo4jClient` o `@Query`, **siempre anclado en el nodo `PersonaMayor`**. Cada operación es una sola transacción | Evita duplicados (regla de integridad 5), se puede repetir sin romper nada y respeta la regla de anclaje |
| Entidades | `@Node` finas, una por tipo de nodo, **sin listas de relaciones** salvo las que haga falta para escribir. `@RelationshipProperties` solo si hace falta para `RESPONDIO`/`SALTEO` (`fecha`) y `CONOCE` (`vinculo`) | Mismo motivo que arriba |
| Identificadores | `PersonaMayor.idPersonaMayor`: **`Long`, el mismo id que `PersonaMayor.id` en PostgreSQL**. `PreguntaGuion.idPreguntaGuion`: `Integer` fijo en el archivo del guion. `Recuerdo`, `Persona` y `Lugar`: **`String` con un UUID generado por la aplicación** | PostgreSQL genera sus ids solos, pero Neo4j no tiene un contador seguro (su número interno se puede reutilizar tras un borrado, y la baja de una persona borra su subgrafo). Un UUID se genera con una línea y no necesita coordinación; un número propio exigiría un contador dentro de Neo4j. Es la única excepción a "ids `Long`" del resto del proyecto |
| Restricciones | Las seis de `docs/Modelo_de_grafo/restricciones.cypher`, creadas al arrancar (ya llevan `IF NOT EXISTS`). Spring Data no las crea | Reglas de unicidad del modelo |
| Transacciones | El proyecto tiene JPA y Neo4j a la vez. `ConfiguracionNeo4j` declara **los dos manejadores de forma explícita**: el de JPA con el nombre `transactionManager` y `@Primary` (el resto del proyecto sigue usándolo sin cambios, con `@Transactional` sin calificar), y el de Neo4j con el nombre `neo4jTransactionManager`. El módulo de historia usa solo el segundo (`@Transactional("neo4jTransactionManager")`). `@EnableNeo4jRepositories` fija el manejador (`transactionManagerRef = "neo4jTransactionManager"`) y el paquete de los repositorios de Neo4j (`historia.repository`); al declararlo, Spring Boot deja de configurar esos repositorios por su cuenta. **Se comprueba primero con una prueba (checklist, punto 1)**, antes de agregar la configuración: qué manejador existe hoy y que los servicios con `@Transactional` sin calificar usan el de JPA; y después, con la configuración, que eso no cambia y que una escritura al grafo se deshace si falla a la mitad. Los resultados se anotan en el historial de este contrato | Spring Boot define un solo bean `transactionManager` con `@ConditionalOnMissingBean`, tanto para JPA como para Neo4j, y ninguna configuración automática va antes que la otra. Si solo se agregara el manejador de Neo4j, el de JPA dejaría de crearse y todos los `@Transactional` sin calificar pasarían al de Neo4j sin avisar |
| Concurrencia en responder y saltear | Al empezar la transacción de responder o saltear (5.1 y 5.3), se **bloquea el nodo `PersonaMayor`** de la persona (por ejemplo, con un `SET` sobre una propiedad de ese nodo). Así, dos pedidos de la misma persona se atienden uno detrás del otro y no pueden pasar a la vez el chequeo de "sin `RESPONDIO` ni `SALTEO`". Se prueba con un test concurrente | Los `MERGE` de relaciones de tipos distintos (`RESPONDIO` y `SALTEO`) no se bloquean entre sí: sin el bloqueo, una respuesta y un salteo simultáneos podrían quedar los dos registrados (regla 3) |
| Dos bases, sin transacción común | No hay transacción que abarque PostgreSQL y Neo4j. La baja de una persona borra primero en PostgreSQL y después el subgrafo, y el borrado del subgrafo se puede repetir sin error | El único dato compartido entre las bases es el id de la persona |
| Nodo de la persona | Se crea con `MERGE` la **primera vez** que la persona usa el módulo (7.1 a 7.4; las repreguntas, 7.5 a 7.7, parten de un recuerdo que ya existe y no lo crean). No guarda otros datos | El modelo no duplica datos de perfil |
| Guion | 21 preguntas en `src/main/resources/historia/guion.json` (Anexo A). **La carga solo crea las preguntas que no existen** (por `idPreguntaGuion`); no modifica las que ya están. Cambiar el texto de una pregunta existente, durante el desarrollo, se hace a mano en el Neo4j Browser o recreando el volumen. Las preguntas nuevas se agregan al final con `orden` mayor y no se reordena | La fuente de verdad pasa a ser el grafo y no el archivo: así, si más adelante un operador edita el guion, un reinicio no le pisa los cambios |
| Repreguntas aplicables | Cada `PreguntaGuion` lleva una propiedad `repreguntas`: la lista ordenada de datos por los que tiene sentido repreguntar (`ANIO`, `LUGARES`, `PERSONAS`). Va en el nodo y no solo en el archivo (Anexo A) | "¿Dónde pasó esto?" no tiene sentido después de "¿Qué comida de tu infancia te acordás?". Guardarla en el nodo la hace editable más adelante |
| Presentación de la pregunta | El back arma, con textos fijos y sin consultar al modelo, una **introducción** que acompaña a la pregunta (sección 5.2). Las preguntas del guion no llevan la introducción adentro | Es predecible, no gasta el único turno del modelo y no cambia el sentido de la pregunta |
| Progreso | Se deduce de las relaciones `RESPONDIO` y `SALTEO`. No hay un puntero aparte (regla 4) | Modelo de grafo |
| Orden de respuesta | Se puede responder o saltear **cualquier** pregunta pendiente, no solo la próxima. Una pregunta ya respondida o salteada no se puede volver a responder (regla 3) | El front siempre muestra la próxima; no hace falta bloquear de más |
| La pregunta se marca al momento | Al responder, la pregunta queda `RESPONDIO` **de inmediato**, sin esperar la extracción, y la extracción se encola. Si la extracción falla, igual queda respondida (respaldo, 5.4) | CU-03, paso 9: el progreso se guarda con cada respuesta |
| Cola de extracciones | **En memoria** (un solo hilo). Si el back se reinicia, se pierden las que estaban esperando. Se acepta: la respuesta ya quedó registrada como respondida, aunque su recuerdo no llegue a crearse | Persistir la cola en PostgreSQL es más robusto y bastante más trabajo; no se justifica en el MVP |
| Prioridad del chat | `LimitadorDePedidos` pasa a tener **dos niveles**: el chat espera su turno hasta 10 s como hoy, y la extracción solo toma el turno si **ningún chat está esperando** y **espera sin tope de tiempo** (el de 10 s es solo del chat; v12). Una extracción que ya está llamando al modelo **no se corta**: cada llamada tiene un tope propio de 25 s, así que en un caso raro el chat puede recibir el `503` de siempre. Es una limitación aceptada | Cortar la extracción en curso suma complejidad que no se justifica en el MVP |
| Modelo | El mismo `gemma4:cloud` (`OLLAMA_MODEL`). Razonamiento desactivado, **temperatura 0,1** y tope de **600** tokens de salida, propios de la extracción (el chat sigue con 0,6 y 400). Se agrega a `ia/` la forma de pedir parámetros distintos por llamada | Para extraer datos conviene una temperatura baja (0 a 0,2) |
| Repreguntas: cuántas veces | **Cada dato de cada recuerdo se pregunta como máximo 2 veces en total, una por charla.** No hay tope de repreguntas seguidas: dentro de una misma charla se puede preguntar cada dato faltante una vez (hasta 3 seguidas). Cuenta como intento tanto responder (aunque no aporte el dato) como saltear. El back cuenta los intentos con tres propiedades numéricas del recuerdo (4.1); "una por charla" lo resuelve el front (5.6) | El servidor no guarda sesiones, así que no sabe qué es "una charla"; los contadores son de tamaño fijo y no crecen con el grafo |
| Estado de la extracción | Mientras una extracción está en cola o en curso, el back lo informa en `GET /api/historia/repreguntas/siguiente` (7.5). No hay notificaciones ni sockets | CU-03: el procesamiento es transparente, sin feedback técnico |
| Baja de la persona | Hoy el panel de operador **no** da de baja personas (solo ejercicios y microcápsulas). Se implementa `HistoriaDeVidaService.eliminarHistoria(personaId)` y se prueba, pero **no se engancha a ningún endpoint**. Cuando exista la baja de personas, tiene que llamarla | Regla de integridad 8 |
| Datos de ejemplo | Una historia ya cargada para `ana` (Anexo B), en la misma carga inicial que el guion, solo si `alma.seed.enabled=true` y solo si `ana` **no tiene recuerdos ni progreso** (ninguna relación `RESPONDIO` ni `SALTEO`): si ya respondió o salteó algo, por ejemplo desde Swagger, no se le agrega nada, para no chocar con lo que contestó (regla 3). La carga se hace con un `ApplicationListener<ApplicationReadyEvent>`, que corre después de los `ApplicationRunner` de datos de prueba y por eso encuentra a `ana` ya creada en PostgreSQL (`DatosDePrueba` no tiene `@Order` y no se toca). Las restricciones y el guion se cargan siempre, con o sin `alma.seed.enabled` | La segunda rama puede empezar sin esperar al extractor |
| Fuera del chat | Esta rama **no toca** `chat/`. El chat sigue como en `docs/contrato-chat.md` | Se cambia en la segunda rama |

---

## 3. Convenciones de la API

Valen las de `docs/contrato-login.md` (sección 3). Para este módulo:

- Todo cuelga de `/api/historia`.
- La persona sale siempre del token (`sub`), nunca de la URL ni del cuerpo.
- Los textos viajan como texto plano.
- Los enums van en `MAYUSCULAS_CON_GUION_BAJO`: `EstadoPregunta` (`RESPONDIDA`, `SALTEADA`, `PENDIENTE`), `DatoFaltante` (`ANIO`, `LUGARES`, `PERSONAS`), `EstadoRepregunta` (`PROCESANDO`, `HAY`, `NINGUNA`).
- Los ids de recuerdos son `String` (UUID); los de preguntas, números.

---

## 4. Datos

### 4.1 Neo4j

Los cinco nodos, las siete relaciones y las propiedades son los de `docs/Modelo_de_grafo/modelo-de-grafo.md`, con estos **ajustes** (hay que reflejarlos en ese documento, ver sección 13):

| Cambio | Detalle |
|---|---|
| Tipo de id | Ver sección 2: `Long` para `PersonaMayor`, `Integer` para `PreguntaGuion`, `String` con UUID para `Recuerdo`, `Persona` y `Lugar` |
| `PreguntaGuion.repreguntas` (nueva, lista de texto, puede estar vacía, hasta 3 elementos) | Los datos por los que se puede repreguntar sobre esa pregunta, en orden de prioridad. Es una propiedad de la pregunta, igual para todas las personas |
| `Recuerdo.intentosAnio`, `Recuerdo.intentosLugares`, `Recuerdo.intentosPersonas` (nuevas, enteros, valen 0 si no existen) | Cuántas veces se preguntó por cada dato de ese recuerdo. Se suman 1 al responder o saltear una repregunta y nunca pasan de 2. No las muestra ningún endpoint de lectura de la segunda rama |
| Enums | `precision` y `etapaDeVida` se guardan como texto y se controlan con enums de Java, como dice el modelo |

Las reglas de integridad que las restricciones no cubren las valida la capa de servicio: 3 (estado de la pregunta), 5 (sin duplicados), 6 (coherencia temporal) y 7 (repreguntas).

### 4.2 Lo que se lee de PostgreSQL

Con `PersonaMayorRepository.findByCuentaId(cuentaId)`: `id`, `nombre`, `apellido` y `fechaNacimiento` (obligatoria). El apellido sirve para descartar a la propia persona cuando el modelo la devuelve con nombre y apellido (5.4, paso 5). Si no existe la `PersonaMayor`, `404`.

"Hoy" y el año actual salen de `LocalDate.now(clock)` del `Clock` inyectado.

### 4.3 Nada nuevo en PostgreSQL

No hay tablas ni cambios en entidades JPA.

---

## 5. Reglas de negocio

### 5.1 Responder una pregunta

1. Se busca la `PersonaMayor`; si no existe, `404`.
2. Se valida el cuerpo (5.8), **antes de tocar el grafo**: un pedido inválido no crea nada en Neo4j.
3. Se hace `MERGE` del nodo de la persona en el grafo y, ya dentro de la transacción de Neo4j, se **bloquea ese nodo** (sección 2, "Concurrencia en responder y saltear"). Se busca la pregunta; si no existe, `404`.
4. Se registra `RESPONDIO` con la `fecha` en **una sola consulta** que solo crea la relación si la persona aún no tiene `RESPONDIO` ni `SALTEO` hacia esa pregunta. Si ya la tenía, `409` y no se encola nada (regla 3, también con dos pedidos a la vez).
5. Se encola la extracción (5.4) y se responde `202`.

### 5.2 Próxima pregunta y su introducción

La próxima pregunta es la de menor `orden` sin `RESPONDIO` ni `SALTEO` (P1). Junto con ella el back devuelve una **introducción** de texto fijo, según el caso:

| Caso | Cuándo | Introducción |
|---|---|---|
| Primera vez | La persona no respondió ni salteó ninguna pregunta | "Me gustaría conocerte mejor. Te voy a hacer preguntas de a una sobre tu vida, y podés saltear las que quieras." |
| Después de un salteo | La última pregunta registrada (P3) fue salteada | "Está bien, pasemos a otra." |
| Normal | Cualquier otro caso | Una de tres frases, según el resto de dividir el `orden` de la pregunta por 3: resto 0, "Sigamos con otra pregunta."; resto 1, "Ahora me gustaría saber otra cosa."; resto 2, "Vamos con la siguiente." |
| Sin preguntas pendientes | No queda ninguna (CU-03, camino alterno Nº4) | No hay pregunta ni introducción: se devuelve el mensaje final "Ya respondiste todas las preguntas que tengo por ahora. ¡Gracias por compartir tu historia conmigo!" |

El front muestra la introducción y la pregunta como un solo mensaje de Alma, en dos renglones. Los textos van como constantes en `MensajesHistoria`.

### 5.3 Saltear una pregunta

Igual que 5.1 pero se registra `SALTEO`, no se valida texto, no se encola nada y no se toca el grafo de recuerdos. Responde `204`. Si ya estaba respondida o salteada, `409`.

### 5.4 Extracción (en segundo plano)

El trabajo de extracción lleva: el id de la persona, el id de la pregunta, el texto de la respuesta y, si es repregunta, el recuerdo y el dato que completa. Además lleva, leídos antes de encolarlo (así el hilo de la cola no toca PostgreSQL): el nombre, el apellido y la fecha de nacimiento de la persona, el texto de la pregunta (en una repregunta, el texto fijo de la repregunta) y su franja de edad. Se atiende de a uno, en orden de llegada, y respeta la prioridad del chat (sección 2).

1. **Pedido:** el prompt lleva el texto de la pregunta (para interpretar respuestas breves), la respuesta y el esquema de 5.5. Razonamiento desactivado, temperatura 0,1, tope de 600 tokens y 25 s por llamada.
2. **Limpieza:** se extrae solo el bloque JSON (sin texto antes o después ni delimitadores ```` ```json ````).
3. **Conversión:** se pasa a la clase Java del esquema normalizando tipos (`"1957"` pasa a número) y nulos (`"null"` pasa a nulo); un campo que falta se completa con nulo o lista vacía.
4. **Validación de estructura:** `titulo` y `descripcion` no vacíos; `anio` entre 1900 y el año actual; `edadMencionada` entre 0 y 110; `personas` y `lugares` son listas.
5. **Validación de contenido** ("no inventar datos"): `anio` y `edadMencionada` solo se aceptan si ese número **aparece escrito en cifras** en el texto de la respuesta (los números escritos con letras no se detectan: límite conocido; una década escrita como "los años 50" tampoco se convierte en año, porque "1950" no aparece en el texto: ver sección 12); si no, se descartan y el recuerdo se crea sin ese dato. Se descarta toda persona cuyo nombre normalizado coincida con el de la propia persona mayor (con su nombre solo o con nombre y apellido), y las personas con nombre repetido dentro de la misma respuesta cuentan una sola vez.
6. **Reintento:** si falla un paso entre 2 y 4, se repite el pedido **una sola vez**, indicando el error al modelo. El paso 5 no provoca reintento: descarta.
7. **Respaldo:** si el segundo intento falla, o el modelo no está disponible, o se acaba el tiempo, se crea igual un `Recuerdo` con título igual al texto de la pregunta, descripción igual a la respuesta textual (sin espacios en los extremos), sin personas ni lugares y con año estimado (5.6) si la pregunta tiene franja de edad. La pregunta ya estaba registrada como respondida. (En una repregunta no hay respaldo: el recuerdo ya existe y simplemente no cambia.)

En todos los casos, el trabajo termina sin lanzar errores hacia afuera: un fallo del modelo se registra en el log (sin el texto) y no afecta a nada más.

### 5.5 Esquema del JSON de la extracción

Los campos sin datos van como `null` o lista vacía. El modelo no inventa datos: `anio` solo si la persona lo dijo, `edadMencionada` solo si dijo una edad, `nombre` es `null` si solo se menciona el vínculo ("mi tío"), la propia persona no se incluye, y **las mascotas no son personas** (el modelo de grafo no tiene un nodo para ellas): se mencionan en la descripción del recuerdo y no van en `personas`.

```json
{
  "titulo": "Mi maestra Elena",
  "descripcion": "Su maestra de primer grado, que le enseñó a leer.",
  "anio": 1957,
  "anioAproximado": false,
  "edadMencionada": null,
  "personas": [{ "nombre": "Elena", "vinculo": "maestra" }],
  "lugares": ["Escuela N° 12"]
}
```

### 5.6 Qué se escribe en el grafo

Todo en **una sola transacción** de Neo4j, y siempre anclado en el `PersonaMayor`.

| Dato | Qué se hace |
|---|---|
| Recuerdo | Se crea con `titulo`, `descripcion`, `fechaRegistro` (ahora), `anio`, `precision` y `etapaDeVida` según la fila siguiente, con los tres contadores de intentos en 0. Se enlaza con `TIENE_RECUERDO` y con `SURGIO_DE` hacia la pregunta de origen (regla 2) |
| Año y precisión | `anio` con `anioAproximado = false` da `EXACTA`; con `true`, `APROXIMADA`. Si no hay `anio` pero sí `edadMencionada`: año = año de nacimiento + edad, `CALCULADA_POR_EDAD`. Si no hay ninguno de los dos y la pregunta tiene franja de edad: año = año de nacimiento + (`edadDesde` + `edadHasta`) / 2 (división entera), `ESTIMADA`. Si nada de eso, el recuerdo queda **sin año ni precisión** (regla 6). Un año anterior al nacimiento se descarta y se sigue con lo que viene: primero la edad mencionada y después la franja de la pregunta (v11). Un año calculado que quedaría en el futuro también se descarta y se sigue igual |
| Etapa de vida | Edad en el año del recuerdo = `anio` menos el año de nacimiento. `INFANCIA` 0 a 11, `ADOLESCENCIA` 12 a 17, `JUVENTUD` 18 a 29, `ADULTEZ` 30 a 59, `VEJEZ` 60 o más. Sin año no hay etapa |
| Personas | `nombreNormalizado`: minúsculas, sin tildes ni diéresis (la `ñ` pasa a `n`) y espacios colapsados. **Con nombre:** si ya existe una persona con ese `nombreNormalizado` (P6), se reutiliza y **no se cambia su vínculo**; si no, se crea con `CONOCE {vinculo}`. **Sin nombre:** si hay **exactamente una** persona con ese vínculo (P7), se reutiliza; si no hay o hay más de una, se crea una nueva sin nombre. Después se enlaza con `PARTICIPA_EN` hacia el recuerdo |
| Lugares | Con el `nombreNormalizado` (P8): si ya existe, se reutiliza; si no, se crea. Se enlaza con `OCURRIO_EN`. Un recuerdo puede tener más de un lugar |
| Duplicados | Nunca dos `Persona` ni dos `Lugar` con el mismo `nombreNormalizado` en el grafo de una persona (regla 5), aunque lleguen dos extracciones seguidas |

### 5.7 Repreguntas

Completan un recuerdo ya creado **sin crear nodos `PreguntaGuion` ni tocar el progreso** (regla 7).

**Cuándo un dato es candidato a repreguntar.** Para un recuerdo, el dato está en la lista `repreguntas` de su pregunta de origen **y**:

| Dato | Falta cuando |
|---|---|
| `ANIO` | el recuerdo no tiene año, o tiene un año con precisión `ESTIMADA` (una estimación por franja no cuenta como año real) |
| `LUGARES` | no tiene ningún `OCURRIO_EN` |
| `PERSONAS` | ninguna persona `PARTICIPA_EN` el recuerdo |

**y** el contador de intentos de ese dato es menor que 2 (`intentosAnio`, `intentosLugares` o `intentosPersonas`).

El orden de preguntas dentro de un recuerdo es el de la lista `repreguntas` de la pregunta.

**"Una vez por charla" lo resuelve el front.** El servidor no guarda sesiones. Quien muestra las repreguntas (el front, en la segunda rama) lleva en memoria qué datos ya preguntó **en esa charla** y se los manda al back en `excluir` (7.5), para no volver a preguntar lo mismo en el mismo rato. La memoria del front se pierde al recargar o al volver a ingresar: esa es la "otra charla". Así, un dato sin respuesta se vuelve a ofrecer en la charla siguiente, y como máximo dos veces en total.

**Textos fijos** (constantes en `MensajesHistoria`):

| Dato | Texto |
|---|---|
| `ANIO` | "¿Te acordás en qué año fue, o cuántos años tenías?" |
| `LUGARES` | "¿Dónde pasó esto?" |
| `PERSONAS` | "¿Quién más estaba con vos en ese momento?" |

**Al responder o saltear:** se suma 1 al contador de ese dato en una sola consulta que solo lo hace si vale menos de 2 (si no, `409`).

**Al responder además:** se encola una extracción de la respuesta con el mismo esquema (5.5) y el back **aplica solo lo que corresponde al dato preguntado**:

- `ANIO`: si la respuesta aporta un año o una edad (misma regla de 5.6), se fija el año, la precisión y la etapa de vida, **reemplazando** un año `ESTIMADA` o la ausencia de año. No pisa un año `EXACTA`, `APROXIMADA` o `CALCULADA_POR_EDAD` que ya existiera.
- `LUGARES`: se suman los lugares nuevos (con la regla de duplicados de 5.6).
- `PERSONAS`: se suman las personas nuevas (con la regla de duplicados de 5.6).

Nunca cambia `titulo` ni `descripcion`, ni agrega datos que no son del dato preguntado. Si la respuesta no aporta nada ("no me acuerdo"), o la extracción falla, el recuerdo no cambia y el intento ya quedó contado.

**Qué "pregunta" ve el extractor (v13).** El texto fijo de la repregunta (por ejemplo, "¿Dónde pasó esto?"), **sin** el título del recuerdo ni el texto de la pregunta original del guion. Se probaron dos variantes con el modelo real y los tres datos ("Fue en 1962", "En Allen", "Con mi hermana Marta", más "No me acuerdo"): con el texto fijo solo, 4 de 4 correctos y sin datos inventados; con el título como contexto, el modelo copiaba al resultado lugares y personas del título que la persona no dijo (por ejemplo, Elena y la escuela, o Cipolletti) y en "No me acuerdo" no quedaba vacío, y además se mandaba el título de la persona al modelo sin necesidad (los datos copiados no se aplicarían, porque solo cuenta el dato preguntado). Resultados crudos en `alma-anexo-c/corrida-repreguntas-variantes-2026-11-09.md`, fuera del repositorio.

**La charla libre no es una repregunta.** Lo que la persona cuente de más, sin que sea la respuesta a una repregunta, va al chat normal y **no se guarda** en el grafo (mejora futura).

### 5.8 Validaciones

| Campo | Regla | Mensaje |
|---|---|---|
| `texto` (responder, repregunta) | Obligatorio y con texto (sin contar espacios) | "Escribí una respuesta o elegí saltear la pregunta." (el de CU-03) |
| `texto` | Hasta 1000 caracteres | "Tu respuesta es muy larga. Probá con una más corta." |
| `{preguntaId}` | Debe existir | `404`, texto general |
| `{dato}` y valores de `excluir` | Uno de `ANIO`, `LUGARES`, `PERSONAS` | `400`, "Revisá los datos ingresados." |

El límite de 1000 caracteres es una propiedad de `historia.properties`.

### 5.9 Privacidad y registro

Lo que la persona cuenta de su vida es sensible. Igual que en el chat:

- **No se escribe en ningún log el texto de las respuestas, ni del prompt, ni del JSON del modelo, ni los nombres de personas y lugares.** Sí se registran la duración, los tokens y el tipo de error.
- No se clasifica el contenido por tema ni se generan alertas a partir de él. **Única excepción (v15):** el texto de una respuesta del guion o de una repregunta (7.3 y 7.6) pasa por el detector de riesgo del chat (`DetectorDeRiesgo`: urgencia médica o malestar emocional grave) antes de guardarse o encolarse. Si salta, se responde el texto predefinido del chat y no se guarda ni se encola nada. La comparación se hace en memoria y el texto sigue sin escribirse en ningún log. Detalle en `docs/contrato-historia-de-vida.md` (2.9 y 5.6).
- Con el modelo en la nube, lo que se cuenta sale del equipo. Para la PPS se usan datos sintéticos. Con usuarios reales corresponde el perfil local del modelo.

### 5.10 Baja de la persona

`eliminarHistoria(personaId)` borra el nodo `PersonaMayor` y todos sus `Recuerdo`, `Persona` y `Lugar` (regla 8). **No** modifica los `PreguntaGuion` ni los datos de otras personas. Si la persona no tiene nodo, termina sin error.

---

## 6. Textos

| Dónde | Texto |
|---|---|
| Respuesta vacía | "Escribí una respuesta o elegí saltear la pregunta." |
| Respuesta muy larga | "Tu respuesta es muy larga. Probá con una más corta." |
| Pregunta ya respondida o salteada (`409`) | "Esa pregunta ya fue respondida." |
| Repregunta que ya no corresponde (`409`) | "Esa pregunta ya no hace falta." |
| Introducciones y mensaje final | Ver 5.2 |
| Repreguntas | Ver 5.7 |

**Texto acordado para la segunda rama** (lo muestra el front o el chat, no este módulo): al terminar las repreguntas de un recuerdo, Alma pregunta *"¿Querés contarme algo más sobre esto o te hago otra pregunta?"*. Si la persona elige otra pregunta, se sigue con `GET /guion/proxima`. Si elige contar algo más, charla con Alma como en el chat normal, y eso no se guarda en el grafo (5.7).

Los textos del módulo van como constantes en `historia/MensajesHistoria`. Los generales (`400`, `401`, `403`, `404`) son los de `common/MensajesError`. El texto de las preguntas del guion va en `guion.json`.

---

## 7. Endpoints

Todos exigen sesión de `PERSONA_MAYOR`. `@Tag(name = "Historia de vida")` y `@Operation` en cada uno.

### 7.1 `GET /api/historia/guion/proxima`

Próxima pregunta pendiente (P1) con su introducción (5.2).

**200 OK**
```json
{
  "pregunta": { "id": 7, "orden": 7, "texto": "¿Qué te acordás de tu escuela primaria?" },
  "introduccion": "Sigamos con otra pregunta.",
  "mensajeFinal": null,
  "quedanPendientes": true
}
```
Si no quedan pendientes: `{ "pregunta": null, "introduccion": null, "mensajeFinal": "Ya respondiste todas las preguntas que tengo por ahora. ¡Gracias por compartir tu historia conmigo!", "quedanPendientes": false }`.

### 7.2 `GET /api/historia/guion/estado`

Todas las preguntas con su estado (P4) y la última respondida o salteada (P3).

**200 OK**
```json
{
  "respondidas": 4,
  "salteadas": 1,
  "pendientes": 17,
  "preguntas": [
    { "id": 1, "orden": 1, "texto": "…", "estado": "RESPONDIDA" },
    { "id": 8, "orden": 8, "texto": "…", "estado": "SALTEADA" },
    { "id": 3, "orden": 3, "texto": "…", "estado": "PENDIENTE" }
  ],
  "ultima": { "preguntaId": 8, "estado": "SALTEADA", "fecha": "2026-11-09T11:10:00-03:00" }
}
```
`ultima` es `null` si no hay ninguna.

### 7.3 `POST /api/historia/guion/{preguntaId}/respuesta`

Registra la respuesta y encola la extracción (5.1).

**Request:** `{ "texto": "Nací en Cipolletti, en una casa chica con patio…" }`

**202 Accepted** (v15), con cuerpo `{ "estado": "ACEPTADA" }`: la respuesta quedó registrada y la extracción, encolada.

**200 OK** (v15): el texto activó el detector de riesgo del chat (5.9). No se guardó ni se encoló nada y la pregunta sigue pendiente. El cuerpo es `{ "estado": "ALARMA", "respuesta": "<texto predefinido del chat>" }`. Ver `docs/contrato-historia-de-vida.md` (2.9, 5.6 y 7.1).

**Errores:** `400` (5.8), `401`, `403`, `404` (persona o pregunta), `409` (ya respondida o salteada). Con riesgo detectado no se llega a validar la pregunta: el `404` de pregunta inexistente y el `409` no se dan, se responde el `200` con `ALARMA`.

### 7.4 `POST /api/historia/guion/{preguntaId}/salteo`

Registra el salteo (5.3). **204 No Content.** Errores: `401`, `403`, `404`, `409`.

### 7.5 `GET /api/historia/repreguntas/siguiente`

Dice si hay una repregunta candidata (5.7).

**Parámetros opcionales**
- `preguntaId`: busca la repregunta del recuerdo que surgió de esa pregunta del guion (el que se acaba de crear). Sin este parámetro, busca en **todos** los recuerdos de la persona y devuelve el **más antiguo** (por `fechaRegistro`) que tenga una repregunta candidata, con su primer dato candidato.
- `excluir`: datos separados por coma (por ejemplo `ANIO,LUGARES`) que no se deben devolver, porque ya se preguntaron en esta charla (5.7).

**200 OK**
```json
{
  "estado": "HAY",
  "repregunta": { "recuerdoId": "5f1c…", "dato": "ANIO", "texto": "¿Te acordás en qué año fue, o cuántos años tenías?" }
}
```
- `estado: "PROCESANDO"`: hay una extracción en cola o en curso de esta persona (de cualquier pregunta o repregunta: es por persona, no por recuerdo); el cliente vuelve a preguntar en unos segundos. Se mira **antes** de leer el grafo. `repregunta` es `null`.
- `estado: "NINGUNA"`: no hay repregunta candidata. `repregunta` es `null`. También es lo que devuelve un `preguntaId` que no existe o del que la persona no tiene recuerdos (no es un `404`: el `404` es solo por la persona).

**Errores:** `400` (valor inválido en `excluir`), `401`, `403`, `404` (persona).

### 7.6 `POST /api/historia/repreguntas/{recuerdoId}/{dato}/respuesta`

Responde una repregunta (5.7). **Request:** `{ "texto": "Fue en 1962" }`. **202 Accepted** (v15), con cuerpo `{ "estado": "ACEPTADA" }`. Si el texto activa el detector de riesgo del chat (5.9), devuelve **200 OK** con `{ "estado": "ALARMA", "respuesta": "<texto predefinido del chat>" }`: no se cuenta el intento, no se completa el dato y no se encola nada. Ver `docs/contrato-historia-de-vida.md` (2.9, 5.6 y 7.2).

**Errores:** `400`, `401`, `403`, `404` (el recuerdo no existe o no es de la persona), `409` (el dato no aplica a ese recuerdo, ya no falta o ya se preguntó 2 veces). Con riesgo detectado no se valida el recuerdo ni el dato: el `404` y el `409` no se dan, se responde el `200` con `ALARMA`.

### 7.7 `POST /api/historia/repreguntas/{recuerdoId}/{dato}/salteo`

Saltea una repregunta (suma un intento). **204 No Content.** Mismos errores que 7.6, sin `400` por texto.

**Formato de los errores:** Problem Details, igual que el resto de `/api` (`docs/contrato-login.md`, sección 7).

---

## 8. Reglas de acceso

| Ruta | Quién puede |
|---|---|
| `/api/historia/**` | Solo `PERSONA_MAYOR`. Un `ADMINISTRADOR` recibe `403` |

Sin token o con token vencido: `401`. Una persona nunca puede leer ni modificar recuerdos de otra: todo el acceso al grafo parte del nodo de la persona del token (sección 2).

---

## 9. Errores y mensajes

Formato Problem Details y textos generales: los de `common/MensajesError` (`docs/contrato-login.md`, sección 7). Los del módulo van en `historia/MensajesHistoria` (sección 6).

---

## 10. Back: qué implementar

Paquete base: `ar.edu.utn.frm.gelab.alma`. Un paquete nuevo, `historia/`, y cambios en `ia/`.

**`historia/`**
- `controller/`: `HistoriaController` (7.1 a 7.7).
- `dto/`: las respuestas y pedidos de la sección 7.
- `domain/`: las `@Node` (`PersonaMayorNodo`, `PreguntaGuion`, `Recuerdo`, `Persona`, `Lugar`) y los enums (`PrecisionAnio`, `EtapaDeVida`, `EstadoPregunta`, `DatoFaltante`, `EstadoRepregunta`), entre otras. Las clases de nodo llevan sufijo si chocan con las de JPA (`PersonaMayor` ya existe en `persona/domain`).
- `repository/`: `Neo4jRepository` para leer nodos como entidad, `LecturaDelGrafo` (las consultas de lectura de `consultas.cypher`, con `Neo4jClient`; devuelven `record`) y `EscrituraDelGrafo` (escrituras con Cypher explícito por `Neo4jClient`), entre otras.
- `service/`: `HistoriaDeVidaService` (guion, progreso, introducción, `eliminarHistoria`), `RecuerdoService` (escribe el recuerdo, 5.6) y `RepreguntaService` (5.7), entre otras.
- `extraccion/`: `ExtractorDeRecuerdos` (pedido, limpieza, conversión, validación, reintento y respaldo, 5.4), `RespuestaExtraida` (el esquema de 5.5) y `ColaDeExtracciones` (un hilo, en memoria, con el estado "procesando" por persona), entre otras.
- `config/`: `ConfiguracionNeo4j` (el manejador de transacciones propio, sección 2), `CargaInicialDelGrafo` (restricciones, guion que solo crea y datos de ejemplo) y `ConfiguracionHistoria` (`@ConfigurationProperties` con `@PropertySource` en UTF-8, igual que `ConfiguracionChat`).
- `MensajesHistoria`: sección 6.
- Recursos: `src/main/resources/historia/guion.json` (Anexo A, con `repreguntas` de cada pregunta), `historia/restricciones.cypher` (copia de `docs/Modelo_de_grafo/restricciones.cypher`: hay que mantenerlas sincronizadas: `RestriccionesCypherTest` falla si las dos copias dejan de ser iguales, salvo el comentario de la copia, y otra prueba comprueba que existen las seis restricciones), `historia.properties` (límite de 1000 caracteres, máximo de 2 intentos por dato, parámetros del modelo para extracción) y el prompt del extractor en `historia/prompt-extractor.txt`.

**`ia/`** (compartido con el chat: avisar antes de tocarlo)
- `ModeloConversacional` / `OllamaModeloConversacional`: poder pedir la temperatura y el tope de tokens **por llamada** (un método `default` con `OpcionesDeLlamada`) y usar, para la extracción, un segundo cliente de Ollama con su propio tope de 25 s (`alma.ia.timeout-extraccion-ms`): el tiempo máximo no es un parámetro de la llamada sino del cliente. El chat conserva sus valores.
- `LimitadorDePedidos`: dos niveles (sección 2). **El comportamiento actual del chat no cambia**, y sus pruebas existentes siguen pasando.

Los módulos se comunican solo por sus servicios: `historia/` usa `PersonaMayorRepository` (solo lectura de `id`, `nombre`, `apellido` y `fechaNacimiento`) y `ia/`. Solo `ia/` importa Spring AI.

**Cambios en archivos compartidos (avisar antes de tocarlos; sirven para la descripción del PR)**

| Archivo | Cambio |
|---|---|
| `config/SecurityConfig.java` | Una línea: `.requestMatchers("/api/historia/**").hasRole("PERSONA_MAYOR")`, **antes** de `.requestMatchers("/api/**").authenticated()` |
| `ia/` | Ver arriba |
| `common/exception/ConflictoException.java` (nueva) | Igual que `RecursoNoEncontradoException`: lleva el texto que ve la persona. Se lanza en los `409` de la sección 6 |
| `common/GlobalExceptionHandler.java` | Un `@ExceptionHandler(ConflictoException.class)` que responde `409` en Problem Details, con título en español y el texto de la excepción como detalle, y un `case 409` en `traducir` para los `409` que genere Spring |
| `common/MensajesError.java` | La constante del título del `409` (`TITULO_CONFLICTO`) |

**No se tocan:** `application.properties` (la conexión a Neo4j ya está), `AlmaApplication.java`, `DatosDePrueba`, el front, `chat/` y los demás módulos. Dos excepciones, ya documentadas: el commit de la precondición (la fecha de nacimiento obligatoria cambió `PersonaMayor`, `PerfilDto`, el perfil del front y las pruebas de otros módulos que crean personas) y `pom.xml`, que cambió en la v10 (el tamaño del pool de conexiones de las pruebas; los starters de Neo4j ya estaban).

**Checklist (en este orden).** Estado al 09/11/2026: ✅ hecho, ⏳ falta. Se actualiza al terminar cada paso, para que sirva de memoria entre sesiones.

0. ✅ **Precondición:** `docs/contrato-fecha-nacimiento.md` hecho en esta rama, en un commit propio.
1. ✅ **Prueba de transacciones:** primero, sobre el estado actual, qué manejador existe y que los `@Transactional` sin calificar usan el de JPA. Después, con `ConfiguracionNeo4j`: con JPA y Neo4j juntos, una escritura al grafo dentro del manejador propio se deshace si falla a la mitad, y las consultas de PostgreSQL siguen funcionando igual. Verificar que el arranque no tenga avisos de repositorios ambiguos y anotar qué cambia `@EnableNeo4jRepositories` en la configuración de Boot. Anotar el resultado en el historial. *Resultado en la v5. `TransaccionesNeo4jTest` queda como prueba permanente.*
2. ✅ **Prueba del modelo (Anexo C):** probar el prompt del extractor contra `gemma4:cloud` con las respuestas sintéticas. Si el modelo no sirve, se avisa antes de seguir. *Resultado en la v7: JSON válido en 13 de 13. Los prompts, el script y los resultados están fuera del repositorio, en `C:\Users\pauro\Documents\alma-anexo-c`. Repetida con el extractor terminado en el paso 4 (13 de 13, v11); la regla 2 del prompt ya dice que una década escrita no es un año (sección 12).*
3. ✅ **Paso 1 (grafo, guion y progreso):** nodos, restricciones, carga del guion y de los datos de ejemplo, nodo de la persona, `eliminarHistoria` (con su prueba: las pruebas de este paso la usan para limpiar), endpoints 7.1 a 7.4. Sin IA. Se puede mezclar a `main` apenas esté lista, para que la segunda rama empiece. *Hecho en dos commits (grafo, guion y carga; endpoints y pruebas), con la prueba de concurrencia y su control (v9). Lo único que falta de este paso es encolar la extracción al responder, que llega con el paso 4.*
4. ✅ **Paso 2 (extractor):** extractor, escritura del recuerdo y respaldo (5.4 a 5.6). Modelo de prueba en las pruebas. *Hecho (v11): `ExtractorDeRecuerdos`, `RespuestaExtraida`, `RecuerdoService`, `historia/prompt-extractor.txt`, `ModeloDePrueba` y una cola mínima (`ColaDeExtracciones`: un hilo, `encolar`, `esperarQueSeVacie`, `@PreDestroy`) para que responder encole la extracción. En `ia/`, un método nuevo con opciones por llamada (`default`, sin tocar las firmas) y un segundo cliente de Ollama con tope de 25 s. Anexo C con el extractor real: 13 de 13.*
5. ✅ **Paso 3 (cola):** cola y prioridad del chat. *Hecho (v12): `LimitadorDePedidos` con dos niveles (`ejecutar` para el chat y `ejecutarEnSegundoPlano` para la extracción, sin tope de espera), `OllamaModeloConversacional` elige el nivel según el método, y `ColaDeExtracciones` suma `estaProcesando(personaId)` y un apagado que interrumpe las esperas. El diseño de `ia/` se mostró y aprobó antes de escribir código.*
6. ✅ **Paso 4 (repreguntas):** repreguntas y endpoints 7.5 a 7.7. *Hecho (v13): `RepreguntaService` y `SelectorDeRepregunta`, los tres endpoints en `HistoriaController`, `EscrituraDelGrafo.registrarIntento` y `completarRecuerdo`, y `RecuerdoService.completar`. Nada de lo nuevo toca archivos compartidos.*
7. ✅ **Paso 5 (cierre):** documentación del modelo, `CLAUDE.md` (enlazar este contrato y corregir el nombre del paquete, `historia` y no `historiavida`, y la etapa en que entra Neo4j) y `casos-de-uso.md`. *Hecho (v14): los tres documentos, la prueba `RestriccionesCypherTest` y la revisión del contrato contra el código (secciones 5, 7 y 10), con las correcciones de texto de la v14. Lo único que queda abierto es la prueba manual del reinicio con una extracción en cola (sección 11).*

---

## 11. Pruebas y criterio de "listo"

"**Modelo de prueba**" quiere decir una clase que se usa solo en los tests y reemplaza la conexión real con Ollama (implementa `ModeloConversacional` y devuelve respuestas programadas: un JSON válido, uno roto, un error, una demora). Así los tests son rápidos, siempre dan lo mismo y no necesitan Ollama ni internet. `ChatIntegrationTests` ya hace lo mismo con su clase `ModeloFalso`.

**Automáticas** (mismo patrón de `ChatIntegrationTests` y `SesionDePrueba`, contra el Neo4j de `docker compose up -d`; cuentas con prefijo `test_historia_`; cada prueba **borra al terminar** el subgrafo de sus personas con `eliminarHistoria`)

*Grafo y guion:*
- Al arrancar, hay 21 preguntas, cada una con su lista `repreguntas`; arrancar dos veces no duplica nada.
- **La carga no pisa cambios:** si se modifica el texto de una pregunta en la base y se vuelve a ejecutar la carga, el texto queda como estaba; una pregunta que no existe sí se crea.
- `GET /guion/proxima` devuelve la de menor `orden` sin `RESPONDIO` ni `SALTEO`; con todas respondidas o salteadas devuelve `pregunta: null` y el mensaje final.
- Introducción: "Primera vez" cuando no hay nada registrado; "Después de un salteo" cuando la última fue un salteo; "Normal" en el resto, con las tres frases según el `orden`.
- Responder y saltear cambian el estado y la próxima; el estado cuenta bien respondidas, salteadas y pendientes; `ultima` es la más reciente.
- Responder dos veces la misma pregunta (también dos pedidos a la vez) da `409` y deja una sola relación. Saltear una respondida da `409`.
- **Aislamiento:** dos personas con una persona del mismo nombre y un lugar del mismo nombre: cada una ve solo lo suyo y el progreso de una no cambia el de la otra.
- Sin token `401`; con token de `admin` `403`; texto vacío o de más de 1000 caracteres `400` con el mensaje del contrato; pregunta inexistente `404`.

*Extractor (con el modelo de prueba):*
- Respuesta válida: se crea un recuerdo con `SURGIO_DE`, `TIENE_RECUERDO`, personas con `CONOCE` y `PARTICIPA_EN`, y lugares con `OCURRIO_EN`, con los tres contadores en 0.
- Año: con año exacto, aproximado, por edad, estimado por franja y sin franja (sin año). Un año anterior al nacimiento se descarta.
- Etapa de vida en los límites (11/12, 17/18, 29/30, 59/60) y sin año.
- Un `anio` o una `edadMencionada` que no aparecen en el texto se descartan. La propia persona mayor no entra como `Persona`.
- JSON con texto antes y después, con delimitadores de código, con `"anio": "1957"`, con `"null"` y sin el campo `lugares`: se corrige y se guarda.
- JSON inválido dos veces, error del modelo o demora excesiva: se crea el recuerdo de respaldo, la pregunta queda respondida y no hay error hacia afuera.
- Sin duplicados: dos respuestas que mencionan a "Marta" y a "Cipolletti" dejan una sola `Persona` y un solo `Lugar`; "MARTA" y "Marta" cuentan como la misma; una persona sin nombre con un vínculo único se reutiliza y con dos del mismo vínculo se crea otra.
- Ningún log contiene el texto de las respuestas (revisar).

*Cola y prioridad:*
- Con varias extracciones encoladas se atienden de a una y en orden.
- Con un chat esperando, la extracción no toma el turno; sin chat, sí.
- Las pruebas del chat que ya existen siguen pasando.

*Repreguntas:*
- `siguiente` devuelve `PROCESANDO` mientras la extracción está en curso, `HAY` con el dato y el texto fijo correctos, y `NINGUNA` cuando no hay candidata.
- Solo se ofrecen datos de la lista `repreguntas` de la pregunta de origen (por ejemplo, para "¿Dónde naciste?", que tiene la lista vacía, nunca hay repregunta).
- `ANIO` es candidato con un recuerdo sin año y con uno `ESTIMADA`, y no con uno `EXACTA`, `APROXIMADA` o `CALCULADA_POR_EDAD`. `LUGARES` y `PERSONAS` solo si el recuerdo no tiene ninguno.
- `excluir` saca esos datos del resultado. Sin `preguntaId`, devuelve el recuerdo más antiguo con una candidata.
- Responder o saltear suma 1 al contador del dato; con el contador en 2, `siguiente` ya no lo ofrece y 7.6 y 7.7 dan `409`. Saltear y "no me acuerdo" cuentan igual.
- Responder `ANIO` con un año o una edad reemplaza una estimación y fija precisión y etapa; no pisa un año ya exacto. Responder `LUGARES` o `PERSONAS` suma sin duplicar. Nunca cambia `titulo` ni `descripcion`.
- No se crean `PreguntaGuion` ni cambia el progreso.
- Un recuerdo de otra persona da `404`.

*Baja:*
- `eliminarHistoria` borra el nodo de la persona y sus recuerdos, personas y lugares, no toca las preguntas ni lo de otra persona, y repetirlo no da error.

**Manual con el modelo real**
- Anexo C, antes de construir y otra vez con el extractor terminado.
- Con `docker compose up -d`, `ollama signin` hecho y el back corriendo: desde Swagger, responder 3 preguntas y saltear 1; ver en el **Neo4j Browser** (`http://localhost:7474`) el grafo dibujado, con recuerdos, personas y lugares; probar repreguntas (responder, saltear, "no me acuerdo" y ver los contadores); probar un reinicio del back con una extracción en cola.

**Listo cuando:** con `docker compose up -d` y el back corriendo, `ana` tiene su historia de ejemplo; desde Swagger se responde y se saltea el guion, el progreso se retoma donde se dejó y cada respuesta se convierte, en segundo plano, en un recuerdo con sus personas y lugares visible en el Neo4j Browser; las repreguntas completan lo que falta y respetan el máximo de 2 intentos por dato; ante un fallo del modelo la respuesta igual queda guardada; el chat nunca queda esperando detrás de una extracción en cola; y las pruebas automáticas, incluidas las anteriores, pasan.

---

## 12. Pendientes para decidir y límites conocidos

**Pendientes**
1. **Frases de la sección 5.2 y 5.7:** son una propuesta. Se pueden cambiar sin tocar nada más.
2. **Qué repreguntas aplican a cada pregunta** (columna del Anexo A): propuesta a revisar.

**Límites conocidos (ya decididos)**
- La cola de extracciones está en memoria: un reinicio pierde las que esperaban.
- Una extracción en curso no se corta: en un caso raro el chat recibe el error de siempre.
- Los años escritos con letras ("quince años") no se detectan.
- Una década escrita como "los años 50" no se convierte en año: el modelo la interpreta como 1950 aproximado, pero la validación de contenido (5.4, paso 5) lo descarta porque "1950" no aparece en cifras en la respuesta. El recuerdo se crea sin año de la respuesta y queda estimado por la franja de edad de la pregunta (`ESTIMADA`); la descripción puede conservar "años 50". Mejora posible a futuro: con `anioAproximado` verdadero, aceptar el año si aparece la década en cifras.
- Los apodos y los pronombres no se reconocen como personas.
- Los recuerdos de las preguntas sin dato de año (por ejemplo, domingo en familia o tradición) pueden quedar "sin fecha" en la línea de tiempo, a propósito: el año no es relevante.
- Lo que la persona cuenta de más en la charla libre no se guarda en el grafo.
- **Un operador que edite el guion (futuro).** El diseño actual lo permite sin cambios grandes: el progreso se deduce de las relaciones, los ids de pregunta son fijos y la carga no pisa lo que ya existe. Lo que faltaría: endpoints y pantallas del panel (como los de ejercicios), **baja lógica** de preguntas (una propiedad `fechaBaja` en `PreguntaGuion`, donde "sin valor" significa activa, y un filtro en las consultas de progreso, porque borrar una pregunta rompería el `SURGIO_DE` de sus recuerdos y el historial de quienes la respondieron), un `orden` que se pueda renumerar sin romper la restricción de unicidad (por ejemplo, ordenar de 10 en 10 al crear), y actualizar el modelo de grafo, que hoy dice que el guion lo mantiene el equipo de desarrollo, y los casos de uso. Cambiar el texto de una pregunta ya respondida no modifica los recuerdos que ya surgieron de ella.
- **Neo4j caído al arrancar.** La carga inicial (restricciones, guion y datos de ejemplo) registra un ERROR en el log y deja arrancar el resto de la aplicación, para que el chat y los demás módulos no dependan de Neo4j. Si Neo4j no está, las restricciones y el guion no se cargan hasta el próximo arranque, y mientras tanto los endpoints de `/api/historia` fallan.
- **Mascotas (futuro).** No hay una pregunta del guion sobre mascotas porque el modelo no tiene un nodo para ellas. Para sumarlas hace falta un nodo nuevo (por ejemplo `Mascota`), con su relación con el recuerdo y su restricción de unicidad, actualizar el modelo y el extractor, y agregar la pregunta al final del guion con el id 22 y orden 22. Mientras tanto, si la persona menciona una mascota en otra respuesta, queda nombrada en la descripción del recuerdo y no se crea como persona.
- **Segundo escaneo de repositorios de Neo4j (mejora opcional).** Spring Boot escanea los repositorios de Neo4j dos veces: el imperativo (el de `ConfiguracionNeo4j`, que solo mira `historia.repository`) y el reactivo, que se activa porque `reactor` está en el classpath (viene de Spring AI) y recorre todo el proyecto sin encontrar nada. Eso deja unos pocos avisos INFO "Could not safely identify store assignment" en el arranque, que no asignan nada mal. Agregar `spring.data.neo4j.repositories.type=imperative` en `application.properties` elimina el segundo escaneo y deja solo el aviso del escaneo de JPA viendo el repositorio de Neo4j, según el experimento de la v5. No se aplica por ahora: no hace falta para nada de lo que sigue y `application.properties` es un archivo compartido.

---

## 13. Documentos a actualizar

- ✅ `docs/casos-de-uso.md`, CU-03: estado de implementación del Back; reflejar las repreguntas (regla de integridad 7) y la introducción de cada pregunta. La descripción general no se modifica.
- ✅ `docs/Modelo_de_grafo/modelo-de-grafo.md`: tipos de id (sección 2), las propiedades nuevas `PreguntaGuion.repreguntas` y `Recuerdo.intentosAnio/intentosLugares/intentosPersonas` (4.1) y una nota de que el guion se carga solo si no existe. Además: (a) el ejemplo de `APROXIMADA` ("por los años 70") ya no lo produce el extractor, porque una década escrita no se convierte en año (sección 12): cambiarlo por uno con el año en cifras, por ejemplo "por 1970"; (b) anotar que `src/main/resources/historia/restricciones.cypher` es una copia de `restricciones.cypher` de esa carpeta y que hay que mantenerlas sincronizadas.
- ✅ `CLAUDE.md`: enlazar este contrato en "Documentos".

Los tres se hicieron en el paso 7 (v14).

---

## 14. Historial de cambios

- **v1 (07/11/2026):** primera propuesta, contra el commit `d35f1a9`.
- **v2 (08/11/2026):** contrato autocontenido (sin referencias a documentos que no están en el repo); guion de 22 preguntas con repreguntas aplicables por pregunta y carga que no pisa cambios; introducción fija de cada pregunta; fecha de nacimiento obligatoria como precondición; repreguntas sin tope por respuesta, máximo 2 intentos por dato con tres contadores numéricos (se descarta la lista `datosPreguntados`) y "una por charla" resuelta en el front con `excluir`; `siguiente` con `preguntaId` para retomar recuerdos viejos; las mascotas no se guardan como personas; "modelo falso" pasa a llamarse "modelo de prueba" y se explica; nota de cómo se haría la edición del guion por un operador.
- **v3 (08/11/2026):** se saca la pregunta de la mascota porque el modelo no tiene un nodo para ellas; el guion queda en 21 preguntas (la de domingo pasa a ser la 20 y la de tradición la 21), y las mascotas pasan a "No incluye" y a "Límites conocidos" como mejora futura; en el Anexo C, E9 pasa a la pregunta 21 y E11 a la pregunta 5, con una respuesta sobre un perro en el juego.
- **v4 (08/11/2026, contra el commit `4afe5d9`):** transacciones: `ConfiguracionNeo4j` declara los dos manejadores explícitos (`transactionManager` de JPA con `@Primary` y `neo4jTransactionManager`) y `@EnableNeo4jRepositories` fija el manejador y el paquete de los repositorios de Neo4j, porque con un solo manejador agregado el de JPA dejaría de crearse; la prueba de transacciones empieza por comprobar el estado actual; responder y saltear bloquean el nodo `PersonaMayor` para evitar que una respuesta y un salteo simultáneos queden los dos registrados; la carga de ejemplo usa `ApplicationReadyEvent`; se agrega `ConflictoException` y un `case 409` en `GlobalExceptionHandler` (que pasa a "se toca"); `eliminarHistoria` se implementa en el primer paso del grafo, porque las pruebas lo usan para limpiar; el cierre corrige en `CLAUDE.md` el nombre del paquete y la etapa de Neo4j.
- **v5 (08/11/2026, contra el commit `4afe5d9` más los cambios del paso 1):** resultado de la prueba de transacciones (checklist, punto 1), con Spring Boot 4.1.1, Spring Data Neo4j 8.1.1, driver 6.1.0 y Neo4j 5.26.29. **Antes de la configuración:** existía un solo manejador, `transactionManager` (`JpaTransactionManager`); no había ninguno de Neo4j; los 14 servicios con `@Transactional` sin calificar usaban el de JPA; una escritura de JPA que falla a la mitad se deshacía. **Con `ConfiguracionNeo4j`** (`TransaccionesNeo4jTest`, prueba permanente, 12 casos): hay exactamente dos manejadores, el de JPA con `@Primary` y `neo4jTransactionManager`; los mismos 14 servicios siguen usando el de JPA; una escritura al grafo con `@Transactional("neo4jTransactionManager")` que falla a la mitad se deshace (0 nodos), y sin fallar se confirma (2); sin ninguna transacción el primer nodo queda confirmado (control: la prueba detecta el problema); con una escritura en las dos bases que falla, Neo4j se deshace y PostgreSQL queda (no hay transacción común); un repositorio de Neo4j usa `neo4jTransactionManager` y se une a su transacción. **Hallazgo:** Spring Data Neo4j se engancha (`Neo4jSessionSynchronization`) a la transacción de otro manejador que esté activa, de modo que un `@Transactional` de JPA que falla también deshace las escrituras hechas con `Neo4jClient`; no es una transacción común (el commit de Neo4j ocurre después del de JPA), y el motivo de tener el manejador propio sigue siendo el de la sección 2 (que JPA siga creándose y que los repositorios de Neo4j usen el suyo). **Qué cambia `@EnableNeo4jRepositories`:** el escaneo de repositorios de Neo4j deja de abarcar todo el proyecto (antes registraba un aviso por cada repositorio de JPA) y busca solo en `historia.repository`; los 11 repositorios de JPA siguen igual. **Avisos de repositorios ambiguos:** no desaparecen del todo, son de nivel INFO ("Could not safely identify store assignment") y no asignan nada mal: uno por cada repositorio de Neo4j que ve el escaneo de JPA, y los del segundo escaneo de Neo4j, el reactivo, que Spring Boot activa porque `reactor` está en el classpath (viene de Spring AI) y que sigue recorriendo todo el proyecto y no encuentra nada. Con `spring.data.neo4j.repositories.type=imperative` (experimento, no aplicado) queda solo el primero; hacerlo permanente toca `application.properties`. `./mvnw test` completo: 301 pruebas, 0 fallas (las 289 anteriores y las 12 nuevas).
- **v6 (09/11/2026):** en "Límites conocidos" (sección 12) se anota, como mejora opcional, agregar `spring.data.neo4j.repositories.type=imperative` en `application.properties`: elimina el segundo escaneo (reactivo) de repositorios de Neo4j y baja los avisos INFO del arranque de 12 a 1. No se aplica por ahora: no hace falta para el resto del trabajo y `application.properties` es un archivo compartido.
- **v7 (09/11/2026):** resultado de la prueba del modelo (Anexo C, 2 corridas con `gemma4:cloud`: JSON válido en 13 de 13). E3 ("Por los años 50"): se mantiene la regla 5.4.5 tal cual, de modo que una década escrita como "los años 50" no se convierte en año y el recuerdo queda estimado por la franja de la pregunta; el Anexo C pasa a pedir "sin `anio`" para E3, y se anota en la sección 5.4 (paso 5) y en "Límites conocidos" (sección 12), con la mejora posible a futuro.
- **v8 (09/11/2026):** antes del paso 3 del checklist: la carga de los datos de ejemplo de `ana` es solo si no tiene recuerdos **ni progreso** (sección 2 y Anexo B); en 5.1 el cuerpo se valida antes del `MERGE` del nodo de la persona (y se corrige la referencia de la extracción, que es 5.4 y no 5.3); la sección 10 lista el recurso `historia/restricciones.cypher` y la sección 13 anota que es una copia a mantener sincronizada con la del modelo, y que el ejemplo "por los años 70" de `APROXIMADA` ya no lo produce el extractor; "Límites conocidos" suma que, con Neo4j caído al arrancar, la carga inicial registra un ERROR y el resto de la aplicación sigue, sin cargar las restricciones ni el guion hasta el próximo arranque.

- **v9 (09/11/2026):** paso 3 del checklist, commit 2 (endpoints de guion y progreso). **Lecturas:** se descubre que un `@Query` de repositorio con columnas sueltas no sirve en Spring Data Neo4j 8.1.1 (la proyección se resuelve contra la entidad y falla con "Invalid property ... of bean class PreguntaGuion"), así que las lecturas pasan a `Neo4jClient` con un `record` y mapeo propio (secciones 2 y 10). **Concurrencia (`HistoriaConcurrenciaTest`, `HistoriaSinBloqueoTest`):** dos pedidos de la misma persona sobre la misma pregunta, soltados a la vez, con 20 preguntas por combinación. Con el bloqueo del nodo `PersonaMayor`: responder + responder, responder + saltear y saltear + saltear dan 20 de 20 pares correctos (un pedido registrado y el otro con `409`, una sola relación), 0 duplicados y 0 errores. Control sin bloqueo (mismo código de producción con `bloquearPersona` anulado): la carrera aparece en 56 de 60 pares (19, 19 y 18 pares con los dos pedidos registrados y la relación duplicada), así que la prueba con bloqueo sí demuestra algo. `./mvnw test` completo: 353 pruebas, 0 fallas.
- **v10 (09/11/2026):** el checklist de la sección 10 marca qué pasos están hechos (0, 1, 2 y 3) y cuáles faltan (4, 5, 6 y 7), para que sirva de memoria del estado entre sesiones. Cambio de infraestructura de pruebas: `pom.xml` agrega `-Dspring.datasource.hikari.maximum-pool-size=5` al `argLine` de surefire (junto a la zona horaria que ya tenía). Motivo: Spring cachea cada contexto de prueba con su propio pool de hasta 10 conexiones y PostgreSQL admite 100, así que con diez contextos o más la suite fallaba con "FATAL: sorry, too many clients already" (la suite venía con 9 contextos, el commit 1 llegó a 10 y el commit 2 a 12). Con el pool de 5 (comprobado: `maximumPoolSize=5`), `./mvnw test` completo da 353 pruebas, 0 fallas y 0 errores de conexión.
- **v11 (09/11/2026):** paso 4 del checklist (extractor). **Decisiones:** (a) 5.6: un año anterior al nacimiento se descarta y se sigue con la edad mencionada y después con la franja de la pregunta; un año calculado que quedaría en el futuro también se descarta y se sigue igual. (b) Cola mínima en este paso: `ColaDeExtracciones` (un hilo, en memoria, `encolar`, `esperarQueSeVacie`, `@PreDestroy`); el paso 5 le suma el estado "procesando" y la prioridad del chat, y la extracción pasa a esperar su turno sin el tope de 10 s. **`ia/`:** `ModeloConversacional` tiene un método `default responder(..., OpcionesDeLlamada)` que delega en el de siempre (el chat y `ModeloFalso` no cambian); `OllamaModeloConversacional` usa con las opciones un segundo cliente (`ollamaChatModelExtraccion`, tope de 25 s, propiedad `alma.ia.timeout-extraccion-ms`) y los beans actuales de `ConfiguracionIa` pasan a `@Primary`. **Prompt:** `historia/prompt-extractor.txt` es el v2 con la regla 2 cambiada (una década no es un año, `anio` va null) y un tercer ejemplo con una década. **Escritura:** todo el recuerdo en una transacción de Neo4j (`EscrituraDelGrafo.registrarRecuerdo`), con el nodo de la persona bloqueado; la llamada al modelo queda fuera de la transacción. `responder` lee nombre, apellido y fecha de nacimiento de la persona y texto y franja de la pregunta antes de encolar, así el hilo de la cola no toca PostgreSQL. **Pruebas** (`./mvnw test`: 419, 0 fallas; eran 358): `CalculoDeAnioTest`, `ExtractorDeRecuerdosTest` (sin Spring), `ExtraccionIntegrationTests` (con `ModeloDePrueba`); `HistoriaGuionIntegrationTests`, `HistoriaConcurrenciaTest` y `HistoriaSinBloqueoTest` ahora usan el modelo de prueba y esperan la cola antes de limpiar. **Anexo C con el extractor real** (gemma4:cloud, un pedido por caso, 13 de 13 sin respaldo y 13 de 13 con los criterios: E3 sin año, E7 sin Ana, E11 sin Pepe en `personas`); resultados en `alma-anexo-c/corrida-extractor-real-2026-11-09.md`, fuera del repositorio. Observación: en E2 el modelo puso "panadería" como lugar, y en E5 y E6 devolvió los vínculos "nieta" y "pareja" como personas sin nombre.
- **v12 (09/11/2026):** paso 5 del checklist (cola y prioridad del chat). **`LimitadorDePedidos`:** el turno sigue siendo un `Semaphore` justo. `ejecutar` (chat) conserva su firma, sus 10 s y el orden de llegada entre chats, y cuenta a los chats que esperan; `ejecutarEnSegundoPlano` (extracción) **no tiene tope de espera**: espera en un monitor a que el turno esté libre y ningún chat esté esperando, nunca se bloquea dentro de la cola del semáforo (así un chat que llega después no queda detrás) y, si toma el turno y aparece un chat, se lo devuelve. Quien suma o resta un chat y quien suelta el turno avisa por el monitor; la extracción además vuelve a mirar cada 250 ms, como red de seguridad contra un aviso perdido. Si el hilo se interrumpe mientras espera, falla con `ModeloNoDisponibleException` y conserva la marca de interrupción. **`OllamaModeloConversacional`:** el método con `OpcionesDeLlamada` (extracción) usa el nivel de segundo plano y el de dos argumentos (chat) el de siempre; no cambió ninguna firma. El tope de 25 s de cada llamada sigue siendo del cliente HTTP y no incluye la espera por el turno. **`ColaDeExtracciones`:** `estaProcesando(personaId)` es verdadero mientras la persona tenga un trabajo en la cola o en curso (todavía no lo expone ningún endpoint: lo lee el 7.5, paso 6); el contador solo queda subido si el trabajo fue aceptado (si el encolado se rechaza, por ejemplo con la cola apagada, vuelve atrás). `apagar()` (`@PreDestroy`) interrumpe el trabajo que espera turno o llama al modelo, no escribe su recuerdo y descarta los que no habían empezado, bajando sus contadores. **Pruebas** (`./mvnw test`: 438 pruebas, 0 fallas; eran 430): las de prioridad del limitador y de `ia/` se repiten en rondas (150 y 40), porque sin la prioridad el orden correcto depende de una carrera de microsegundos (el semáforo justo le da el turno al chat casi siempre): con la prioridad anulada las tres fallan (4 corridas de 4) y con ella pasan (5 de 5); las esperas de las pruebas del limitador, de `ia/` y de la cola tienen `@Timeout`. Límite que se mantiene: una extracción que ya está llamando al modelo no se corta, y un chat que llega en ese momento espera hasta 10 s y puede recibir el `503` de siempre.
- **v13 (09/11/2026):** paso 6 del checklist (repreguntas). **Tres decisiones:** (a) **la "pregunta" que ve el extractor en una repregunta es el texto fijo de la repregunta**, sin el título del recuerdo (variantes probadas con el modelo real; ver 5.7). (b) **`GET /repreguntas/siguiente` con un `preguntaId` desconocido o sin recuerdos da `NINGUNA`**, no `404`. (c) **`PROCESANDO` es por persona**, no por recuerdo, y tiene prioridad sobre cualquier candidata (se mira antes de leer el grafo). **Corrección del Anexo C:** el caso E13 decía "pregunta 17", pero lo que se corrió (`correr.mjs`, `REPREGUNTA_ANIO`) fue el texto fijo de la repregunta de `ANIO`; la tabla ahora dice eso. **Servicio:** `RepreguntaService` (`siguiente`, `responder`, `saltear`) y `SelectorDeRepregunta` (función pura: la primera de la lista `repreguntas` que falta, tiene menos de 2 intentos y no está en `excluir`; entre recuerdos, el más antiguo). Responder y saltear cuentan el intento en una transacción de Neo4j con el nodo de la persona bloqueado (dos pedidos a la vez dejan uno registrado y el otro con `409`; controlado quitando el bloqueo); el chequeo de "está en la lista, todavía falta y tiene menos de 2" es una sola consulta, con las mismas condiciones que usa la lectura de 7.5 (`CypherRepreguntas`). `409` con "Esa pregunta ya no hace falta." si el dato no aplica, ya no falta o ya se preguntó 2 veces; `404` si el recuerdo no existe o no es de la persona. **Extracción de una repregunta:** el mismo extractor con la misma validación de contenido; `ColaDeExtracciones` llama a `RecuerdoService.completar` y, si el resultado es el respaldo, no cambia nada (una repregunta no tiene respaldo). `completar` aplica solo lo del dato preguntado: `ANIO` con un año o una edad (sin franja), solo si el recuerdo no tiene año o es `ESTIMADA`; `LUGARES` y `PERSONAS` suman los nuevos sin duplicar (para eso `enlazarLugar` pasa a `MERGE`). Nunca cambia el título ni la descripción. **Swagger:** los `ResponseEntity<Void>` se documentaban como `200`; los cuatro `POST` de `HistoriaController` declaran ahora su código real (`202` y `204`) con `@ApiResponse`. **Pruebas** (`./mvnw test`: 512 pruebas, 0 fallas; eran 489 después de la tanda 1 del paso 6): `SelectorDeRepreguntaTest`, `RepreguntaServiceIntegrationTests` (40, a nivel de servicio), `RepreguntaEndpointsIntegrationTests` (23: 401, 403, 404, 400, 409, códigos HTTP y esquemas de `/v3/api-docs`) y tres casos nuevos en `ColaDeExtraccionesTest`; `RecuerdosDePrueba` es el helper que arma recuerdos en el grafo.
- **v14 (09/11/2026):** paso 7 del checklist (cierre). **Documentos:** `modelo-de-grafo.md` (tipos de id, `repreguntas` y los tres contadores, reglas 7 y 9, el ejemplo de `APROXIMADA` y la nota de la copia de `restricciones.cypher`), `casos-de-uso.md` (CU-03: estado del Back, y la introducción de cada pregunta y las repreguntas en las reglas de negocio; la descripción no se modifica) y `CLAUDE.md` (enlace a este contrato, paquete `historia` y Neo4j en la Etapa 3). **Prueba nueva:** `RestriccionesCypherTest` compara el `restricciones.cypher` del modelo con la copia de la aplicación, ignorando el comentario de la copia (se saltea si no encuentra `docs/`); se controló en las dos direcciones. **Revisión del contrato contra el código** (secciones 5, 7 y 10): solo correcciones de texto, sin cambios de comportamiento. (a) El nodo de la persona lo crean 7.1 a 7.4, no las repreguntas. (b) Se lee también el `apellido` de PostgreSQL. (c) El trabajo de extracción lleva más datos de los que decía (nombre, apellido, fecha de nacimiento, texto y franja de la pregunta). (d) La propia persona se descarta también con nombre y apellido, y las personas repetidas dentro de una respuesta cuentan una sola vez; el respaldo guarda la respuesta sin espacios en los extremos. (e) El mapeo de las introducciones queda explícito: resto 0, 1 y 2 de `orden` entre 3, en el orden de las frases. (f) En `ia/` solo la temperatura y el tope de tokens son parámetros de la llamada; el tiempo máximo es del segundo cliente de Ollama. (g) Las listas de clases de la sección 10 dicen "entre otras". (h) "No se tocan" reconoce las dos excepciones (la precondición y el `pom.xml` de la v10). (i) El encabezado pasa a "implementado" con el commit revisado, la sección 13 queda hecha, y los "ver abajo" de la v12 y la v13 ahora llevan sus números. (j) La sección 12 ya no fija un número de avisos INFO. **Swagger (queda registrado acá porque la v13 no lo mencionaba):** springdoc nombra los esquemas por el nombre simple de la clase, y `juegos.dto.RespuestaRequest` y `historia.dto.RespuestaRequest` se llamaban igual, así que los `POST .../respuesta` de historia mostraban en `/v3/api-docs` el cuerpo de juegos (`ejercicioId`, `opcionId`, `ordenOpcionIds`) en lugar de `{ "texto": "..." }`. Se renombró la clase de historia a `RespuestaTextoRequest` (sin tocar juegos); la usan 7.3 y 7.6, y `RepreguntaEndpointsIntegrationTests` comprueba en `/v3/api-docs` los esquemas de los endpoints de historia y que el de juegos sigue con el suyo. **Pruebas:** `./mvnw test`: 514 pruebas, 0 fallas (eran 512). Queda abierta la prueba manual del reinicio del back con una extracción en cola (sección 11); la de repreguntas por Swagger ya se hizo.
- **v15 (10/11/2026):** ajuste por `docs/contrato-historia-de-vida.md` (v3). **5.9:** las respuestas del guion y de las repreguntas pasan por el detector de riesgo del chat antes de guardarse o encolarse (sigue sin loguearse el texto ni clasificarse el contenido por tema). **7.3 y 7.6:** la respuesta aceptada lleva cuerpo `{ "estado": "ACEPTADA" }` con `202`; con alarma devuelve `200` con `{ "estado": "ALARMA", "respuesta": "..." }` y no se guarda, no se cuenta el intento y no se encola nada; con riesgo no se valida la pregunta ni el recuerdo (no hay `404` ni `409`). No cambia nada más del contrato.
---

## Anexo A. Guion

Contenido de `src/main/resources/historia/guion.json`. Los ids son fijos y no se reutilizan; el `orden` es único. "Franja" son edades (sin franja: no sirve para estimar el año). **Repreguntas** es la lista (en orden) que se guarda en el nodo; `ANIO` además se ofrece cuando el año quedó estimado.

| id | orden | Pregunta | Franja | Repreguntas |
|---|---|---|---|---|
| 1 | 1 | ¿Dónde naciste? | 0 a 0 | (ninguna) |
| 2 | 2 | ¿Cómo era la casa donde pasaste tu infancia? | 0 a 11 | LUGARES, PERSONAS |
| 3 | 3 | ¿Con quiénes vivías en tu infancia? | 0 a 11 | PERSONAS |
| 4 | 4 | ¿Cómo era el barrio donde creciste? | 0 a 17 | LUGARES, PERSONAS |
| 5 | 5 | ¿A qué te gustaba jugar en tu infancia? | 4 a 11 | PERSONAS |
| 6 | 6 | ¿Qué comida de tu infancia te acordás con cariño? | 0 a 11 | PERSONAS |
| 7 | 7 | ¿Qué te acordás de tu escuela primaria? | 6 a 12 | ANIO, LUGARES, PERSONAS |
| 8 | 8 | ¿Te acordás de algún maestro o maestra que te haya marcado? | 6 a 17 | ANIO, LUGARES |
| 9 | 9 | ¿Quién fue una persona muy importante para vos en tu infancia? | 0 a 11 | PERSONAS |
| 10 | 10 | ¿Con quiénes pasabas el tiempo en tu adolescencia? | 12 a 17 | LUGARES |
| 11 | 11 | ¿Qué música o qué bailes te gustaban de joven? | 12 a 25 | LUGARES |
| 12 | 12 | ¿Cuál fue tu primer trabajo? | 14 a 25 | ANIO, LUGARES, PERSONAS |
| 13 | 13 | ¿Cómo fue la primera casa donde viviste por tu cuenta? | 18 a 35 | ANIO, LUGARES, PERSONAS |
| 14 | 14 | ¿Cómo conociste a tu pareja? | 18 a 35 | ANIO, LUGARES |
| 15 | 15 | Si tuviste hijos o hijas, ¿cómo fue el día en que nació el primero? | sin franja | ANIO, LUGARES, PERSONAS |
| 16 | 16 | ¿Cuál fue uno de los días más felices de tu vida? | sin franja | ANIO, LUGARES, PERSONAS |
| 17 | 17 | ¿Hubo algún viaje que te haya marcado? | sin franja | ANIO, LUGARES, PERSONAS |
| 18 | 18 | ¿Hubo alguna mudanza que te haya cambiado la vida? | sin franja | ANIO, LUGARES |
| 19 | 19 | ¿Qué logro de tu vida te hace sentir más orgullo? | sin franja | ANIO, PERSONAS |
| 20 | 20 | ¿Cómo era un domingo típico con tu familia? | sin franja | LUGARES, PERSONAS |
| 21 | 21 | ¿Qué tradición o comida de tu familia te gustaría que no se olvide? | sin franja | PERSONAS |

## Anexo B. Datos de ejemplo para `ana`

`ana` (nacida el 12/05/1950) con las preguntas 1, 2, 7 y 17 respondidas, la 8 salteada y la 3 como próxima pendiente. Recuerdos:

- "Nací en Cipolletti": 1950, `ESTIMADA`, `INFANCIA`, lugar Cipolletti (pregunta 1).
- "La casa de la calle San Martín": 1958, `APROXIMADA`, `INFANCIA`, con Marta (hermana) y Rosa (madre), en Cipolletti (pregunta 2).
- "Mi maestra Elena": 1957, `EXACTA`, `INFANCIA`, con Elena (maestra), en la Escuela N° 12 (pregunta 7).
- "Un viaje a la costa": **sin año**, con una persona sin nombre (marido), lugar "la costa", con `intentosAnio` en 1 (pregunta 17).

Cubre un recuerdo sin año, una persona sin nombre y una repregunta con un intento ya usado. Se carga solo si `alma.seed.enabled=true` y `ana` no tiene recuerdos ni progreso (ninguna relación `RESPONDIO` ni `SALTEO`). `carlos` queda sin historia. (Los archivos `datos-de-ejemplo.cypher` del modelo son otra cosa: se cargan a mano en un Neo4j de pruebas y no sirven para la aplicación.)

## Anexo C. Prueba del modelo (respuestas sintéticas)

Antes de construir el extractor, se manda cada respuesta, con el texto de su pregunta, al prompt del extractor contra `gemma4:cloud`, y se anota si el resultado cumple. Casos:

| N° | Pregunta | Respuesta | Debe dar |
|---|---|---|---|
| E1 | 7 (escuela) | "En 1957 empecé primer grado en la Escuela 12. Mi maestra Elena me enseñó a leer." | `anio` 1957; persona Elena (maestra); lugar Escuela 12 |
| E2 | 12 (primer trabajo) | "Trabajé en una panadería cuando tenía 17 años, con mi tío." | `edadMencionada` 17; persona sin nombre con vínculo "tío"; sin `anio` |
| E3 | 2 (casa) | "Por los años 50, en Cipolletti, en una casa con patio." | Sin `anio` (una década escrita como "años 50" no se convierte en año, sección 12: el recuerdo se estima con la franja de edad de la pregunta, y la descripción puede conservar "años 50"); lugar Cipolletti |
| E4 | 17 (viaje) | "Un viaje a la costa con mi marido, me acuerdo del mar." | Sin `anio` ni edad; persona con vínculo "marido" y sin nombre; lugar la costa |
| E5 | 16 (día feliz) | "Cuando nació mi nieta." | Respuesta corta: título y descripción no vacíos; sin `anio`; sin personas con nombre inventado |
| E6 | 14 (pareja) | "Lo conocí en un baile, se llamaba Roberto." | Persona Roberto; **sin** `anio` |
| E7 | 10 (adolescencia) | "Me llamo Ana y salía con mis amigas Marta y Rosa." (la persona es Ana) | Marta y Rosa; **Ana no entra** como persona |
| E8 | 9 (persona importante) | "Mi abuela, que me crió." | Persona sin nombre con vínculo "abuela" |
| E9 | 21 (tradición) | "Los ñoquis del 29, con toda la familia en la casa de mi hermana en Neuquén." | Lugar Neuquén; persona con vínculo "hermana"; sin `anio` |
| E10 | 5 (juegos) | "No me acuerdo de nada." | No inventa nada; título y descripción no vacíos; sin personas ni lugares |
| E11 | 5 (juegos) | "Jugaba con mi perro, el Pepe, en el patio de casa." | **Pepe no entra** como persona; aparece en la descripción; lugar el patio; sin `anio` |
| E12 | 15 (hijos) | "Mi hijo Martín nació en 1978, en el Hospital Regional." | `anio` 1978; persona Martín (hijo); lugar Hospital Regional |
| E13 | el texto fijo de la repregunta de `ANIO` ("¿Te acordás en qué año fue, o cuántos años tenías?"), como repregunta | "Y fue cuando yo tenía 30 años." | `edadMencionada` 30; sin personas ni lugares nuevos |

Criterios: JSON válido en el 90 % o más (con la limpieza y la conversión del backend); **ningún dato inventado** en ningún caso (año, edad o nombres que no están en el texto); E7 sin la propia persona; E11 sin la mascota como persona. Los resultados se anotan en el informe de la PPS, no en este contrato. Si el modelo no cumple, se prueba `gpt-oss:120b-cloud` y se avisa antes de seguir.
