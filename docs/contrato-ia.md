# Contrato del proveedor de IA (Gemini u Ollama) · Alma Acompaña

**Estado:** acordado entre los dos. Es la referencia única para este cambio. Si algo cambia, se cambia primero acá y se avisa al otro.

**Por qué existe:** desde el 9 de noviembre de 2026 el modelo `gemma4:cloud` de Ollama Cloud dejó de ser gratuito (error `402 Payment Required` en las cuentas sin créditos). El chat dejó de responder. Este contrato agrega **Gemini** como proveedor del modelo y deja **Ollama** como alternativa, con un interruptor en el `.env`.

**Fuente de verdad:** `docs/contrato-chat.md` (secciones 2, 5.6, 5.7, 5.8 y 10) y la documentación de arquitectura (sección 4: solo el paquete `ia/` conoce al modelo). Este contrato **solo** cambia de dónde sale el modelo; el comportamiento del chat, los guardrails, el system prompt, los endpoints y los textos no cambian. Si este contrato contradice al del chat en lo que toca al modelo, gana este.

**Dónde va:** `docs/contrato-ia.md`. El `CLAUDE.md` debe enlazarlo en la sección "Documentos".

**Última revisión contra el repo:** v1 contra el commit `d35f1a9` (merge del PR #7, `feature/microcapsulas`), `main` del 11/11/2026. Desde la v1.3 (11/11/2026) también refleja la rama `feature/historia-de-vida` (extracción de recuerdos, calentamientos y regla 7 del prompt), todavía sin commit al escribir esta revisión.

**Contratos relacionados:** `docs/contrato-chat.md` (el módulo que consume el modelo) y `docs/contrato-login.md` (convenciones generales).

**Aviso a Paula:** el paquete `ia/` es parte del módulo Chat. Este cambio agrega clases nuevas y toca **solo anotaciones y configuración** de lo que ya existe en `ia/`. Se avisa en la descripción del PR. Desde la v1.3 también hay en `ia/` la extracción con Gemini (`GeminiModeloConversacional`, `ConfiguracionIa`) y `CalentamientoDeGemini`, y en `chat/` el system prompt (regla 7) con su test (`SystemPromptDelChatTest`): también son del módulo Chat y hay que avisarle a Paula.

---

## 1. Alcance

**Incluye (solo back):**
- Una implementación nueva de `ModeloConversacional` para Gemini, en `ia/`.
- El interruptor `IA_PROVEEDOR` (`gemini` u `ollama`) y las variables de Gemini.
- La dependencia de Spring AI para Gemini.
- Pruebas automáticas de la implementación nueva y de la selección de proveedor.
- La documentación de privacidad (sección 5) y la nota para el informe (sección 8).

**No incluye:**
- **Nada de front.** El front sigue llamando a `/api/chat/saludo` y `/api/chat/mensajes`, con el mismo `503` y el mismo texto cuando el modelo falla.
- Cambios en el system prompt, las listas de guardrails, `chat.properties` (respuestas, listas y límites), el saludo o los endpoints.
- Streaming, llamadas con imágenes o audio, ni uso de herramientas del modelo.
- Alertas por superar la cuota o el costo.

---

## 2. Decisiones

| Tema | Decisión | Motivo |
|---|---|---|
| Proveedor por defecto | `gemini` | Ollama Cloud ya no es gratuito |
| Interruptor | Variable `IA_PROVEEDOR` con los valores `gemini` u `ollama`. Cualquier otro valor hace fallar el arranque con un mensaje claro en español | Cambiar de proveedor sin tocar código |
| Modelo de Gemini | `gemini-3.5-flash-lite` (variable `GEMINI_MODEL`) | Probado con la clave del equipo: español rioplatense natural, 95 tokens en total y sin tokens de razonamiento. `gemini-2.5-flash-lite` ya no se ofrece a cuentas nuevas |
| `gemma-4-*` por la API de Google | **No se usa** | Probado: mezcla el razonamiento en el texto de la respuesta y gasta 265 de los 400 tokens pensando. Alma no puede mostrar nunca el razonamiento |
| Ollama | Se conserva tal cual (`IA_PROVEEDOR=ollama`). No se borra código ni el contenedor de Docker | Plan B local, sin costo, para quien tenga el equipo |
| Interfaz | No cambia. `ChatService` sigue hablando solo con `ModeloConversacional` | Arquitectura, sección 4 |
| Una implementación a la vez | Solo una de las dos implementaciones está activa (condición de arranque). Nunca hay dos candidatas para inyectar | Evita ambigüedad en Spring |
| Sin clave | Si el proveedor es `gemini` y falta `GEMINI_API_KEY`, **la aplicación arranca igual** y escribe un aviso en el log. Cada pedido al chat falla con `ModeloNoDisponibleException` (503 con el texto de siempre) | El resto de los módulos tiene que poder correr sin la clave, y `contextLoads` también |
| Parámetros | Los mismos: `alma.ia.temperatura` y `alma.ia.max-tokens`, y los mismos tiempos (30 s por llamada, 10 s de espera de turno) | Una sola configuración; el contrato del chat no cambia |
| Un pedido a la vez | Se mantiene el limitador | El nivel gratuito tiene un tope por minuto; no se pretende más concurrencia |
| Sin reintentos | Se mantienen cero reintentos en las dos implementaciones. **Única excepción:** la extracción con Gemini repite el pedido sin `responseMimeType` si la API lo rechaza con un 400 (ver más abajo) | Un pedido fallido es un solo pedido; no se rompe el máximo de 30 segundos |
| Reintentos del SDK de Google | Desactivados (un solo intento). El SDK trae su propio reintento (5 intentos por defecto, también ante cortes por tiempo), además del `RetryTemplate` de Spring AI; hay que apagar los dos | Si no, un 429 o un 500 se pediría varias veces y se rompería el máximo de 30 segundos |
| Qué se registra al fallar | Clase de la causa raíz y código HTTP si existe, nunca el mensaje | El SDK envuelve todo en una `RuntimeException` genérica y sus mensajes pueden incluir el cuerpo de la respuesta |
| URL base | Constante `https://generativelanguage.googleapis.com` si `alma.ia.gemini.base-url` está vacía. La clave siempre se pasa explícita | El SDK lee variables de entorno por su cuenta (`GOOGLE_GEMINI_BASE_URL`, `GOOGLE_API_KEY`, `GOOGLE_GENAI_USE_VERTEXAI`); así una variable perdida no redirige la clave a otro host |
| Turnos de Alma al inicio de la conversación | No se mandan como mensajes (la API de Gemini puede rechazarlos); su texto se agrega al final de las instrucciones de sistema, una línea por turno ("Lo último que le dijiste a la persona antes de este mensaje: …"). Si no queda ningún turno de la persona, falla sin llamar a la red | Pasa cuando el primer turno es la pregunta contextual del ánimo, de Alma. Con turnos que empiezan en la persona, el pedido no cambia |
| Razonamiento | No se pide razonamiento ni se configura el nivel. Si la respuesta trae partes marcadas como razonamiento, se descartan y solo se usa el texto final. Texto final vacío = falla | Contrato del chat, 5.6 |
| Respuesta cortada por el tope | Si el modelo termina por `MAX_TOKENS`, se usa el texto recibido y se escribe un aviso en el log (sin el texto) | Mismo criterio que con Ollama |
| Extracción de recuerdos (historia de vida) | Usa el proveedor activo, con un cliente propio (bean `googleGenAiClientExtraccion` / `googleGenAiChatModelExtraccion`; los del chat son `@Primary`): 25 s por llamada (`alma.ia.timeout-extraccion-ms`), temperatura y tope de tokens de `historia.properties`, turno en segundo plano detrás del chat (`ejecutarEnSegundoPlano`) y como máximo 2 pedidos al modelo por extracción (`ExtractorDeRecuerdos`: el pedido y un reintento si el JSON no sirve; después, el respaldo). La excepción de la fila siguiente puede sumar una llamada HTTP más, solo la primera vez. Con `ollama` existen los beans de extracción de Ollama; con `gemini`, solo los de Gemini | Contrato del grafo, 5.4 |
| Salida JSON de la extracción con Gemini | Se pide `responseMimeType=application/json`. Si la API responde 400 nombrando ese campo, se repite el pedido sin él, se recuerda para los siguientes y se avisa en el log (sin textos). Es el único caso en que un pedido hace dos llamadas HTTP, y solo la primera vez de cada ejecución del back: una extracción puede llegar a 4 llamadas HTTP (2 pedidos de 2 llamadas) esa vez. El prompt `historia/prompt-extractor.txt` no cambia | No todos los modelos lo admiten; la extracción no debe romperse |
| Tope de tokens de la extracción con Gemini | El mayor entre el de `historia.properties` (600) y `alma.ia.gemini.extraccion-max-tokens` (1500 por defecto). No se configura razonamiento. El log informa los tokens de razonamiento de cada respuesta | Si el modelo gasta tokens pensando y cuentan dentro del tope, un tope de 600 podría cortar el JSON. No se pudo verificar sin clave: se mide con el log |
| Calentamiento de Gemini al arrancar | `CalentamientoDeGemini` hace un `generateContent` de 1 token (`maxOutputTokens=1`, sin razonamiento ni JSON) con el texto fijo "Hola" (nunca texto de una persona) con cada uno de los dos modelos (chat y extracción), por los mismos beans de Spring AI que usa producción. Cada pedido va por `ejecutarEnSegundoPlano`: cede el turno al chat y no se superpone con otro pedido. Corre en un hilo aparte después de `ApplicationReadyEvent`, nunca rompe el arranque y sin clave no hace nada. Ante una falla registra solo la clase de la causa y el código HTTP. Se apaga con `alma.ia.gemini.calentar=false` | En la prueba real, pedir solo los metadatos del modelo (`models.get`) dejó la primera llamada del chat en ~3,5 s (después ~0,8 s): el costo está en la primera generación. Cuesta 2 pedidos por arranque con unos pocos tokens cada uno, que cuentan para el límite de pedidos por minuto del nivel gratuito. Si la demora es del lado de Google, solo se mueve al arranque; la conexión se cierra tras unos minutos sin uso |
| Errores | Cualquier error de red, del servicio (400, 401, 403, 429, 5xx), de la clave o de cuota, o una respuesta vacía → `ModeloNoDisponibleException` → `503` | Contrato del chat, 5.7 |
| Cómo se construye el cliente | A mano, en `ia/`, como ya se hace con Ollama (`ConfiguracionIa`), para controlar el tope de tiempo y los cero reintentos | La autoconfiguración usa reintentos y exige la clave al arrancar |
| System prompt | **Este contrato no lo cambia.** Se cambia solo en el contrato del chat: la v4 (11/11/2026) agrega la regla 7 y un ejemplo (el modelo no formula ni propone preguntas del guion de historia de vida) | Si las pruebas T1 a T18 muestran un problema de tono (por ejemplo "querido") o de comportamiento, se corrige en el contrato del chat, no acá |
| Dónde va `alma.ia.proveedor` | En `application.properties` (cambio aditivo), no en `chat.properties` | `@ConditionalOnProperty` no lee de forma confiable las propiedades de un `@PropertySource`; se verifica en el plan |

---

## 3. Configuración

### 3.1 Variables del `.env` (y `.env.example`)

```dotenv
# Proveedor de IA del chat: gemini | ollama
IA_PROVEEDOR=gemini

# Gemini (crear la clave en https://aistudio.google.com/apikey)
# ATENCIÓN: el nivel gratuito de Gemini NO debe usarse con datos reales de personas (ver docs/contrato-ia.md, sección 5).
GEMINI_API_KEY=
GEMINI_MODEL=gemini-3.5-flash-lite

# Ollama (solo si IA_PROVEEDOR=ollama)
OLLAMA_MODEL=gemma4:cloud
```

- En `.env.example` las variables de Gemini van con el valor de la clave vacío y el comentario de arriba.
- **Cada integrante crea su propia clave.** No se comparte ni se sube a Git. El `.env` ya está en `.gitignore`.

### 3.2 Propiedades

| Propiedad | Dónde | Valor |
|---|---|---|
| `alma.ia.proveedor` | `application.properties` | `${IA_PROVEEDOR:gemini}` |
| `alma.ia.gemini.api-key` | `application.properties` | `${GEMINI_API_KEY:}` |
| `alma.ia.gemini.modelo` | `application.properties` | `${GEMINI_MODEL:gemini-3.5-flash-lite}` |
| `alma.ia.gemini.base-url` | `application.properties` | vacío por defecto (usa el servicio de Google); solo lo cambian las pruebas |
| `alma.ia.timeout-extraccion-ms` | `historia.properties` | `25000`: tope por llamada de la extracción de recuerdos (los dos proveedores) |
| `alma.ia.gemini.extraccion-max-tokens` | sin definir (valor por defecto en el código) | `1500`: piso del tope de salida de la extracción con Gemini. **No tiene variable de entorno propia:** se cambia con `ALMA_IA_GEMINI_EXTRACCION_MAX_TOKENS` (mapeo automático de Spring) o agregando la línea a `application.properties` |
| `alma.ia.gemini.calentar` | sin definir (valor por defecto en el código) | `true`: calentamiento de Gemini al arrancar; `false` lo apaga. En las pruebas está en `false` (`src/test/resources/config/application.properties`) |
| `alma.historia.calentar-neo4j` | sin definir (valor por defecto en el código) | `true`: calentamiento de Neo4j al arrancar (contrato de historia de vida, límite 12); `false` lo apaga. En las pruebas está en `false` |

Los parámetros que ya existen (`alma.ia.temperatura`, `alma.ia.max-tokens`, `alma.ia.timeout-llamada-ms`, `alma.ia.espera-turno-ms`) siguen en `chat.properties` y valen para las dos implementaciones. La temperatura (0,1) y el tope de tokens (600) de la extracción están en `historia.properties`.

---

## 4. Back: qué implementar

```
ia/
  GeminiModeloConversacional     nueva: implementa ModeloConversacional contra Gemini
  ConfiguracionIa                se agregan los beans de Gemini (chat y extracción) y la condición para los de Ollama
  OllamaModeloConversacional     solo se le agrega la condición de arranque (anotación)
  CalentamientoDeGemini          (v1.3+) generateContent de 1 token al arrancar, con los dos modelos
historia/config/
  CalentamientoDeNeo4j           (v1.3+) lecturas de solo lectura al arrancar; contrato de historia de vida
```

**Pasos, con pausa de prueba al final de cada uno:**

1. **Dependencia y selección.** Agregar `spring-ai-starter-model-google-genai` al `pom.xml` (la versión sale del BOM de Spring AI 2.0.1). Agregar las propiedades de 3.2 y la condición de arranque: con `alma.ia.proveedor=ollama` solo existe `OllamaModeloConversacional` (y sus beans); con `gemini`, solo la nueva; con otro valor, falla con un mensaje claro. Verificar en el plan cómo evitar que la autoconfiguración de Spring AI intente armar el cliente de Gemini sin clave (propuesta: `spring.ai.model.chat=none` y construir a mano, como se hace con Ollama) y que Ollama sigue funcionando con eso. Prueba: arrancar el back con cada valor de `IA_PROVEEDOR`, y con `gemini` sin clave.
2. **Cliente de Gemini.** `GeminiModeloConversacional`: arma el pedido con las instrucciones como mensaje de sistema, los turnos como mensajes de usuario y de Alma, `alma.ia.temperatura` y `alma.ia.max-tokens`; usa el modelo de `alma.ia.gemini.modelo`; extrae solo el texto final (descarta partes de razonamiento); respeta el tope de 30 segundos y cero reintentos; usa el limitador; registra en el log la duración, los tokens de entrada y de salida y el tipo de error (nunca textos, ni el prompt, ni la clave, ni el mensaje de las excepciones). Verificar en el plan cómo se fijan el tope de tiempo y los cero reintentos con la API de Spring AI 2.0.1 y cómo se cambia la URL base para las pruebas.
3. **Pruebas automáticas** (sección 6).
4. **Prueba manual con el chat real** (sección 6) y resumen.

### Cambios en archivos compartidos (para la descripción del PR)

| Archivo | Cambio |
|---|---|
| `pom.xml` | Una dependencia nueva: `spring-ai-starter-model-google-genai` |
| `application.properties` | Las cuatro propiedades de 3.2 (aditivo) y, si hace falta, `spring.ai.model.chat=none` |
| `.env.example` | Las variables de 3.1 |
| `ia/ConfiguracionIa.java` y `ia/OllamaModeloConversacional.java` | Módulo Chat: solo condiciones de arranque y los beans de Gemini |
| `CLAUDE.md` | Stack y documentos (sección 7) |

**No se tocan:** `SecurityConfig`, `chat/` (servicio, controlador, guardrails, textos), `chat.properties`, `docker-compose.yml`, `AlmaApplication.java` y ningún otro módulo. `system-prompt.txt` no lo toca este contrato; lo cambió el del chat (v4, regla 7).

---

## 5. Privacidad y datos reales

Según los [términos adicionales de la API de Gemini](https://ai.google.dev/gemini-api/terms) (vigentes desde el 23/03/2026):

- **Nivel gratuito:** Google usa el contenido para mejorar sus productos y personas de Google pueden leer, anotar y procesar las entradas y salidas. Los términos dicen: "No envíen información sensible, confidencial o personal a los servicios sin costo".
- **Nivel pago** (proyecto de Google Cloud con facturación activa): Google no usa los prompts ni las respuestas para mejorar sus productos y los procesa según su acuerdo de procesamiento de datos. Igual los registra por un tiempo limitado, para detectar abusos y por obligaciones legales, y los datos pueden pasar por cualquier país donde Google tenga instalaciones.

**Qué recibe el modelo:**
- **En el chat:** el mensaje de la persona, su ánimo de hoy, los recordatorios del día (con medicamentos y dosis) y las citas médicas próximas (con médico y lugar). Son datos de salud. **Desde la historia de vida** (contrato de historia de vida, 2.13 y 5.5) también el progreso del guion (cantidades) y hasta 3 recuerdos relacionados con el mensaje, con sus títulos, descripciones, nombres de personas y lugares, años y vínculos: es lo que la persona contó de su vida.
- **En la extracción de recuerdos** (contrato del grafo, 5.4): el texto de la pregunta del guion y la respuesta de la persona, tal como la escribió, con cualquier nombre propio o dato personal que haya dicho. Sale por la misma API y con las mismas condiciones que el chat.
- **No recibe** el nombre, el usuario, la fecha de nacimiento ni datos de la cuenta (contrato del chat, sección 2). Esto último no impide que la persona escriba su propio nombre o el de sus familiares en un mensaje o en una respuesta: eso sí viaja.
- **Calentamientos:** solo envían un texto fijo ("Hola"), nunca datos de nadie.

Con el nivel gratuito de Gemini todo lo anterior queda al alcance de Google (ver arriba): por eso **solo se usan datos de prueba**.

**Reglas decididas:**
1. **Desarrollo, pruebas y presentación académica:** nivel gratuito, **solo con usuarios de prueba** (ana, carlos).
2. **Uso con personas reales:** no se usa el nivel gratuito. Alternativas: Gemini con facturación activa, un modelo propio en un servidor de la empresa (`IA_PROVEEDOR=ollama` con un modelo local) u otro servicio con contrato de empresa. La elección es de la empresa.
3. Antes de un uso real, la empresa debe consultar con su área legal el tratamiento de datos de salud y la transferencia de datos a otro país (la ley argentina de protección de datos personales, 25.326, trata los datos de salud como sensibles). Este documento no es una opinión legal.
4. La clave de Gemini es secreta: nunca se escribe en el código, en `.env.example`, en un log, en un commit ni en un chat.
5. Los logs no registran textos de mensajes, respuestas ni prompts (contrato del chat, 5.8), y tampoco la clave.

---

## 6. Pruebas y criterio de "listo"

**Pruebas automáticas** (no necesitan internet ni clave: usan un servidor HTTP falso que imita el servicio de Gemini, como las de Ollama):

- El pedido que sale: modelo, mensaje de sistema, turnos en orden y con el rol correcto, temperatura y tope de tokens, y la clave en la cabecera (no en la URL).
- Respuesta correcta: se devuelve el texto sin espacios en los extremos.
- Partes de razonamiento: se descartan y solo queda el texto final.
- Texto final vacío: falla (`ModeloNoDisponibleException`).
- Errores 400, 401, 403, 429 y 500: fallan, sin reintentar (un solo pedido).
- Demora mayor al tope: se corta a tiempo y libera el turno del limitador.
- Sin clave: falla sin hacer ninguna llamada de red.
- Logs: no contienen el texto del prompt, de los mensajes ni de las respuestas, ni la clave.
- Selección: con `ollama` solo existe la implementación de Ollama; con `gemini`, solo la de Gemini; con otro valor, falla el arranque con el mensaje claro; con `gemini` y sin clave, el contexto arranca (`contextLoads`).
- **Extracción con Gemini** (v1.3, `GeminiExtraccionTest`): el pedido lleva temperatura 0,1, el piso de tokens y `responseMimeType`; tope de 25 s por llamada sin reintento; cede el turno al chat; si la API rechaza `responseMimeType` se repite sin ese campo y se recuerda; un 400 que no es de ese campo no se reintenta; sin clave no llama a la red; los logs no tienen textos de la persona.
- **Dos clientes por proveedor** (`ClientesDeIaTest`): con `gemini` existen los dos `Client` y los dos modelos y el del chat es el `@Primary`; con `ollama`, los dos de Ollama y ninguno de Gemini; sin clave arranca sin clientes.
- **Calentamientos** (`CalentamientoDeGeminiTest`, `CalentamientoDeNeo4jTest`): Gemini hace un `generateContent` de 1 token con texto fijo por modelo, cede el turno al chat y no propaga fallas ni registra mensajes ni la clave; Neo4j solo lee (con Neo4j real no cambia nodos ni relaciones ni crea un `PersonaMayor` con id -1).
- **System prompt** (`SystemPromptDelChatTest`): primera línea, marcadores, LF, regla 7 (acotada a las preguntas del guion: no prohíbe las de seguimiento) y su ejemplo.
- Las pruebas que ya existen (`OllamaModeloConversacionalTest`, `ChatIntegrationTests`, etc.) siguen pasando sin modificarlas.

**Pruebas manuales** (las hace quien tiene la clave, con `docker compose up -d` y el back andando):

1. Con `IA_PROVEEDOR=gemini` y la clave, entrar con ana, abrir el chat y escribir un mensaje cotidiano: responde en español rioplatense, con voseo, sin mostrar razonamiento.
2. Escribir un mensaje de urgencia y uno de malestar grave: salen las respuestas fijas, sin llamar al modelo.
3. Con una clave inválida o vacía: el chat muestra "Alma no pudo responder en este momento. Intentá nuevamente en unos segundos." y el resto de la aplicación sigue andando.
4. Con `IA_PROVEEDOR=ollama`: el comportamiento es el de antes (si el modelo de Ollama no tiene créditos, mismo mensaje de error).
5. Repetir con Gemini los casos de prueba T1 a T18 del contrato del chat (anexo B) y anotar los resultados para el informe.

**Listo cuando:** pasan todas las pruebas automáticas (`./mvnw test` completo), las manuales 1 a 4 salen bien y los resultados de T1 a T18 quedaron anotados.

---

## 7. Documentos a actualizar

- `CLAUDE.md`: en el stack, "Spring AI con Gemini (por defecto) u Ollama, según `IA_PROVEEDOR`"; en "Decisiones tomadas", una línea: "El proveedor del modelo del chat es Gemini (`gemini-3.5-flash-lite`) u Ollama, según `IA_PROVEEDOR`; el nivel gratuito de Gemini no se usa con datos reales"; en "Documentos", este contrato.
- `docs/contrato-chat.md`: en la decisión "Modelo y razonamiento" y en 5.6, una frase: "El proveedor del modelo (Gemini u Ollama) se elige con `IA_PROVEEDOR`; ver `docs/contrato-ia.md`"; entrada v4 en el historial.
- `.env.example`: lo indicado en 3.1.

---

## 8. Nota para el informe

Texto sugerido para la sección de privacidad o limitaciones:

> *Alma Acompaña se desarrolló y probó con usuarios y datos de prueba, usando el nivel gratuito de la API de Gemini como proveedor del modelo de lenguaje. Los términos de ese nivel permiten que Google utilice el contenido para mejorar sus productos y que personal de Google lo revise, por lo que no es adecuado para datos reales de salud. Para una implementación con personas reales, la organización deberá usar un proveedor con condiciones de privacidad adecuadas (por ejemplo, un servicio de pago con acuerdo de procesamiento de datos o un modelo alojado en infraestructura propia) y evaluar con su área legal el tratamiento de datos sensibles. El sistema permite cambiar el proveedor del modelo con una variable de configuración, sin modificar el código.*

---

## 9. Cómo usar este documento con Claude Code

**Back**
> Leé `CLAUDE.md`, `docs/contrato-ia.md` y las secciones 2, 5.6, 5.7, 5.8 y 10 de `docs/contrato-chat.md`. Mostrame primero el plan del back según las secciones 2, 3 y 4, y no escribas código hasta que lo apruebe.

Si Claude Code propone algo que contradice el contrato, gana el contrato. Si hay que cambiarlo, se edita este archivo.

---

## 10. Pruebas realizadas

Mediciones reales (11/11/2026) con `gemini-3.5-flash-lite`, nivel gratuito, `IA_PROVEEDOR=gemini`, usuarios de prueba y el back local. Se tomaron de los tiempos del log del back; son rangos observados, no un benchmark.

| Qué se midió | Resultado | Cómo se vio |
|---|---|---|
| Extracción de recuerdos (historia de vida) | Entre 0,8 y 1,9 s, siempre con `respaldo: false` (no hizo falta el respaldo de título igual a la pregunta) | Líneas `Gemini (extracción) respondió en X ms` y `Extracción: recuerdo extraído (pedidos al modelo: N, respaldo: false, X ms)` |
| Chat en régimen | Entre 0,75 y 1,3 s por respuesta | Línea `Gemini respondió en X ms` |
| Primera llamada del chat tras arrancar, sin calentamiento | Unos 3,5 s (después, ~0,8 s). Con el calentamiento por `models.get` quedó igual (~3,5 s): el costo está en la primera generación | Línea `Gemini respondió en X ms` |
| Primera llamada tras arrancar, con el calentamiento de 1 token (v1.5) | **A confirmar** | Ver si `Gemini respondió en X ms` del primer chat baja de ~3,5 s a ~0,8 s |
| Regla 7 del prompt (contrato del chat, v4) | **OK solo para un caso:** la persona pide seguir con su historia de vida y Alma la deriva a la sección Historia de vida, sin inventar una pregunta del pasado | Prueba manual en el chat |
| Regla 7: conversación libre con preguntas de seguimiento sobre un recuerdo que la persona cuenta por su cuenta | **Pendiente de prueba** | — |
| Casos T1 a T18 del contrato del chat (anexo B) con Gemini | **Pendientes** | — |
| Pruebas automáticas | Suite completa del back: 648 pruebas, 0 fallos (11/11/2026) | `./mvnw test` |

Sin medir todavía: los tokens de razonamiento (`de razonamiento` en el log de cada respuesta), el costo en cuota y el comportamiento con varias personas a la vez.

---

## 11. Límites conocidos y trabajo futuro

**Límites conocidos**

| Límite | Detalle |
|---|---|
| Plan gratuito: tope de pedidos y sin privacidad | El nivel gratuito de Gemini tiene un tope de pedidos por minuto (y otros límites de uso); si se supera, el chat responde con el `503` de siempre. Y Google puede usar y leer lo que se envía: **solo datos de prueba** (sección 5). Los dos calentamientos suman 2 pedidos por arranque |
| `LimitadorDePedidos` | Es un semáforo global de **un pedido a la vez**, pensado para un servicio que atiende un pedido a la vez (el plan gratuito de Ollama Cloud, o un modelo propio de un solo hilo). Lo comparten todas las personas y los dos proveedores: con varias personas a la vez, un chat puede esperar hasta 10 s por su turno y fallar con `503`. La extracción solo toma el turno si ningún chat espera |
| Cola de extracción | Un solo hilo y en memoria (contrato del grafo, sección 2): si el back se reinicia se pierden las extracciones pendientes (la pregunta queda respondida sin recuerdo). Con 0,8 a 1,9 s por extracción no es hoy un cuello de botella |
| Lectura del grafo en cada mensaje | El chat lee Neo4j (progreso y recuerdos) con un tope de **2 s** por mensaje, 4 hilos y sin cola; pasado el tope, ese mensaje sale sin recuerdos. El calentamiento de Neo4j ayuda al primer mensaje tras arrancar, no al que sigue a una pausa larga (contrato de historia de vida, 5.5.2 y límites 11 a 13) |
| Sin cortacircuitos si Neo4j cae | Con Neo4j caído o con la red descartando paquetes, cada mensaje pierde hasta 2 s antes de seguir sin recuerdos (contrato de historia de vida, límite 13) |
| Calentamientos | Ayudan al primer mensaje después de arrancar; la conexión a Gemini se cierra tras unos minutos sin uso. Si la demora es del lado de Google, el calentamiento solo la mueve al arranque |
| Razonamiento y tope de tokens | No se verificó si los tokens de razonamiento de `gemini-3.5-flash-lite` cuentan dentro del tope de salida; por eso la extracción tiene un piso de 1500 tokens. El log informa los tokens de razonamiento |
| Salida JSON de la extracción | Si la API rechaza `responseMimeType`, la primera extracción hace una llamada HTTP más (sección 2) |
| Regla 7 del prompt | Es una instrucción al modelo, no una garantía: puede fallar. Conviene repetir la prueba manual cuando cambie el modelo (sección 10) |

**Trabajo futuro**
- **Implementación real:** un proveedor de pago (Gemini con facturación activa u otro con contrato de empresa) o un modelo propio en un servidor de la empresa (`IA_PROVEEDOR=ollama` con un modelo local), más la consulta legal de la sección 5.
- Completar las pruebas pendientes de la sección 10: T1 a T18, la conversación libre con la regla 7 y el primer mensaje con el calentamiento de 1 token.
- Si hace falta atender a varias personas a la vez: un limitador con más permisos o por proveedor y, si se mantiene el límite, una cola con avisos al usuario.
- Si la extracción se vuelve lenta o no se pueden perder pendientes: una cola persistente (por ejemplo, en PostgreSQL) y más de un hilo.
- Si Neo4j resulta inestable: un cortacircuitos de unos 30 s solo ante `ServiceUnavailable` o timeout (contrato de historia de vida, límite 13).
- Medir y, si hace falta, ajustar el tope de 2 s, el piso de 1500 tokens y los calentamientos.

---

## 12. Historial de cambios

- **v1.6 (11/11/2026):** coherencia con los contratos del chat (v4) y del grafo (v16): privacidad (sección 5) ahora incluye lo que recibe el modelo por la historia de vida y la extracción; propiedades nuevas (3.2); clases nuevas (4); excepción de los cero reintentos; nuevas secciones 10 (pruebas realizadas) y 11 (límites conocidos y trabajo futuro), y el historial pasa a la 12.
- **v1 (11/11/2026):** primera versión acordada, contra el commit `d35f1a9`.
- **v1.1 (11/11/2026):** tras verificar Spring AI 2.0.1, se agregan a la sección 2 las decisiones sobre los reintentos del SDK de Google, lo que se registra al fallar y la URL base.
- **v1.5 (11/11/2026):** el calentamiento de Gemini pasa de `models.get` a un `generateContent` de 1 token con texto fijo, por los dos modelos y cediendo el turno al chat. El system prompt cambió en el contrato del chat (v4, regla 7). Fines de línea: si fallan pruebas por saltos de línea, `chat.properties` se regenera con `git checkout`, pero `system-prompt.txt` **no** (descartaría la regla 7): se corrige con `sed -i 's/\r$//'`; `SystemPromptDelChatTest` detecta los `\r`.
- **v1.4 (11/11/2026):** calentamiento de las conexiones a Gemini al arrancar (`models.get`, apagable con `alma.ia.gemini.calentar`). Ver la sección 2.
- **v1.3 (11/11/2026):** la extracción de recuerdos usa el proveedor activo (clientes propios, 25 s, segundo plano, salida JSON con respaldo si la API rechaza `responseMimeType`, tope de tokens con piso propio para Gemini). Ver la sección 2.
- **v1.2 (11/11/2026):** se agrega a la sección 2 el tratamiento de los turnos de Alma al inicio de la conversación en la implementación de Gemini.
