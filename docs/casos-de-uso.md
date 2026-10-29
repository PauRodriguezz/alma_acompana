# Casos de uso de Alma Acompaña

## Contexto

Alma Acompaña es una herramienta digital accesible y no clínica, pensada para brindar compañía cotidiana, estimulación cognitiva y organización de rutinas a personas mayores. Hay dos actores principales:

- **Persona usuaria (adulto mayor):** conversa con el asistente digital, registra su estado de ánimo, realiza actividades lúdicas de memoria, consulta consejos de autocuidado y gestiona sus recordatorios.
- **Administrador de contenidos (operador):** gestiona el catálogo de ejercicios y las microcápsulas informativas desde un panel de administración, sin tocar el código fuente.

## Listado de casos de uso

| CU | Nombre | Módulo | Actor |
|---|---|---|---|
| CU-01 | Registrar estado de ánimo | Registro de bienestar | Persona mayor |
| CU-02 | Conversar con Alma | Chat de acompañamiento | Persona mayor |
| CU-03 | Responder pregunta guiada de historia de vida | Historia de vida | Persona mayor |
| CU-04 | Visualizar línea de tiempo | Historia de vida | Persona mayor |
| CU-05 | Realizar ejercicio de estimulación | Juegos y actividades de estimulación | Persona mayor |
| CU-06 | Consultar microcápsulas disponibles | Microcápsulas de cuidado y prevención | Persona mayor |
| CU-07 | Registrar/Editar/Eliminar recordatorio de medicación | Recordatorios | Persona mayor |
| CU-08 | Registrar/Editar/Eliminar recordatorio de cita médica | Recordatorios | Persona mayor |
| CU-09 | Recibir aviso de recordatorio | Recordatorios | Persona mayor, Sistema |
| CU-10 | Gestionar catálogo de ejercicios | Panel de operador (backoffice) | Administrador de contenidos |
| CU-11 | Gestionar microcápsula de información | Panel de operador (backoffice) | Administrador de contenidos |
| CU-12 | Autenticarse en el panel de operador | Panel de operador (backoffice) | Administrador de contenidos |
| CU-13 | Ajustar configuración de accesibilidad | Accesibilidad (transversal) | Persona mayor |
| CU-14 | Iniciar y cerrar sesión (persona mayor) | Autenticación (transversal) | Persona mayor |

## Formato de cada caso de uso

Cada caso tiene: actor(es), módulo, descripción, precondición, postcondición, camino básico, caminos alternos o flujos secundarios, reglas de negocio y pantallas relacionadas. En los pasos, "Actor" es lo que hace la persona y "Sistema" lo que hace la aplicación. Las referencias "C.A. Nº" indican el camino alterno que puede salir de ese paso.

---

## CU-01: Registrar estado de ánimo

- **Actor(es):** Persona mayor
- **Módulo:** Registro de bienestar
- **Descripción:** Permite a la persona mayor indicar, de forma simple y no invasiva, cómo se siente en un momento del día mediante una escala visual de emojis. A diferencia de un formulario tradicional, la selección del emoji no requiere confirmación ni comentario adicional: dispara automáticamente una pregunta contextual de Alma acorde al nivel seleccionado, que da inicio a la conversación libre (CU-02).
- **Precondición:**
  - El usuario se encuentra autenticado en la aplicación.
  - El widget de registro de ánimo se encuentra visible, como parte del inicio del chat de acompañamiento (CU-02).
- **Postcondición:**
  - Queda almacenado un nuevo registro de ánimo asociado al usuario, compuesto por: nivel seleccionado (escala de 1 a 5) y fecha/hora del registro.
  - El registro queda disponible como dato de contexto para el chat de acompañamiento (CU-02).
  - Se determina una pregunta contextual asociada al nivel seleccionado, que continúa el flujo dentro de CU-02.

### Camino básico

1. **Actor:** Selecciona el emoji que representa su estado de ánimo actual, dentro del widget mostrado por CU-02. (C.A. Nº1)
2. **Sistema:** Registrar el estado de ánimo.
   - 2.1. Guardar nivel seleccionado y fecha/hora actual, asociados al usuario autenticado.
   - 2.2. Asociar el registro como contexto disponible para el chat de acompañamiento (CU-02).
3. **Sistema:** Determinar una pregunta contextual asociada al nivel de ánimo seleccionado (por ejemplo, distinta según el usuario haya indicado un ánimo bajo, neutro o alto).
4. **Sistema:** Ocultar el widget de registro de ánimo.
5. **Sistema:** Entregar el control a CU-02, que muestra la pregunta contextual como siguiente mensaje de Alma en el chat.
6. Fin CU.

### Camino alterno Nº1: El usuario no selecciona ningún emoji

1. **Actor:** No interactúa con el widget de registro de ánimo.
2. **Sistema:** Mantener el widget visible mientras el usuario no realice una selección.
3. **Sistema:** Fin CU (el flujo de CU-02 continúa habilitando el chat libre sin pregunta contextual asociada).

### Reglas de negocio

- No existe límite de registros de ánimo por día; el usuario puede registrar su estado las veces que lo desee.
- La selección de un emoji dispara automáticamente la pregunta contextual, sin pasos de confirmación intermedios.
- El registro de ánimo más reciente del día es el que se utiliza como contexto para el saludo y la conversación del chat de acompañamiento (CU-02).

### Pantallas relacionadas

Pantallas en el Figma:
- Chat (inicio) (móvil y escritorio)
- Chat (después de elegir ánimo) (móvil)

---

## CU-02: Conversar con Alma

- **Actor(es):** Persona mayor
- **Módulo:** Chat de acompañamiento
- **Descripción:** Permite a la persona mayor mantener una conversación de acompañamiento con Alma, asistente conversacional basado en un modelo de lenguaje accedido mediante Ollama Cloud (perfil adoptado para el MVP), con la posibilidad de usar un modelo local mediante un cambio de configuración. Al iniciar, Alma saluda de forma proactiva contemplando los recordatorios del día (si los hay) y da paso al widget de registro de ánimo (CU-01); la selección de un emoji dispara una pregunta contextual que abre la conversación libre. El chat convive únicamente con los widgets de Registro de ánimo y Recordatorios al iniciar sesión; Historia de vida (CU-03) no se presenta como widget de inicio, sino que se ofrece de forma proactiva por Alma durante la conversación libre cuando existe progreso pendiente, o se accede desde su propia sección. La conversación es de tipo libre, no se conserva historial entre sesiones, y cuenta con reglas explícitas de seguridad (guardrails) que evitan diagnosticar, aconsejar sobre salud o sustituir el criterio de un profesional.
- **Precondición:**
  - El usuario se encuentra autenticado en la aplicación.
  - El usuario ingresa a la pantalla principal de la aplicación.
- **Postcondición:**
  - Se mantiene, durante la sesión activa, un intercambio de mensajes entre el usuario y Alma (no se persiste como historial consultable).
  - En caso de haberse mostrado recordatorios agrupados, estos quedan disponibles para consulta desde la sección Recordatorios (CU-09).
  - En caso de haberse registrado un estado de ánimo, queda aplicado lo definido en la postcondición de CU-01.

### Camino básico

1. **Actor:** Ingresa a la pantalla principal de la aplicación.
2. **Sistema:** Buscar recordatorios del usuario programados para el día actual. (C.A. Nº1)
3. **Sistema:** Componer saludo inicial.
   - 3.1. Agrupar los recordatorios encontrados en un único mensaje si hay más de uno.
   - 3.2. Incluir mención a los recordatorios del día dentro del saludo de Alma.
4. **Sistema:** Mostrar el saludo de Alma como primer mensaje del chat.
5. **Sistema:** Incluir CU-01: mostrar el widget de registro de ánimo debajo del saludo.
6. **Actor:** Interactúa con el widget de ánimo (CU-01) o lo ignora.
7. **Sistema:** Recibir de CU-01 la pregunta contextual generada (si el usuario registró un emoji) y mostrarla como mensaje de Alma; si no registró, habilitar directamente el chat libre.
8. **Actor:** Escribe un mensaje libre dirigido a Alma (respondiendo la pregunta contextual o iniciando otro tema).
9. **Sistema:** Procesar el mensaje del usuario. (C.A. Nº2 / Nº3 / Nº4)
   - 9.1. Mostrar indicador de espera ("Alma está pensando…").
   - 9.2. Incorporar como contexto el registro de ánimo del día (CU-01) y el progreso de historia de vida (CU-03), si existen.
   - 9.3. Evaluar si corresponde ofrecer continuar con historia de vida (progreso pendiente) como parte de la respuesta.
   - 9.4. Enviar la consulta al modelo conversacional junto con las reglas de seguridad (guardrails) configuradas.
10. **Sistema:** Mostrar la respuesta generada por Alma en el chat.
11. **Actor:** Continúa la conversación o abandona la pantalla.
12. **Sistema:** Retornar al camino básico, paso 8, mientras el usuario continúe escribiendo.
13. Fin CU.

### Camino alterno Nº1: No existen recordatorios para el día

2. **Sistema:** No encontrar recordatorios programados para el día actual.
3. **Sistema:** Componer saludo inicial sin mención a recordatorios.
4. **Sistema:** Retornar al camino básico, paso 4.

### Camino alterno Nº2: El modelo conversacional no responde

9. **Sistema:** Detectar error o demora excesiva en la respuesta del modelo.
   - 9.1. Mostrar mensaje: "Alma no pudo responder en este momento. Intentá nuevamente en unos segundos."
10. Fin CU.

### Camino alterno Nº3: Consulta fuera de los límites definidos (guardrails)

9. **Sistema:** Detectar que la consulta excede los límites definidos para el asistente (por ejemplo, pedido de diagnóstico, consejo médico o señales de riesgo).
   - 9.1. Mostrar una respuesta de derivación cálida, sin profundizar en el tema.
10. **Sistema:** Retornar al camino básico, paso 11.

### Camino alterno Nº4: Existe progreso pendiente de historia de vida

9. **Sistema:** Detectar, mediante el puntero de progreso del guión, que existen preguntas de historia de vida pendientes de responder.
   - 9.1. Incluir en la respuesta de Alma una invitación a continuar, por ejemplo: "¿Seguimos completando tu historia de vida?"
10. **Sistema:** Continuar según CU-03, camino básico A, si el usuario acepta.

### Reglas de negocio

- El chat no conserva historial de conversaciones entre sesiones; cada ingreso a la aplicación comienza con un nuevo saludo de Alma.
- El saludo inicial siempre antecede al widget de registro de ánimo (CU-01), y este a su vez al chat libre.
- Si existe más de un recordatorio para el día, deben presentarse agrupados en un solo mensaje, nunca uno por uno.
- El chat solo convive con los widgets de Registro de ánimo y Recordatorios al iniciar sesión; ningún otro módulo se presenta como widget en la pantalla principal.
- La navegación hacia las demás secciones (Historia de vida, Entretenimiento, Recordatorios, Panel de operador) se realiza mediante el menú inferior, no mediante interpretación de lenguaje natural del chat, salvo la invitación proactiva a continuar con historia de vida.
- Ante cualquier consulta fuera de los límites definidos para el asistente, el sistema nunca debe diagnosticar, aconsejar sobre salud, ni sustituir el criterio de un profesional.

### Pantallas relacionadas

Pantallas en el Figma:
- Chat (inicio) (móvil y escritorio)
- Chat (después de elegir ánimo) (móvil)
- Chat: pregunta de Alma (móvil y escritorio)

Sin crear todavía en el Figma: la pantalla del chat cuando Alma no puede responder y el indicador "Alma está pensando…".

---

## CU-03: Responder pregunta guiada de historia de vida

- **Actor(es):** Persona mayor
- **Módulo:** Historia de vida
- **Descripción:** Permite a la persona mayor responder, de a una por vez, preguntas guiadas sobre su historia de vida, siguiendo un guión con orden lógico. Cada respuesta es procesada por el sistema para actualizar el grafo de conocimiento del usuario en segundo plano. El progreso del guion (qué pregunta fue respondida o salteada) se guarda como un dato de negocio independiente del historial de mensajes del chat: este último no se persiste (CU-02), pero el progreso del guion sí, lo que permite retomar donde se dejó. Existen dos formas de acceder a una pregunta pendiente: que Alma la ofrezca de forma proactiva dentro de la conversación libre (camino básico A), o que el usuario la encuentre al ingresar a la sección Historia de vida (camino básico B).
- **Precondición:**
  - El usuario se encuentra autenticado en la aplicación.
  - Existe al menos una pregunta del guión pendiente de responder o saltear.
- **Postcondición:**
  - El grafo de conocimiento del usuario queda actualizado con la información relevada en las respuestas registradas durante la sesión.
  - Queda actualizado el puntero de progreso del guión de preguntas (última pregunta respondida o salteada), para permitir continuar en una sesión posterior, independientemente de que el historial de chat no se persista.

### Camino básico A: Continuar desde el chat (ofrecimiento proactivo de Alma)

1. **Sistema:** Detectar, durante una conversación en curso (CU-02), que existe progreso pendiente en el guión de historia de vida. (Incluido desde CU-02, C.A. Nº4)
2. **Sistema:** Ofrecer continuar con la historia de vida como parte de la respuesta de Alma en el chat.
3. **Actor:** Acepta continuar. (C.A. Nº1)
4. **Sistema:** Determinar la siguiente pregunta a presentar. (C.A. Nº4)
   - 4.1. Buscar el puntero de progreso guardado del usuario en el guión de preguntas.
   - 4.2. Seleccionar la siguiente pregunta del guión según el orden lógico definido, no respondida ni salteada aún.
5. **Sistema:** Mostrar la pregunta como mensaje de Alma en el chat.
6. **Actor:** Responde la pregunta con texto libre. (C.A. Nº2)
7. **Sistema:** Validar que la respuesta no esté vacía. (C.A. Nº5)
8. **Sistema:** Procesar la respuesta y actualizar el grafo de conocimiento (proceso en segundo plano, sin feedback visual al usuario).
   - 8.1. Extraer personas, lugares, fechas o referencias temporales y recuerdos relevantes mencionados en la respuesta.
   - 8.2. Guardar o actualizar los nodos y relaciones correspondientes en el grafo asociado al usuario.
9. **Sistema:** Actualizar el puntero de progreso, marcando la pregunta como respondida.
10. **Actor:** Decide continuar con la siguiente pregunta o seguir charlando de otro tema. (C.A. Nº3)
11. **Sistema:** Retornar al paso 4, si el usuario decide continuar con otra pregunta.
12. Fin CU.

### Camino básico B: Acceso desde la sección Historia de vida

1. **Actor:** Navega a la sección Historia de vida desde el menú inferior.
2. **Sistema:** Incluir CU-04: mostrar la línea de tiempo del usuario.
3. **Sistema:** Determinar la siguiente pregunta pendiente del guión. (C.A. Nº4)
   - 3.1. Buscar el puntero de progreso guardado del usuario.
   - 3.2. Seleccionar la siguiente pregunta no respondida ni salteada.
4. **Sistema:** Mostrar la pregunta pendiente junto a la línea de tiempo.
5. **Actor:** Toca la pregunta pendiente.
6. **Sistema:** Redirigir al chat (CU-02), mostrando la pregunta como mensaje de Alma.
7. **Sistema:** Continuar según el camino básico A, paso 6.

### Camino alterno Nº1: El usuario no acepta continuar en este momento

3. **Actor:** No acepta continuar con historia de vida.
4. **Sistema:** Mantener el progreso pendiente sin cambios, disponible para un próximo ofrecimiento o para acceso manual (camino básico B).
5. Fin CU.

### Camino alterno Nº2: El usuario saltea la pregunta

6. **Actor:** Selecciona la opción de saltear la pregunta.
7. **Sistema:** Registrar la pregunta como salteada en el puntero de progreso del usuario (sin actualizar el grafo).
8. **Sistema:** Retornar al camino básico A, paso 10.

### Camino alterno Nº3: El usuario detiene la sesión antes de continuar

10. **Actor:** Decide no continuar con más preguntas por el momento.
11. **Sistema:** Mantener el progreso guardado, disponible para retomarse en un próximo ingreso (camino básico A o B).
12. Fin CU.

### Camino alterno Nº4: No quedan preguntas pendientes en el guión

4. **Sistema:** No encontrar preguntas pendientes de responder o saltear en el guion.
5. **Sistema:** Informar al usuario que completó todas las preguntas disponibles por el momento.
6. Fin CU.

### Camino alterno Nº5: Respuesta vacía

7. **Sistema:** Informar que la respuesta no puede quedar vacía.
   - 7.1. Mostrar mensaje: "Escribí una respuesta o elegí saltear la pregunta."
8. **Sistema:** Retornar al camino básico A, paso 6.

### Reglas de negocio

- Las preguntas se presentan de a una por vez, siguiendo un guion con orden lógico (no aleatorio).
- El usuario puede saltear cualquier pregunta sin que esto le impida continuar con las siguientes.
- El puntero de progreso del guion es un dato de negocio independiente del historial de mensajes del chat; se guarda automáticamente ante cada respuesta o salteo, aunque el chat en sí no se persista.
- La actualización del grafo de conocimiento es siempre un proceso transparente: el usuario no visualiza el grafo ni recibe confirmación técnica de su actualización.
- El acceso a una pregunta pendiente puede darse tanto por ofrecimiento proactivo de Alma en el chat como por ingreso manual a la sección Historia de vida; ambos caminos comparten el mismo puntero de progreso.

### Pantallas relacionadas

Pantallas en el Figma:
- Chat: pregunta de Alma (móvil y escritorio)
- Historia de vida (móvil y escritorio)

---

## CU-04: Visualizar línea de tiempo

- **Actor(es):** Persona mayor
- **Módulo:** Historia de vida
- **Descripción:** Permite a la persona mayor consultar, en formato de línea de tiempo y en modo solo lectura, los recuerdos construidos a partir de sus respuestas a las preguntas guiadas (CU-03). Junto a la línea de tiempo, se muestra la próxima pregunta pendiente del guion, que redirige al chat (CU-03, camino básico B) si el usuario la selecciona. No se incluye en este alcance la edición ni eliminación de recuerdos, dado que implicaría operaciones de modificación directa sobre el grafo de conocimiento no contempladas en el MVP.
- **Precondición:**
  - El usuario se encuentra autenticado en la aplicación.
  - El usuario accede a la sección Historia de vida.
- **Postcondición:** Ninguna; es un caso de uso de solo consulta, salvo que el usuario seleccione la pregunta pendiente, en cuyo caso continúa según CU-03.

### Camino básico

1. **Actor:** Accede a la sección Historia de vida desde el menú inferior.
2. **Sistema:** Buscar los recuerdos asociados al usuario en el grafo de conocimiento. (C.A. Nº1)
3. **Sistema:** Ordenar los recuerdos de forma cronológica.
4. **Sistema:** Mostrar la línea de tiempo con los recuerdos encontrados.
5. **Sistema:** Incluir CU-03: determinar y mostrar la próxima pregunta pendiente del guión, si existe.
6. **Actor:** Selecciona un recuerdo para ver su detalle.
7. **Sistema:** Mostrar el detalle del recuerdo seleccionado, en modo solo lectura.
8. Fin CU.

### Camino alterno Nº1: El usuario no tiene recuerdos cargados

2. **Sistema:** No encontrar recuerdos asociados al usuario.
3. **Sistema:** Mostrar mensaje invitando a comenzar a responder preguntas de historia de vida (CU-03).
4. **Sistema:** Retornar al camino básico, paso 5.

### Reglas de negocio

- Los recuerdos se muestran siempre ordenados cronológicamente, sin opción de reordenamiento manual.
- La línea de tiempo y el detalle de cada recuerdo son de solo lectura; no se permite editar ni eliminar recuerdos en este alcance.

### Pantallas relacionadas

Pantallas en el Figma:
- Historia de vida (móvil y escritorio)

---

## CU-05: Realizar ejercicio de estimulación

- **Actor(es):** Persona mayor
- **Módulo:** Juegos y actividades de estimulación
- **Descripción:** Permite a la persona mayor realizar ejercicios breves de estimulación cognitiva (refranes, series, asociaciones y memoria). A diferencia de un catálogo de selección manual, es el sistema el que decide íntegramente qué ejercicio presentar en cada momento (incluida la categoría), procurando no repetir el último jugado siempre que exista otra alternativa y ajustando la dificultad de forma progresiva y transparente. Existe noción de sesión: el usuario puede abandonar en cualquier momento y continuar exactamente donde lo dejó la próxima vez que ingrese.
- **Precondición:**
  - El usuario se encuentra autenticado en la aplicación.
  - El usuario accede a la sección Entretenimiento > Jugar.
- **Postcondición:**
  - Queda registrado el resultado del ejercicio realizado (acierto o error), asociado al usuario.
  - El nivel de dificultad del usuario queda actualizado según su desempeño.
  - Queda guardado el progreso de la sesión (próximo ejercicio a presentar), permitiendo continuar en un próximo ingreso desde el punto en que se dejó.

### Camino básico

1. **Actor:** Accede a la sección Entretenimiento > Jugar.
2. **Sistema:** Determinar el próximo ejercicio a presentar. (C.A. Nº1)
   - 2.1. Buscar el progreso de sesión guardado del usuario (último ejercicio jugado, categoría y nivel de dificultad alcanzado).
   - 2.2. Seleccionar la categoría y el ejercicio siguiente según la secuencia definida por el sistema, acorde al nivel de dificultad actual del usuario.
   - 2.3. Excluir el último ejercicio jugado, siempre que exista otro ejercicio activo disponible en el nivel actual. Solo se consideran ejercicios activos del catálogo.
3. **Sistema:** Mostrar el ejercicio seleccionado.
   - 3.1. Si el ejercicio tiene texto a memorizar: mostrar solo ese texto y el botón "Ya lo recordé". Al tocarlo, ocultar el texto y mostrar consigna y opciones.
   - 3.2. Si el formato es opción múltiple: mostrar consigna y opciones, con la imagen de cada opción si la tiene.
   - 3.3. Si el formato es ordenar: mostrar los pasos desordenados y un botón "Listo".
4. **Actor:** Resuelve el ejercicio: elige una opción o ordena los pasos y confirma con Listo.
5. **Sistema:** Validar la respuesta: en opción múltiple, que la opción elegida sea la correcta. En ordenar, que el orden de todos los pasos coincida con el definido.
6. **Sistema:** Mostrar feedback inmediato de acierto o error.
7. **Sistema:** Registrar el resultado, actualizar el progreso de la sesión y ajustar el nivel de dificultad del usuario según su desempeño (de forma transparente, sin mostrar el criterio o número de nivel).
8. **Actor:** Decide jugar otro ejercicio o finalizar. (C.A. Nº2)
9. **Sistema:** Retornar al camino básico, paso 2, si el usuario decide continuar.
10. Fin CU.

### Camino alterno Nº1: No hay ejercicios disponibles sin repetir

2. **Sistema:** No encontrar ejercicios del nivel actual que no sean el último jugado.
3. **Sistema:** Permitir repetir el ejercicio jugado hace más tiempo.
4. **Sistema:** Retornar al camino básico, paso 3.

### Camino alterno Nº2: El usuario finaliza antes de completar un ejercicio

8. **Actor:** Abandona la pantalla sin resolver el ejercicio en curso.
9. **Sistema:** Descartar el ejercicio en curso sin registrar resultado ni afectar el nivel de dificultad, manteniendo el progreso de sesión en el último ejercicio completado.
10. Fin CU.

### Reglas de negocio

- El sistema decide íntegramente qué ejercicio presentar en cada momento, incluida la categoría; el usuario no elige manualmente.
- El sistema no repite el último ejercicio jugado, salvo que no exista otro ejercicio activo en el nivel actual (C.A. Nº1).
- El ajuste de dificultad es progresivo y transparente: el usuario percibe que "sube de nivel", pero nunca ve el criterio o número de dificultad exacto.
- No se muestra puntaje de tipo clínico o diagnóstico en ningún momento del ejercicio.
- Un ejercicio abandonado sin resolver no se contabiliza como acierto ni como error, y no altera el progreso de sesión más allá del último ejercicio completado.
- Regla interna de dificultad: el nivel sube tras 3 aciertos consecutivos y baja tras 2 errores consecutivos. Esta regla no se muestra al usuario.
- Los ejercicios son de opción múltiple (3 a 4 opciones, una sola correcta) o de ordenar (3 a 5 pasos).
- En ordenar no hay respuesta parcial: o el orden es completo y correcto, o es error.

### Pantallas relacionadas

Pantallas en el Figma:
- Entretenimiento (móvil y escritorio)
- Juegos (inicio) (móvil y escritorio)
- Juegos (ejercicio) (móvil)
- Juegos (respuesta correcta) (móvil)
- Juegos (respuesta casi) (móvil)
- Juegos (final) (móvil)

---

## CU-06: Consultar microcápsulas disponibles

- **Actor(es):** Persona mayor
- **Módulo:** Microcápsulas de cuidado y prevención
- **Descripción:** Permite a la persona mayor consultar el listado de microcápsulas de cuidado y prevención vigentes, publicadas por el administrador de contenidos, ver el detalle de cada una, y marcarlas como favoritas para poder releerlas más adelante. Una microcápsula nunca se elimina físicamente al perder vigencia (por ejemplo, al publicarse contenido nuevo la semana siguiente): deja de listarse como vigente, pero su registro persiste. La marca de favorita es una asociación independiente entre el usuario y la microcápsula, por lo que una microcápsula guardada sigue siendo accesible para ese usuario aunque ya no esté entre las vigentes.
- **Precondición:**
  - El usuario se encuentra autenticado en la aplicación.
  - El usuario accede a la sección Entretenimiento > Leer.
- **Postcondición:** Ante la marca de una microcápsula como favorita, queda registrada la asociación entre el usuario y la microcápsula, de forma persistente e independiente de la vigencia de esta última.

### Camino básico

1. **Actor:** Accede a la sección Entretenimiento > Leer.
2. **Sistema:** Buscar las microcápsulas publicadas. (C.A. Nº1)
3. **Sistema:** Mostrar el listado de microcápsulas en formato de tarjetas, sin opciones de filtro.
4. **Actor:** Selecciona una tarjeta.
5. **Sistema:** Mostrar el detalle ampliado de la microcápsula seleccionada.
6. **Actor:** Marca la microcápsula como favorita (opcional). (C.A. Nº2)
7. **Sistema:** Guardar la marca de favorita asociada al usuario.
8. Fin CU.

### Camino alterno Nº1: No hay microcápsulas publicadas

2. **Sistema:** No encontrar microcápsulas publicadas.
3. **Sistema:** Mostrar mensaje informando que no hay contenido disponible por el momento.
4. Fin CU.

### Camino alterno Nº2: El usuario quita una marca de favorita existente

6. **Actor:** Selecciona quitar la marca de favorita de una microcápsula ya marcada.
7. **Sistema:** Eliminar la marca de favorita asociada al usuario para esa microcápsula.
8. **Sistema:** Retornar al camino básico, paso 8.

### Reglas de negocio

- El listado principal muestra únicamente microcápsulas vigentes; no cuenta con filtros ni ordenamiento configurable por el usuario en esta etapa.
- Una microcápsula que deja de ser vigente no se elimina del sistema: solo deja de listarse en la consulta general.
- La marca de favorita es una relación independiente entre usuario y microcápsula; una microcápsula guardada permanece accesible para el usuario que la guardó, aunque haya perdido vigencia.

### Pantallas relacionadas

Pantallas en el Figma:
- Entretenimiento (móvil y escritorio)
- Leer (móvil y escritorio)

Sin crear todavía en el Figma: el detalle de una microcápsula y la sección de guardadas (con la marca de favorita).

---

## CU-07: Registrar/Editar/Eliminar recordatorio de medicación

- **Actor(es):** Persona mayor
- **Módulo:** Recordatorios
- **Descripción:** Permite a la persona mayor gestionar sus recordatorios de medicación, indicando nombre del medicamento, dosis, horario y frecuencia/duración del tratamiento (por ejemplo: "cada día, por 31 días, a las 8:00, 1 pastilla").
- **Precondición:**
  - El usuario se encuentra autenticado en la aplicación.
  - El usuario accede a la sección Recordatorios.
- **Postcondición:** Queda registrado, editado o eliminado un recordatorio de medicación asociado al usuario, con sus notificaciones programadas en consecuencia.

### Camino básico: Registrar recordatorio de medicación

1. **Actor:** Selecciona la opción Nuevo recordatorio > Medicación.
2. **Sistema:** Mostrar formulario de medicación (nombre del medicamento, dosis, hora, frecuencia, duración en días).
3. **Actor:** Completa los datos solicitados.
4. **Actor:** Confirma el registro.
5. **Sistema:** Validar los datos ingresados. (C.A. Nº1)
   - 5.1. Controlar que los campos obligatorios estén completos.
   - 5.2. Controlar que la hora sea válida y la duración sea mayor a cero.
6. **Sistema:** Guardar el recordatorio de medicación asociado al usuario.
7. **Sistema:** Programar las notificaciones correspondientes según la frecuencia y duración indicadas.
8. **Sistema:** Confirmar al usuario que el recordatorio fue creado.
9. Fin CU.

### Camino alterno Nº1: Datos inconsistentes o incompletos

5. **Sistema:** Informar que los datos ingresados son inválidos o están incompletos.
   - 5.1. Mostrar mensaje: "Revisá los datos del recordatorio e intentá nuevamente."
6. **Sistema:** Retornar al camino básico, paso 3.

### Flujo secundario: Editar recordatorio de medicación

1. **Actor:** Selecciona un recordatorio de medicación existente y elige Editar.
2. **Sistema:** Mostrar el formulario de medicación con los datos actuales cargados.
3. **Actor:** Modifica los datos deseados y confirma.
4. **Sistema:** Validar los datos ingresados (igual que en el camino básico). (C.A. Nº1)
5. **Sistema:** Actualizar el recordatorio de medicación.
6. **Sistema:** Reprogramar las notificaciones correspondientes según los nuevos datos.
7. **Sistema:** Confirmar al usuario que el recordatorio fue actualizado.
8. Fin CU.

### Flujo secundario: Eliminar recordatorio de medicación

1. **Actor:** Selecciona un recordatorio de medicación existente y elige Eliminar.
2. **Sistema:** Solicitar confirmación de la eliminación.
3. **Actor:** Confirma la eliminación.
4. **Sistema:** Eliminar el recordatorio de medicación.
5. **Sistema:** Cancelar las notificaciones programadas asociadas al recordatorio eliminado.
6. **Sistema:** Confirmar al usuario que el recordatorio fue eliminado.
7. Fin CU.

### Reglas de negocio

- Un recordatorio de medicación siempre requiere hora y frecuencia/duración; no se admite un recordatorio de medicación puntual sin repetición.
- La eliminación de un recordatorio requiere confirmación explícita del usuario antes de ejecutarse.
- Toda alta, edición o eliminación de un recordatorio recalcula automáticamente sus notificaciones asociadas (CU-09).

### Pantallas relacionadas

Pantallas en el Figma:
- Recordatorios (móvil y escritorio)
- Nuevo recordatorio (medicación) (móvil y escritorio)
- Recordatorios (calendario) (móvil y escritorio)

Sin crear todavía en el Figma: la confirmación al eliminar un recordatorio.

---

## CU-08: Registrar/Editar/Eliminar recordatorio de cita médica

- **Actor(es):** Persona mayor
- **Módulo:** Recordatorios
- **Descripción:** Permite a la persona mayor gestionar sus recordatorios de citas médicas, indicando título, especialidad, médico, lugar, fecha, hora e indicaciones previas (por ejemplo, estudios a llevar). A diferencia de los recordatorios de medicación, estos son puntuales y no se repiten.
- **Precondición:**
  - El usuario se encuentra autenticado en la aplicación.
  - El usuario accede a la sección Recordatorios.
- **Postcondición:** Queda registrado, editado o eliminado un recordatorio de cita médica asociado al usuario, con su notificación programada en consecuencia.

### Camino básico: Registrar recordatorio de cita médica

1. **Actor:** Selecciona la opción Nuevo recordatorio > Cita médica.
2. **Sistema:** Mostrar formulario de cita médica (título, especialidad, médico, lugar, fecha, hora, indicaciones previas opcionales).
3. **Actor:** Completa los datos solicitados.
4. **Actor:** Confirma el registro.
5. **Sistema:** Validar los datos ingresados. (C.A. Nº1)
   - 5.1. Controlar que los campos obligatorios estén completos.
   - 5.2. Controlar que la fecha y hora sean futuras respecto al momento actual.
6. **Sistema:** Guardar el recordatorio de cita médica asociado al usuario.
7. **Sistema:** Programar la notificación correspondiente.
8. **Sistema:** Confirmar al usuario que el recordatorio fue creado.
9. Fin CU.

### Camino alterno Nº1: Datos inconsistentes o incompletos

5. **Sistema:** Informar que los datos ingresados son inválidos o están incompletos.
   - 5.1. Mostrar mensaje: "Revisá los datos de la cita e intentá nuevamente."
6. **Sistema:** Retornar al camino básico, paso 3.

### Flujo secundario: Editar recordatorio de cita médica

1. **Actor:** Selecciona un recordatorio de cita médica existente y elige Editar.
2. **Sistema:** Mostrar el formulario de cita médica con los datos actuales cargados.
3. **Actor:** Modifica los datos deseados y confirma.
4. **Sistema:** Validar los datos ingresados (igual que en el camino básico). (C.A. Nº1)
5. **Sistema:** Actualizar el recordatorio de cita médica.
6. **Sistema:** Reprogramar la notificación correspondiente según los nuevos datos.
7. **Sistema:** Confirmar al usuario que el recordatorio fue actualizado.
8. Fin CU.

### Flujo secundario: Eliminar recordatorio de cita médica

1. **Actor:** Selecciona un recordatorio de cita médica existente y elige Eliminar.
2. **Sistema:** Solicitar confirmación de la eliminación.
3. **Actor:** Confirma la eliminación.
4. **Sistema:** Eliminar el recordatorio de cita médica.
5. **Sistema:** Cancelar la notificación programada asociada al recordatorio eliminado.
6. **Sistema:** Confirmar al usuario que el recordatorio fue eliminado.
7. Fin CU.

### Reglas de negocio

- Un recordatorio de cita médica es siempre puntual: no admite frecuencia ni repetición, a diferencia del recordatorio de medicación (CU-07).
- La fecha y hora de una cita médica deben ser siempre posteriores al momento de creación o edición del recordatorio.
- La eliminación de un recordatorio requiere confirmación explícita del usuario antes de ejecutarse.

### Pantallas relacionadas

Pantallas en el Figma:
- Recordatorios (móvil y escritorio)
- Nuevo recordatorio (cita médica) (móvil)
- Recordatorios (calendario) (móvil y escritorio)

Sin crear todavía en el Figma: el formulario de cita médica en escritorio y la confirmación al eliminar un recordatorio.

---

## CU-09: Recibir aviso de recordatorio

- **Actor(es):** Persona mayor, Sistema
- **Módulo:** Recordatorios
- **Descripción:** Describe el procedimiento mediante el cual el sistema avisa a la persona mayor sobre sus recordatorios programados (ya sean de medicación o de citas médicas). Esto se realiza mediante alertas dentro de la aplicación al iniciar sesión y presentando los recordatorios del día agrupados en el saludo inicial de Alma (CU-02). Cuando el usuario recibe un aviso de medicación, puede marcar la dosis como "Tomada" mediante un botón. Si el aviso es de cita médica, al tocarlo es redirigido al detalle de la cita y esta se marca automáticamente como "vista".
- **Precondición:** Existe al menos un recordatorio (de medicación o de cita médica) programado para el usuario en el momento evaluado.
- **Postcondición:** El usuario fue avisado del recordatorio correspondiente, mediante su presentación agrupada al ingresar a la aplicación o mientras se encuentra con la aplicación abierta.

### Camino básico

1. **Sistema:** Verificar periódicamente los recordatorios programados para el momento actual (proceso automático del sistema).
2. **Sistema:** Encontrar uno o más recordatorios cuya fecha/hora programada coincide con el momento actual. (C.A. Nº2)
3. **Sistema:** Agrupar los recordatorios encontrados si corresponden a la misma franja horaria.
4. **Sistema:** Mostrar el aviso/notificación en pantalla o incluir los recordatorios del día agrupados como parte del saludo inicial de Alma (CU-02).
5. **Actor:** Toca el aviso o interactúa con él. (C.A. Nº1)
6. **Sistema:** Evaluar el tipo de recordatorio notificado.
   - 6.1. Si es de medicación: permitir al usuario presionar el botón "Tomada" para registrar la toma.
   - 6.2. Si es de cita médica: redirigir al usuario a la pantalla de detalle de la cita y marcar la notificación como "vista".
7. **Sistema:** Actualizar el estado del aviso/recordatorio en el sistema según la acción realizada.
8. Fin CU.

### Camino alterno Nº1: El usuario no interactúa con la notificación en el momento

5. **Actor:** Cierra el aviso mostrado en pantalla sin realizar acciones adicionales.
6. **Sistema:** Informar al usuario que el recordatorio continuará disponible para recordárselo más tarde y mantenerlo programado.
7. Fin CU.

### Camino alterno Nº2: No existen recordatorios para el momento evaluado

2. **Sistema:** No encontrar recordatorios programados para el momento actual.
3. **Sistema:** Fin CU (no se genera notificación).

### Reglas de negocio

- Dos o más recordatorios de la misma franja horaria siempre se agrupan en una única notificación y en una única mención dentro del saludo de Alma.
- Un recordatorio no notificado en su momento por falta de interacción del usuario no se pierde: queda disponible para el próximo ingreso a la aplicación.

### Pantallas relacionadas

Pantallas en el Figma:
- Chat (inicio) (móvil y escritorio)
- Recordatorios (móvil y escritorio)

Sin crear todavía en el Figma: el detalle de la cita que se abre al tocar el aviso.

---

## CU-10: Gestionar catálogo de ejercicios

- **Actor(es):** Administrador de contenidos
- **Módulo:** Panel de operador (backoffice)
- **Descripción:** Permite al administrador de contenidos dar de alta, editar o eliminar ejercicios del catálogo de estimulación cognitiva utilizado por el módulo de Juegos (CU-05).
- **Precondición:** El administrador se encuentra autenticado en el panel de operador (CU-12).
- **Postcondición:** Queda registrado, editado o eliminado un ejercicio en el catálogo utilizado por CU-05.

### Camino básico: Dar de alta un ejercicio

1. **Actor:** Accede a Panel de operador > Ejercicios > Nuevo.
2. **Sistema:** Mostrar formulario de ejercicio (categoría, formato, consigna, nivel de dificultad, explicación y, si corresponde, texto a memorizar). El formulario de opciones cambia según el formato: en opción múltiple se marca cuál es la correcta, y en ordenar se indica la posición de cada paso. Cada opción admite una imagen opcional.
3. **Actor:** Completa los datos del ejercicio.
4. **Actor:** Confirma el alta.
5. **Sistema:** Validar que los campos obligatorios estén completos. (C.A. Nº1, C.A. Nº2)
6. **Sistema:** Guardar el ejercicio en el catálogo.
7. **Sistema:** Confirmar al administrador que el ejercicio fue creado.
8. Fin CU.

### Camino alterno Nº1: Datos incompletos

5. **Sistema:** Informar que existen campos obligatorios sin completar.
   - 5.1. Mostrar mensaje: "Completá todos los campos requeridos del ejercicio."
6. **Sistema:** Retornar al camino básico, paso 3.

### Camino alterno Nº2: Opciones inválidas

5. **Sistema:** Informar que las opciones no cumplen el formato elegido.
   - Mensaje para opción múltiple: "Carga de 3 a 4 opciones y marcá una sola como correcta."
   - Mensaje para ordenar: "Carga de 3 a 5 pasos y asígnale a cada uno una posición distinta."
6. **Sistema:** Retornar al camino básico, paso 3.

### Flujo secundario: Editar ejercicio

1. **Actor:** Selecciona un ejercicio existente del catálogo y elige Editar.
2. **Sistema:** Mostrar el formulario de ejercicio con los datos actuales cargados.
3. **Actor:** Modifica los datos deseados y confirma.
4. **Sistema:** Validar los datos ingresados (igual que en el camino básico). (C.A. Nº1, C.A. Nº2)
5. **Sistema:** Actualizar el ejercicio en el catálogo.
6. **Sistema:** Confirmar al administrador que el ejercicio fue actualizado.
7. Fin CU.

### Flujo secundario: Eliminar ejercicio

1. **Actor:** Selecciona un ejercicio existente del catálogo y elige Eliminar.
2. **Sistema:** Solicitar confirmación de la eliminación.
3. **Actor:** Confirma la eliminación.
4. **Sistema:** Eliminar el ejercicio del catálogo.
5. **Sistema:** Confirmar al administrador que el ejercicio fue eliminado.
6. Fin CU.

### Reglas de negocio

- Un ejercicio eliminado del catálogo deja de estar disponible para ser seleccionado por CU-05, sin afectar los resultados ya registrados de partidas anteriores.
- El nivel de dificultad de un ejercicio es un dato de catálogo, independiente del nivel de dificultad individual alcanzado por cada usuario en CU-05.
- El texto de cada opción es obligatorio aunque tenga imagen, porque sirve de descripción accesible.
- Cambiar el formato de un ejercicio existente obliga a revisar sus opciones.

### Pantallas relacionadas

Pantallas en el Figma:
- Operador · Panel (escritorio)
- Operador · Ejercicios (escritorio)
- Operador · Ejercicio nuevo (escritorio)
- Operador · Ejercicio nuevo (error) (escritorio)
- Operador · Ejercicios (confirmar eliminar) (escritorio)

---

## CU-11: Gestionar microcápsula de información

- **Actor(es):** Administrador de contenidos
- **Módulo:** Panel de operador (backoffice)
- **Descripción:** Permite al administrador de contenidos crear, editar o dar de baja microcápsulas de cuidado y prevención, consultadas por la persona mayor en CU-06. La publicación es directa al guardar, sin manejo de estados de borrador.
- **Precondición:** El administrador se encuentra autenticado en el panel de operador (CU-12).
- **Postcondición:** Queda creada, editada o dada de baja una microcápsula. Al darse de baja, deja de figurar en el listado general de CU-06 pero sigue accesible para los usuarios que la marcaron como favorita.

### Camino básico: Crear una microcápsula

1. **Actor:** Accede a Panel de operador > Microcápsulas > Nueva.
2. **Sistema:** Mostrar formulario de microcápsula (título, texto, categoría, imagen, fecha de publicación).
3. **Actor:** Completa los datos de la microcápsula.
4. **Actor:** Confirma la creación.
5. **Sistema:** Validar que los campos obligatorios estén completos. (C.A. Nº1)
6. **Sistema:** Guardar y publicar la microcápsula de forma inmediata.
7. **Sistema:** Confirmar al administrador que la microcápsula fue publicada.
8. Fin CU.

### Camino alterno Nº1: Datos incompletos

5. **Sistema:** Informar que existen campos obligatorios sin completar.
   - 5.1. Mostrar mensaje: "Completá todos los campos requeridos de la microcápsula."
6. **Sistema:** Retornar al camino básico, paso 3.

### Flujo secundario: Editar microcápsula

1. **Actor:** Selecciona una microcápsula existente y elige Editar.
2. **Sistema:** Mostrar el formulario de microcápsula con los datos actuales cargados.
3. **Actor:** Modifica los datos deseados y confirma.
4. **Sistema:** Validar los datos ingresados (igual que en el camino básico). (C.A. Nº1)
5. **Sistema:** Actualizar la microcápsula publicada.
6. **Sistema:** Confirmar al administrador que la microcápsula fue actualizada.
7. Fin CU.

### Flujo secundario: Dar de baja microcápsula

1. **Actor:** Selecciona una microcápsula existente y elige Dar de baja.
2. **Sistema:** Solicitar confirmación de la baja, informando: "Los usuarios que la guardaron como favorita seguirán viéndola."
3. **Actor:** Confirma la eliminación.
4. **Sistema:** Marcar la microcápsula como dada de baja (registrando la fecha de baja). No se elimina físicamente.
5. **Sistema:** Retirar la microcápsula del listado general de CU-06.
6. **Sistema:** Confirmar al administrador que la microcápsula fue dada de baja.
7. Fin CU.

### Reglas de negocio

- No existe estado de borrador: toda microcápsula guardada queda publicada de inmediato y visible en CU-06.
- La baja de una microcápsula es lógica: no elimina las marcas de favorita de los usuarios. Las microcápsulas dadas de baja permanecen accesibles desde la sección de guardadas de quienes las marcaron.
- Una microcápsula dada de baja no puede ser marcada como favorita por nuevos usuarios, ya que no figura en el listado general.

### Pantallas relacionadas

Pantallas en el Figma:
- Operador · Panel (escritorio)
- Operador · Microcápsulas (escritorio)
- Operador · Microcápsula nueva (escritorio)
- Operador · Microcápsula nueva (error) (escritorio)
- Operador · Microcápsulas (confirmar baja) (escritorio)

---

## CU-12: Autenticarse en el panel de operador

- **Actor(es):** Administrador de contenidos
- **Módulo:** Panel de operador (backoffice)
- **Descripción:** Permite al administrador de contenidos iniciar sesión en el panel de operador mediante usuario y contraseña, de forma simple, para poder acceder a la gestión de ejercicios (CU-10) y microcápsulas (CU-11).
- **Precondición:** El administrador posee credenciales (usuario y contraseña) válidas, previamente dadas de alta.
- **Postcondición:** Queda iniciada una sesión activa del administrador en el panel de operador.

### Camino básico

1. **Actor:** Accede a la pantalla de inicio de sesión del panel de operador.
2. **Sistema:** Mostrar formulario de inicio de sesión (usuario, contraseña).
3. **Actor:** Ingresa usuario y contraseña.
4. **Actor:** Confirma el inicio de sesión.
5. **Sistema:** Validar credenciales. (C.A. Nº1)
   - 5.1. Buscar instancia de administrador con el usuario ingresado.
   - 5.2. Comprobar que la contraseña ingresada coincida con la registrada.
6. **Sistema:** Iniciar la sesión del administrador.
7. **Sistema:** Redirigir al panel principal de operador.
8. Fin CU.

### Camino alterno Nº1: Credenciales inválidas

5. **Sistema:** No encontrar coincidencia entre el usuario y/o la contraseña ingresados y los datos registrados.
   - 5.1. Mostrar mensaje: "Usuario o contraseña incorrectos. Intentá nuevamente."
6. **Sistema:** Retornar al camino básico, paso 3.

### Reglas de negocio

- El acceso al panel de operador es exclusivo para usuarios con rol de administrador de contenidos.
- No se contempla, en esta etapa, recuperación de contraseña ni manejo de múltiples roles.

### Pantallas relacionadas

Pantallas en el Figma:
- Login (móvil y escritorio)
- Login (error) (móvil y escritorio)
- Operador · Panel (escritorio)

---

## CU-13: Ajustar configuración de accesibilidad

- **Actor(es):** Persona mayor
- **Módulo:** Accesibilidad (transversal)
- **Descripción:** Permite a la persona mayor ajustar el tamaño de letra y activar el modo de alto contraste desde un botón flotante accesible en toda la aplicación y desde la sección "Perfil". Las preferencias configuradas se guardan de forma persistente por usuario.
- **Precondición:** El usuario se encuentra autenticado en la aplicación.
- **Postcondición:** Las preferencias de accesibilidad configuradas (tamaño de letra, alto contraste) quedan guardadas de forma persistente, asociadas al usuario, y se aplican en todos sus futuros ingresos.

### Camino básico

1. **Actor:** Toca el botón flotante de accesibilidad, disponible en cualquier pantalla.
2. **Sistema:** Mostrar el panel de opciones de accesibilidad (tamaño de letra, alto contraste).
3. **Actor:** Ajusta el tamaño de letra y/o activa el modo de alto contraste.
4. **Sistema:** Aplicar los cambios de forma inmediata sobre la interfaz.
5. **Actor:** Cierra el panel de accesibilidad.
6. **Sistema:** Guardar las preferencias configuradas, asociadas al usuario, de forma persistente.
7. Fin CU.

### Camino alterno Nº1: El usuario restablece los valores por defecto

3. **Actor:** Selecciona la opción de restablecer valores por defecto.
4. **Sistema:** Aplicar el tamaño de letra y contraste estándar de la aplicación.
5. **Sistema:** Retornar al camino básico, paso 5.

### Reglas de negocio

- Las preferencias de accesibilidad son individuales por usuario y se aplican automáticamente en cada ingreso, sin necesidad de reconfigurarlas.
- El botón flotante de accesibilidad se mantiene visible y en la misma posición en todas las pantallas de la aplicación.

### Pantallas relacionadas

Pantallas en el Figma:
- Accesibilidad (móvil y escritorio)
- Perfil (móvil y escritorio) (entrada "Tamaño de letra y contraste")

---

## CU-14: Iniciar y cerrar sesión (persona mayor)

- **Actor(es):** Persona mayor
- **Módulo:** Autenticación (transversal)
- **Descripción:** Permite a la persona mayor ingresar a la aplicación con usuario y contraseña, mantener la sesión iniciada para no volver a loguearse en cada ingreso, y cerrarla cuando lo desee. Los perfiles se dan de alta previamente (dataset sintético); no existe registro propio en este alcance.
- **Precondición:** La persona posee credenciales previamente dadas de alta.
- **Postcondición:** Queda iniciada (o cerrada) una sesión activa del usuario, y se aplican sus preferencias de accesibilidad (CU-13).

### Camino básico

1. **Actor:** Abre la aplicación.
2. **Sistema:** Verificar si existe una sesión vigente. (C.A. Nº2)
3. **Sistema:** Mostrar la pantalla de inicio de sesión (usuario, contraseña, opción de mostrar contraseña, casilla "Mantener sesión iniciada").
4. **Actor:** Ingresa usuario y contraseña, y confirma.
5. **Sistema:** Validar credenciales. (C.A. Nº1)
6. **Sistema:** Iniciar la sesión y cargar las preferencias de accesibilidad del usuario (CU-13).
7. **Sistema:** Redirigir a la pantalla principal (CU-02).
8. Fin CU.

### Camino alterno Nº1: Credenciales inválidas

5. **Sistema:** No encuentra coincidencia, muestra "Usuario o contraseña incorrectos. Intenta nuevamente." y vuelve al paso 4.

### Camino alterno Nº2: Sesión vigente

2. **Sistema:** Detecta una sesión iniciada previamente, omite el login y continúa en el paso 6.

### Camino alterno Nº3: Cerrar sesión

1. **Actor:** Selecciona "Cerrar sesión".
2. **Sistema:** Solicita confirmación.
3. **Actor:** Confirma.
4. **Sistema:** Invalida la sesión y redirige al login.
5. Fin CU.

### Reglas de negocio

- La sesión es persistente por defecto, para minimizar la fricción con adultos mayores.
- No hay registro de usuarios ni recuperación de contraseña en este alcance.
- Se utiliza una única tabla de usuarios con campo rol (PERSONA_MAYOR / ADMINISTRADOR) y un único mecanismo de autenticación (compartido con CU-12).
- Los mensajes de error no indican si falló el usuario o la contraseña.
- En un despliegue integrado a OSEP, la autenticación la provee el sistema de identidad institucional; este login es un mecanismo de demostración que emite el mismo tipo de token.

### Pantallas relacionadas

Pantallas en el Figma:
- Login (móvil y escritorio)
- Login (error) (móvil y escritorio)
- Perfil (móvil y escritorio)
- Perfil (confirmar cerrar sesión) (móvil y escritorio)

---

## Estado de implementación

Se actualiza al terminar cada funcionalidad, del lado del back y del front.

| CU | Back | Front |
|---|---|---|
| CU-01 | Pendiente | Pendiente |
| CU-02 | Pendiente | Pendiente |
| CU-03 | Pendiente | Pendiente |
| CU-04 | Pendiente | Pendiente |
| CU-05 | Pendiente | Pendiente |
| CU-06 | Pendiente | Pendiente |
| CU-07 | Hecho: alta, edición, baja y consulta de medicaciones con sus notificaciones (`/api/recordatorios/medicacion`, `GET` y `DELETE /api/recordatorios/{id}`), solo `PERSONA_MAYOR`; las pruebas de integración están escritas pero todavía no se corrieron contra la base | Hecho: alta y edición de medicaciones (`/recordatorios/nuevo` y `/recordatorios/:id/editar`) con nombre, dosis, hora, frecuencia (botones de opción, "Todos los días" preseleccionado) y duración de 1 a 365 días con − y +; errores junto a cada campo y aviso arriba si la API responde 400; eliminación con confirmación ("¿Querés eliminar este recordatorio?") desde la pantalla de edición; aviso flotante de éxito ("Tu recordatorio quedó creado", "actualizado" o "eliminado"). Se ven en la pestaña Lista ("Medicación de hoy" y "Mis medicaciones") y en el Calendario, con "Ya la tomé" y "Deshacer" solo en el día de hoy. **Diferencias con el Figma:** frecuencia, "Mis medicaciones" y tarjetas nuevas (contrato de recordatorios, secciones 10.1 y 12). La hora usa el campo nativo del navegador. No hay pruebas automáticas del front |
| CU-08 | Hecho: alta, edición, baja y consulta de citas con su notificación (`/api/recordatorios/citas`); las pruebas de integración están escritas pero todavía no se corrieron contra la base | Hecho: alta y edición de citas con título, especialidad, médico, lugar, fecha, hora e indicaciones previas (opcional), con errores junto a cada campo y aviso arriba si la API responde 400; detalle de la cita (`/recordatorios/:id`) con "Editar" y "Eliminar" (con confirmación); las citas se ven en "Citas médicas" de la Lista y en el Calendario (punto en el día). La fecha y la hora usan campos nativos del navegador (formato y aspecto según el navegador). **Diferencia con el Figma:** el formulario y el detalle de la cita no estaban diseñados; se armaron con los mismos componentes que los de medicación. No hay pruebas automáticas del front |
| CU-09 | Parcial: `GET /api/recordatorios/agenda`, `PUT /api/recordatorios/notificaciones/{id}/estado` (tomada, vista, deshacer) y `RecordatorioService.agenda(...)` para el módulo de Chat; falta el saludo de Alma (CU-02) | Parcial: tarjeta "Para hoy" (`TarjetaParaHoy`) que avisa de la primera notificación pendiente del día y agrupa las de la misma hora; "Ya la tomé" para una medicación y, para una cita, la tarjeta entera lleva a su detalle, que la marca como vista; la X la oculta solo en memoria. La agenda de hoy se vuelve a pedir cada 60 segundos y al volver a la ventana. La Lista y el Calendario muestran los estados "Pendiente", "Tomada a las…", "Pasó hace…" y "Vista" con texto e ícono. Falta ubicar la tarjeta en el saludo de Alma (módulo de Chat, CU-02): hoy se monta de forma provisoria en el Inicio |
| CU-10 | Pendiente | Pendiente |
| CU-11 | Pendiente | Pendiente |
| CU-12 | Hecho: `POST /api/auth/login` con rol `ADMINISTRADOR` y `/api/admin/**` reservado a ese rol (todavía no hay endpoints de administración) | Hecho: login compartido y redirección a `/admin`; el panel de operador es una pantalla provisoria con un "Salir" temporal, sin confirmación |
| CU-13 | Hecho: `GET` y `PUT /api/accesibilidad` solo para `PERSONA_MAYOR`, y las preferencias viajan en el login y en `/api/auth/me`; faltan pruebas automáticas de esos dos endpoints | Hecho: panel (tamaño de letra, alto contraste, "Volver a lo original") montado una sola vez en el layout de la persona mayor y abierto desde el botón de accesibilidad (flotante arriba a la derecha en celular; botón "Accesibilidad" al pie del menú lateral en escritorio) y desde la fila "Tamaño de letra y contraste" de Perfil; se guardan en el dispositivo y con `PUT`. **Diferencia con el CU-13:** en celular, la pantalla Perfil NO muestra el botón flotante, porque ya tiene la fila "Tamaño de letra y contraste" que abre el mismo panel (en escritorio el botón del menú está siempre). En el Login el botón sigue flotante (abajo a la derecha en escritorio), distinto del menú lateral de las demás pantallas en escritorio |
| CU-14 | Hecho: `POST /api/auth/login`, `GET /api/auth/me` y `GET /api/perfil` (datos de la pantalla Perfil); no hay endpoint para cerrar sesión (el contrato lo resuelve en el front) | Hecho para la persona mayor: login con "Mostrar contraseña" y "Mantener sesión iniciada", recuperación de la sesión con `/api/auth/me`, redirección por rol, pantalla Perfil y confirmación "¿Querés cerrar sesión?" (`DialogoCerrarSesion`). Falta conectar ese diálogo en el panel de operador, que sigue con el "Salir" provisorio y sin confirmación |

Clases del diagrama ya creadas en el back: `Cuenta`, `PersonaMayor`, `Administrador`, `PreferenciaAccesibilidad`, `Recordatorio`, `RecordatorioMedicacion`, `RecordatorioCita`, `NotificacionRecordatorio` y los enums `Rol`, `TamanioLetra`, `FrecuenciaToma` y `EstadoNotificacion`.
