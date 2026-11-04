# Contrato de Registro de ánimo · Alma Acompaña

**Estado:** aprobado. Es la referencia única para el back y el front de `feature/animo`. Si algo cambia, se cambia primero acá.

**Fuente de verdad:** caso de uso CU-01 y diagrama de clases (clase `RegistroAnimo`, enum `NivelAnimo`). También se tuvieron en cuenta CU-02 (el widget vive dentro del chat), el documento de guardrails (sección 5.3, contexto controlado, y 5.4, privacidad) y la arquitectura (módulo Bienestar, PostgreSQL). Si este contrato contradice esos documentos, ganan los documentos y se corrige el contrato. El Figma es ilustrativo (ver 11.6).

**Dónde va:** `docs/contrato-animo.md`. El `CLAUDE.md` debe enlazarlo en la sección "Documentos".

**Última revisión contra el repo:** commit `321a63f` (merge de `feature/recordatorios`, 29/10/2026).

**Contratos relacionados:** `docs/contrato-login.md` (token, roles, errores, convenciones) y `docs/contrato-recordatorios.md` (mismo patrón de módulo). Todo lo que no se redefine acá sigue como está ahí.

**Responsable:** Paula (módulo Bienestar). Rama: `feature/animo`. La rama siguiente, `feature/chat` (CU-02), parte de esta ya integrada en `main`.

---

## 1. Alcance

**Incluye:** registrar un estado de ánimo (escala de 5 niveles); corregir el último registro con "Cambiar cómo me siento"; consultar el último ánimo del día; la pregunta contextual que Alma muestra al elegir un nivel; el widget del front, la burbuja que muestra lo que la persona eligió y su montaje provisorio en el Inicio; un método de servicio que el Chat puede usar; mensajes de error; pruebas.

**No incluye:**
- El chat, el saludo de Alma y la conversación libre: son de `feature/chat` (CU-02). El widget queda listo y exportado para que el Chat lo monte debajo del saludo.
- Usar el modelo de lenguaje: la pregunta contextual es un texto fijo por nivel (sección 2).
- Historial, gráficos o estadísticas de ánimo, borrar un registro y modificar registros que no sean el ánimo de hoy.
- Alertas, derivaciones o avisos a terceros según el nivel elegido (sección 5.5).
- Notificaciones o recordatorios para que la persona registre su ánimo.

---

## 2. Decisiones

| Tema | Decisión | Motivo |
|---|---|---|
| Escala | Enum `NivelAnimo`: `MUY_MAL`, `MAL`, `NEUTRO`, `BIEN`, `MUY_BIEN` (de menos a más). CU-01 habla de "escala de 1 a 5": `MUY_MAL` = 1 y `MUY_BIEN` = 5. La API usa el enum, no el número | DC y convención de la API (enums en `MAYUSCULAS_CON_GUION_BAJO`) |
| Registrar y corregir | Cuando el widget aparece (cada ingreso a la app) y la persona elige un emoji, se crea un registro nuevo. Si después toca "Cambiar cómo me siento" y elige otro, **se modifica ese mismo registro**: es una corrección dentro del mismo momento, no un registro nuevo. Nunca se borra ninguno | CU-01: cada registro es un momento, sin límite por día y con su timestamp. Un nuevo ingreso es otro momento (registro nuevo); "cambiar" corrige el que acaba de hacer y evita registros duplicados por un toque equivocado |
| Qué se puede modificar | Solo el ánimo de hoy (el más reciente del día, 5.2). Al modificarlo cambian `nivelAnimo` y `fechaHora` (la del momento de la corrección, que pone el back); el `id` se mantiene. El cuerpo del `PUT` lleva solo `nivelAnimo` | Ningún registro anterior se toca. El registro representa el nivel elegido y el momento en que se eligió: si la persona cambia de ánimo más tarde en la misma sesión, la fecha y hora lo reflejan |
| Fecha y hora | `fechaHora` es **`LocalDateTime`** (el DC nombra el campo como `Date`; se implementa igual como `LocalDateTime`). La pone el back con el `Clock` de la app (zona de Argentina) y nunca viene del front | CU-01 pide "fecha/hora" y "timestamp", y hay que ordenar varios registros del mismo día. Convención del proyecto |
| Pregunta contextual | La arma el **back** y viaja en la respuesta del `POST` y del `PUT`. Es un texto fijo por nivel, sin modelo de lenguaje | CU-01 paso 3 la asigna al "Sistema". Así CU-01 no depende de Ollama, responde al instante y funciona aunque el modelo no esté. Los textos están en la sección 6 |
| Quién oculta el widget | El **componente padre** (hoy el Inicio provisorio; después el Chat). El widget avisa con `onRegistrado` y no se oculta solo | CU-01 pasos 4 y 5: el widget se oculta y el control vuelve a CU-02 |
| Qué queda en pantalla al elegir | El widget se reemplaza por una **burbuja de la persona** con la carita elegida y su etiqueta, y debajo la pregunta de Alma. No es un mensaje escrito por la persona ni se guarda: es solo la forma de mostrar lo que eligió | Figma, marco "después de elegir ánimo"; CU-01 paso 5 |
| Hasta cuándo se puede cambiar | Hasta que la persona manda su primer mensaje a Alma. Entonces desaparece "Cambiar cómo me siento" y la burbuja queda como parte de la conversación. Lo decide el front (el padre), sin temporizador; el back no lo sabe y solo exige que el registro sea el ánimo de hoy | El chat no guarda historial (CU-02): no hay forma ni motivo de que el back sepa si ya hubo mensajes |
| "Ánimo de hoy" | El registro más reciente del día calendario de Argentina (desde las 00:00 inclusive hasta las 00:00 del día siguiente). Si hay empate de hora, el de mayor id | CU-01, regla de negocio 3 |
| Endpoints | `POST /api/animo`, `PUT /api/animo/{id}` y `GET /api/animo/hoy`, todos solo `PERSONA_MAYOR`. La persona sale del token | Contrato de login, sección 2 (identidad) |
| `GET /api/animo/hoy` | Sirve para verificar con Swagger que el `POST` guardó, y para quien integre la API (OSEP). **Este front no lo usa**, y el Chat tampoco: lee el ánimo por servicio (sección 10) | Consulta chica; evita que el ánimo quede como un dato que solo se escribe |
| Paquete del back | `bienestar/` (así lo nombra el `CLAUDE.md` y la arquitectura) | Un paquete por módulo |
| Carpeta del front | `features/bienestar/` (plana, con `index.ts`) | Módulo chico; el Chat importa varias cosas |
| Nada se guarda en el navegador | El estado del widget (visible u oculto, qué eligió y el `id` del registro) vive solo en memoria. Al recargar se pierde, y la próxima elección crea un registro nuevo | El chat es efímero (CU-02): cada ingreso empieza con un saludo y el widget de nuevo |

---

## 3. Convenciones de la API

Valen las del contrato de login (sección 3) y las del de recordatorios (sección 3). Para este módulo:

- Todo cuelga de `/api/animo`.
- Fecha y hora: `"2026-11-03T12:30:00"`, sin zona ni desplazamiento.
- El front muestra lo que llega y no convierte zonas.
- Enum `NivelAnimo` en `MAYUSCULAS_CON_GUION_BAJO`.

---

## 4. Modelo de datos

Según el DC. No se agregan campos.

`RegistroAnimo`:

| Campo | Tipo | Notas |
|---|---|---|
| `id` | `Long` | Generado (`IDENTITY`) |
| `nivelAnimo` | `NivelAnimo` | `@Enumerated(EnumType.STRING)`, obligatorio |
| `fechaHora` | `LocalDateTime` | Obligatoria, la completa el back |
| `personaMayor` | `PersonaMayor` | Muchos a uno, obligatoria (DC, relación 6) |

Recomendado (no es un dato nuevo, es una restricción de base de datos): índice sobre (`persona_mayor_id`, `fecha_hora`), porque la consulta del ánimo de hoy filtra por persona y rango de fechas.

No hay restricción de unicidad: se pueden registrar varios ánimos con el mismo nivel y en el mismo minuto.

---

## 5. Reglas de negocio

### 5.1 Registrar

1. Se busca la `PersonaMayor` de la cuenta del token. Si no existe, 404.
2. Se crea el `RegistroAnimo` con el nivel pedido y `fechaHora` = ahora (hora de Argentina).
3. Se responde con el registro y la pregunta contextual de ese nivel (sección 6).

### 5.2 Ánimo de hoy

- Es el registro más reciente de la persona dentro del día de hoy en Argentina (se calcula solo con `LocalDate.now(clock)` / `LocalDateTime.now(clock)` del `Clock` inyectado; no se usa `Fechas.hoy()` ni `LocalDate.now()` sin zona, para que las pruebas con `Clock` fijo funcionen).
- Si no hay registros hoy, `GET /api/animo/hoy` responde 204 sin cuerpo (no es un error).
- Un registro de ayer no cuenta como ánimo de hoy, aunque haya sido a las 23:59.

### 5.3 Qué hace el front al elegir (CU-01 camino básico)

1. La persona toca una carita. No hay botón "Confirmar" ni campo de comentario.
2. El front llama a `POST /api/animo` (o a `PUT /api/animo/{id}` si está corrigiendo, 5.6).
3. Con la respuesta, avisa al padre (`onRegistrado`) con el registro, que incluye la pregunta contextual.
4. El padre oculta el widget y muestra, en este orden: la burbuja de la persona con la carita elegida (11.2), la pregunta como mensaje de Alma y el enlace "Cambiar cómo me siento". Guarda el `id` del registro en memoria.

### 5.4 Si la persona no elige (CU-01 camino alterno 1)

El widget se queda visible mientras no se elija nada, y el chat libre sigue habilitado sin pregunta contextual. No se guarda nada.

### 5.5 Sin alertas ni monitoreo

Elegir `MUY_MAL` o `MAL` **no** dispara la respuesta predefinida de malestar emocional grave, no genera alertas y no avisa a nadie. Esa derivación es solo por las listas de expresiones del chat (guardrails, sección 5.2), y el sistema no monitorea ni clasifica a la persona a partir de sus registros (guardrails, sección 5.4). La pregunta contextual de los niveles bajos es cálida y no profundiza (sección 6).

### 5.6 Cambiar cómo me siento (Figma)

Después de elegir, debajo de la pregunta de Alma, la persona ve el enlace "Cambiar cómo me siento". Funciona así:

1. Al tocarlo, el enlace se oculta y el widget vuelve a mostrarse.
2. La nueva elección **modifica el registro recién creado** (`PUT /api/animo/{id}`, 7.2): cambia su `nivelAnimo` y su `fechaHora` (se actualiza a ahora, hora de Argentina) y se mantiene su `id`.
3. El widget se oculta de nuevo. La burbuja de la persona y la pregunta de Alma **se reemplazan en el mismo lugar** por las del nivel nuevo: no se agregan mensajes nuevos al chat.
4. Esto no crea un registro nuevo. La cantidad de registros del día no cambia.
5. Si la persona toca el enlace pero no elige y manda un mensaje, el widget se cierra y queda el registro anterior.

**Hasta cuándo:** el enlace (y el widget de cambio) desaparecen cuando la persona manda su primer mensaje a Alma. Desde ahí no puede cambiar el ánimo desde la pantalla, y la burbuja queda fija. Esa regla es del front (la aplica el Chat; ver 11.4): el back no la sabe y solo valida que el registro sea el ánimo de hoy.

Si la persona recarga la página o vuelve a ingresar, el widget aparece de nuevo sin `id` y la primera elección crea un registro nuevo (5.1): ese es otro momento. Elegir el mismo nivel que ya tenía no cambia nada (tampoco la `fechaHora`) y responde igual con 200.

### 5.7 Validaciones

| Campo | Regla | Mensaje |
|---|---|---|
| `nivelAnimo` | Obligatorio y uno de los cinco valores del enum (en el `POST` y en el `PUT`) | "Elegí cómo te sentís." |

Un valor que no es del enum, o un cuerpo ilegible, responde 400 con el texto general "Revisá los datos ingresados." (sin lista `errores`), igual que las fechas inválidas en recordatorios. `GlobalExceptionHandler` ya traduce a ese texto los errores de lectura del cuerpo (JSON mal formado, valor que no es del enum), así que no hace falta tocarlo.

---

## 6. Pregunta contextual

Un texto fijo por nivel, en `bienestar/MensajesAnimo`. Sigue los criterios de conversación de los guardrails (sección 2): voseo, cálido, simple, **una sola pregunta**, sin diagnosticar ni aconsejar sobre salud, sin infantilizar y sin dar por hecho cómo es la persona (no usar género: ni "contenta" ni "contento").

**Textos:**

| Nivel | Pregunta contextual |
|---|---|
| `MUY_MAL` | "Gracias por contármelo. Estoy acá para acompañarte. ¿Querés charlar un ratito?" |
| `MAL` | "Qué lástima que hoy no sea un buen día. Estoy acá para escucharte. ¿Querés contarme cómo viene tu día?" |
| `NEUTRO` | "Gracias por contarme. A veces los días son así, tranquilos. ¿Qué estuviste haciendo hoy?" |
| `BIEN` | "¡Qué bueno saberlo! ¿Qué fue lo mejor de tu día hasta ahora?" |
| `MUY_BIEN` | "¡Qué alegría! Me encanta saberlo. ¿Qué hizo que hoy sea un día tan lindo?" |

Los textos de los niveles bajos no profundizan en el malestar y no ofrecen ayuda ni teléfonos: eso corresponde a la respuesta predefinida del chat, que se activa solo por lo que la persona escribe.

---

## 7. Endpoints

Todos exigen sesión de `PERSONA_MAYOR`. La persona sale del token (`sub`), nunca de la URL ni del cuerpo.

### 7.1 `POST /api/animo`

Registra un estado de ánimo (5.1).

**Request**
```json
{ "nivelAnimo": "BIEN" }
```

**201 Created**
```json
{
  "id": 14,
  "nivelAnimo": "BIEN",
  "fechaHora": "2026-11-03T12:30:00",
  "preguntaContextual": "¡Qué bueno saberlo! ¿Qué fue lo mejor de tu día hasta ahora?"
}
```

**Errores:** `400` si falta `nivelAnimo` ("Elegí cómo te sentís.") o no es un valor válido; `401`, `403` y `404` como en el resto de `/api` (404 si la cuenta no tiene `PersonaMayor`).

### 7.2 `PUT /api/animo/{id}`

Corrige el registro de ánimo de hoy (5.6). `{id}` es el `id` que devolvió el `POST`.

**Request**
```json
{ "nivelAnimo": "MAL" }
```

**200 OK:** el registro actualizado, con la misma forma que 7.1: el mismo `id`, el `nivelAnimo` nuevo, la `fechaHora` de la corrección y su `preguntaContextual`.

**Errores:** `400` por validación (5.7); `401` y `403` como en el resto de `/api`; `404` si el registro no existe, no es de la persona del token o no es su ánimo de hoy (es decir, es de otro día o ya hay uno más reciente).

### 7.3 `GET /api/animo/hoy`

El último ánimo registrado hoy (5.2).

**200 OK:** el mismo objeto que 7.1 (incluye `preguntaContextual`).

**204 No Content:** la persona todavía no registró su ánimo hoy.

**Errores:** `401`, `403`, `404` como en el resto de `/api`.

---

## 8. Reglas de acceso

| Ruta | Quién puede |
|---|---|
| `/api/animo/**` | Solo `PERSONA_MAYOR`. Un `ADMINISTRADOR` recibe `403` |

Sin token o con token vencido: `401`.

---

## 9. Errores y mensajes

Formato Problem Details y textos generales: los de `common/MensajesError` (contrato de login, sección 7). No se repiten.

**Back:** los textos del módulo (el de 5.7 y las cinco preguntas de la sección 6) van en `bienestar/MensajesAnimo`; el DTO la referencia (`message = MensajesAnimo.NIVEL_OBLIGATORIO`).

**Front:** los textos del módulo van en `features/bienestar/mensajes.ts`. Los generales ya están en `lib/mensajes.ts`, y el texto de un error de la API sale de `mensajeDeError` (`lib/errores.ts`).

### 9.1 Textos del front

| Dónde | Texto |
|---|---|
| Pregunta del widget (mensaje de Alma) | "¿Cómo te sentís hoy?" |
| Nombre accesible del grupo de caritas | "Cómo te sentís" |
| Etiqueta de `MUY_MAL` | "Muy mal" |
| Etiqueta de `MAL` | "Mal" |
| Etiqueta de `NEUTRO` | "Más o menos" |
| Etiqueta de `BIEN` | "Bien" |
| Etiqueta de `MUY_BIEN` | "Muy bien" |
| Anuncio para el lector de pantalla al registrar (`role="status"`) | "Anotamos cómo te sentís: {etiqueta}." |
| Anuncio para el lector de pantalla al corregir con "Cambiar cómo me siento" (`role="status"`) | "Cambiamos cómo te sentís: {etiqueta}." |
| Error al registrar (aviso arriba de las caritas) | "No pudimos anotar cómo te sentís. Probá de nuevo." (para el 401, el 403 y sin conexión, el texto de `mensajeDeError`) |
| Enlace tras elegir | "Cambiar cómo me siento" |

---

## 10. Back: qué implementar

Paquete base: `ar.edu.utn.frm.gelab.alma`. El módulo vive en `bienestar/`, con el mismo patrón que `persona/` y `recordatorios/`.

- `domain/`: `RegistroAnimo`, `NivelAnimo`.
- `repository/`: `RegistroAnimoRepository`, con una consulta del último registro de una persona (por cuenta) dentro de un rango de fechas, ordenada por `fechaHora` y luego por `id`, ambos descendentes.
- `dto/`: `RegistrarAnimoRequest` (`nivelAnimo`, con `@NotNull`; sirve para el `POST` y el `PUT`) y `RegistroAnimoResponse` (`id`, `nivelAnimo`, `fechaHora`, `preguntaContextual`).
- `service/`: `RegistroAnimoService`, con el `Clock` inyectado (igual que `RecordatorioService`).
- `controller/`: `AnimoController`, con `@Tag(name = "Bienestar")` y `@Operation` en cada endpoint.
- `MensajesAnimo`: constantes de la sección 9 y las preguntas de la sección 6.

**Para el módulo de Chat:** `RegistroAnimoService` expone un método público, por ejemplo `animoDeHoy(Long cuentaId)`, que devuelve `Optional<RegistroAnimoResponse>`. El Chat arma el contexto (`{animo}` del prompt) y el saludo llamando a ese servicio, sin pasar por HTTP: los módulos se comunican solo a través de sus servicios. Qué texto le llega al modelo (por ejemplo, "Bien" en lugar del enum) lo decide el Chat.

**Cambios en archivos compartidos (avisar antes de tocarlos)**

| Archivo | Cambio |
|---|---|
| `config/SecurityConfig.java` | Una línea: `.requestMatchers("/api/animo/**").hasRole("PERSONA_MAYOR")`, **antes** de `.requestMatchers("/api/**").authenticated()` |
| `docs/casos-de-uso.md` | Estado de implementación de CU-01 (Back y Front) |
| `CLAUDE.md` | Enlazar este contrato en "Documentos" |

**No se tocan:** `pom.xml`, `application.properties`, `AlmaApplication.java` (conserva el `TimeZone.setDefault`), `GlobalExceptionHandler` ni `auth/`, `persona/`, `recordatorios/`, `administrador/`, `ejercicios/` o `microcapsulas/`. `DatosDePrueba` tampoco: no se cargan datos de ánimo de ejemplo (los registros se crean usando la app).

**Checklist**

1. Entidad `RegistroAnimo`, enum e índice (sección 4).
2. Servicio: registrar, modificar el ánimo de hoy (5.6), consultar el ánimo de hoy (con el `Clock`) y la pregunta por nivel.
3. Controlador con las rutas de la sección 7.
4. Validación y mensajes (5.7, sección 9).
5. Línea de `SecurityConfig` (sección 8).
6. Pruebas de la sección 12.

---

## 11. Front: qué implementar

**Antes de empezar:** nada nuevo en dependencias ni en `components/ui/`. Se reutilizan `Alert`, `lib/api.ts`, `lib/errores.ts`, el cliente de consultas existente (`lib/cliente-consultas.ts`) y los tokens de color del Figma. Si falta un componente base, se avisa antes de crearlo.

**Estructura** (`features/bienestar/`, plana)

```
index.ts                  exporta WidgetAnimo, BurbujaAnimo, EnlaceCambiarAnimo, ETIQUETAS_ANIMO y los tipos que usa el Chat
tipos.ts                  NivelAnimo, RegistroAnimo, la lista ordenada de niveles
api.ts                    registrarAnimo(nivel), modificarAnimo(id, nivel)
mensajes.ts               textos del módulo (9.1) y las etiquetas por nivel
useGuardarAnimo.ts        mutación de TanStack Query (sin reintentos): crea o modifica
WidgetAnimo.tsx           la pregunta de Alma y las cinco caritas
CaritaAnimo.tsx           una carita (botón con ilustración y etiqueta)
BurbujaAnimo.tsx          burbuja de la persona con la carita elegida y su etiqueta
EnlaceCambiarAnimo.tsx    el enlace "Cambiar cómo me siento"
```

### 11.1 `WidgetAnimo`

Props:

| Prop | Tipo | Uso |
|---|---|---|
| `registroId` | `number \| undefined` | Sin valor, al elegir se crea un registro (`POST`). Con el `id` del registro recién creado, al elegir se corrige ese registro (`PUT`). Lo pasa el padre cuando la persona tocó "Cambiar cómo me siento" |
| `onRegistrado` | `(registro: RegistroAnimo) => void` | El padre recibe el registro (con `id`, nivel y `preguntaContextual`) y decide qué mostrar. Lo llama una sola vez por cada guardado exitoso |

El widget **no** se oculta solo ni muestra la pregunta contextual: lo hace el padre (sección 2).

Comportamiento:

- Se presenta como un mensaje más de Alma con las caritas debajo, no como tarjeta ni modal (CU-01, pantallas relacionadas; prototipo, sección Chat). No bloquea: la persona puede ignorarlo y escribir.
- Cinco caritas en este orden, de izquierda a derecha, como en el Figma: `MUY_BIEN`, `BIEN`, `NEUTRO`, `MAL`, `MUY_MAL` (el orden de la escala del enum, de menos a más, no cambia). Cada una muestra su etiqueta **siempre visible** debajo (nunca solo ilustración o color).
- Tocar una carita registra de inmediato: sin confirmación ni comentario.
- Mientras el pedido está en curso, las cinco caritas quedan deshabilitadas (`aria-disabled` y sin reaccionar a nuevos toques), para que un doble toque no cree dos registros.
- Si falla, se muestra un `Alert` de error arriba de las caritas con el texto de 9.1 (`mensajeDeError`), las caritas se habilitan de nuevo y **no se pierde nada**: la persona puede volver a tocar.
- Las ilustraciones son las del componente "Carita de ánimo" del Figma, como SVG, y no se usan íconos de librería. Las aporta Paula en `src/assets/animo/` con estos nombres: `muy-mal.svg`, `mal.svg`, `neutro.svg`, `bien.svg` y `muy-bien.svg`. Claude Code no las dibuja ni las reemplaza: si todavía no están en esa carpeta cuando llegue al front, lo avisa y espera. Se usan con `alt=""` (la etiqueta de texto ya las nombra).

### 11.2 `BurbujaAnimo` y `EnlaceCambiarAnimo`

Son las piezas que el padre (hoy el Inicio provisorio, después el Chat) muestra cuando la persona ya eligió. Cada una es un componente chico y sin estado propio.

**`BurbujaAnimo`**

| Prop | Tipo | Uso |
|---|---|---|
| `nivelAnimo` | `NivelAnimo` | Nivel elegido: define la carita y la etiqueta |

- Se muestra como un mensaje de la persona (burbuja del lado de ella), con la carita elegida y su etiqueta de 9.1 debajo, como en el Figma ("Burbuja de chat").
- Usa los mismos SVG de 11.1. No es un botón ni se puede tocar.
- No es un mensaje que la persona escribió: no se manda al modelo ni se guarda. El Chat le da al modelo el ánimo por el contexto (sección 10).

**`EnlaceCambiarAnimo`**

| Prop | Tipo | Uso |
|---|---|---|
| `onClick` | `() => void` | El padre vuelve a mostrar el widget con el `registroId` (5.6) |

- Texto "Cambiar cómo me siento" (9.1), con una zona táctil de al menos 44 px de alto.
- Lo muestra el padre debajo de la pregunta de Alma, y lo quita cuando la persona manda su primer mensaje.

### 11.3 Datos

- `registrarAnimo(nivel)`: `POST /animo` con `{ nivelAnimo }` y devuelve el `RegistroAnimo` (7.1).
- `modificarAnimo(id, nivel)`: `PUT /animo/{id}` con `{ nivelAnimo }` y devuelve el `RegistroAnimo` actualizado (7.2).
- `useGuardarAnimo`: `useMutation` sin reintentos (el cliente de consultas ya los desactiva para las mutaciones). Usa `modificarAnimo` si recibe `registroId` y `registrarAnimo` si no. Al terminar bien llama a `onRegistrado`.
- No hay consultas ni caché de ánimo en este módulo: no se invalida nada.

### 11.4 Montaje provisorio en el Inicio

El Chat todavía no existe. Para poder probar CU-01 de punta a punta, `InicioPage` monta el widget de forma **provisoria**, como ya hace con `TarjetaParaHoy`, con un comentario `PROVISORIO` que explique que `feature/chat` lo reemplaza:

1. Se muestra `WidgetAnimo` (debajo de la tarjeta "Para hoy", dentro del mismo bloque angosto).
2. Al registrarse, el widget se oculta, se guarda el registro (con su `id`) en el estado y se muestran, en este orden: `BurbujaAnimo` con el nivel elegido, un mensaje de Alma con la `preguntaContextual` (en una región `role="status"` para el lector de pantalla) y `EnlaceCambiarAnimo`.
3. Tocar el enlace lo oculta y vuelve a mostrar el widget con `registroId` (5.6). La nueva elección corrige ese registro y reemplaza en el mismo lugar la burbuja y el mensaje de Alma por los del nivel nuevo.
4. Todo el estado vive en memoria (`useState`): al recargar, el widget vuelve a aparecer sin `registroId` y la próxima elección crea un registro nuevo (sección 2).
5. En el Inicio todavía no hay campo para escribir, así que el enlace no desaparece con el primer mensaje: esa parte (5.6, "Hasta cuándo") la implementa `feature/chat`.

`InicioPage` es un archivo compartido con Recordatorios: el cambio se limita a ese bloque y se avisa antes.

### 11.5 Accesibilidad (público: personas mayores)

- Las cinco caritas están dentro de un grupo con nombre accesible ("Cómo te sentís") y cada una es un `<button>`. Su nombre accesible es la etiqueta de texto.
- Cada carita mide al menos 48 × 48 px de zona táctil (el prototipo usa 48 px; en celular de 390 px caben cinco en una fila) y el foco se ve claramente.
- Con letra "Muy grande" las etiquetas pueden partirse en dos líneas sin tapar ni cortar las caritas, y con alto contraste cada carita lleva borde.
- Todo se usa con teclado: Tab para moverse y Enter o Espacio para elegir.
- Ningún estado se comunica solo con color.
- Al registrar, el lector de pantalla anuncia el texto de 9.1 y también se le anuncia la pregunta de Alma. Al corregir, se anuncia "Cambiamos cómo te sentís: {etiqueta}." (9.1).
- La burbuja de la persona tiene el texto de la etiqueta (la ilustración va con `alt=""`), así que se lee como un mensaje más.
- Mensajes de error en lenguaje simple, sin códigos.

### 11.6 Diferencias con el Figma

| El Figma | Los documentos | Qué se hace |
|---|---|---|
| Los marcos "Chat (inicio)" y "Chat (después de elegir ánimo)" muestran el saludo, la tarjeta de recordatorios, las caritas y el campo de mensaje | CU-02 es otra rama | Este módulo solo construye el widget y su montaje provisorio; el saludo y el campo de mensaje son de `feature/chat` |
| En el marco "después de elegir ánimo", aparece una burbuja de la persona, la respuesta de Alma y el enlace "Cambiar cómo me siento" | CU-01 paso 5: la pregunta contextual es el siguiente mensaje de Alma | Se implementa como en el Figma: burbuja con la carita elegida, la pregunta de Alma debajo y el enlace. La burbuja solo muestra lo que eligió (11.2) |
| "Cambiar cómo me siento" aparece como enlace de texto, sin indicar hasta cuándo | Prototipo: "es opcional y se puede cambiar después" | Igual, con una zona táctil de al menos 44 px de alto. Corrige el registro recién creado y desaparece con el primer mensaje de la persona (5.6) |

---

## 12. Pruebas y criterio de "listo"

**Back (automáticas, con el mismo patrón de `PerfilIntegrationTests` y `SesionDePrueba`; cuentas temporales con prefijo `test_animo_`)**

- Sin token: `401` en las tres rutas. Token de `admin`: `403`.
- `POST` con `BIEN`: `201` con `id`, `nivelAnimo`, `fechaHora` y la pregunta de ese nivel. Hay un registro nuevo en la base de la persona del token.
- `POST` con cada uno de los cinco niveles devuelve la pregunta que le corresponde.
- `POST` sin `nivelAnimo`, con un valor inválido (`"FELIZ"`) o con cuerpo vacío: `400`, con "Elegí cómo te sentís." en el caso del campo faltante.
- Dos `POST` seguidos crean dos registros (sin límite por día).
- `PUT` sobre el registro de hoy: `200` con el mismo `id`, el nivel nuevo, una `fechaHora` posterior a la anterior (con un `Clock` fijo) y la pregunta de ese nivel; la cantidad de registros no cambia y `GET /hoy` devuelve el nivel nuevo.
- `PUT` con el mismo nivel que ya tenía: `200` sin cambios, tampoco en `fechaHora`.
- `PUT` con nivel faltante o inválido: `400`.
- `PUT` sobre un registro de otra persona o inexistente: `404`. Sin token: `401`. Con token de `admin`: `403`.
- `PUT` sobre un registro de ayer, o sobre uno de hoy que ya no es el más reciente: `404`, y el registro queda como estaba.
- `GET /hoy` sin registros hoy: `204` sin cuerpo.
- `GET /hoy` con varios registros hoy: devuelve el más reciente (con un `Clock` fijo para no depender del reloj).
- Un registro de ayer no cuenta como ánimo de hoy.
- `ana` y `carlos`: cada uno ve solo el suyo.
- `fechaHora` queda en hora de Argentina.
- Las pruebas que ya existen siguen pasando.

**Front (manuales; no hay pruebas automáticas)**

- `ana` ve el widget en el Inicio, toca una carita y se registra sin pedir confirmación.
- El widget se oculta y aparecen la burbuja con la carita elegida, la pregunta de Alma del nivel elegido y el enlace "Cambiar cómo me siento".
- "Cambiar cómo me siento" vuelve a mostrar el widget, y una nueva elección corrige el mismo registro (no suma uno) y reemplaza en el mismo lugar la burbuja y la pregunta, sin agregar mensajes nuevos.
- Después de recargar la página, el widget aparece de nuevo y la elección crea un registro nuevo.
- Que el enlace desaparezca con el primer mensaje de la persona se prueba en `feature/chat`; en el Inicio provisorio no hay campo de mensaje.
- Con el back apagado: aviso simple y las caritas siguen disponibles; no hay pantalla en blanco.
- Doble toque rápido: se crea un solo registro.
- Teclado: Tab pasa por las cinco caritas en orden y Enter o Espacio elige.
- Funciona en celular (390 px) y en escritorio, con letra "Muy grande" y con alto contraste.
- Verificación con lector de pantalla pendiente (igual que en Recordatorios).

**Listo cuando:** con `docker compose up -d`, el back corriendo y `pnpm dev`, `ana` puede registrar su ánimo desde el Inicio, ve la pregunta de Alma que corresponde, puede corregirlo con "Cambiar cómo me siento", y `GET /api/animo/hoy` desde Swagger devuelve el último registro; `carlos` no ve nada de lo de `ana`.

---

## 13. Historial de cambios

- **v1 (03/11/2026):** primera propuesta, contra el commit `321a63f`.
- **v2 (04/11/2026):** se resolvieron los pendientes: "Cambiar cómo me siento" crea un registro nuevo (5.6), textos y preguntas aprobados, ilustraciones como SVG propios (11.1), y `GET /api/animo/hoy` con su motivo. Se quitaron las secciones de pendientes y de uso con Claude Code.
- **v3 (04/11/2026):** "Cambiar cómo me siento" corrige el registro recién creado en lugar de crear uno nuevo: se agrega `PUT /api/animo/{id}` (7.2), solo para el ánimo de hoy, que actualiza `nivelAnimo` y `fechaHora`. Un nuevo ingreso a la app sigue creando un registro nuevo. Se actualizan 5.6, el widget (`registroId`), el montaje provisorio y las pruebas.
- **v4 (04/11/2026):** al elegir, el widget se reemplaza por una burbuja de la persona con la carita elegida y debajo la pregunta de Alma (11.2, `BurbujaAnimo`). "Cambiar cómo me siento" está disponible hasta el primer mensaje de la persona, y al usarlo reemplaza la burbuja y la pregunta en el mismo lugar (5.6). Se actualizan decisiones, montaje provisorio, accesibilidad, diferencias con el Figma y pruebas.
- **v5 (04/11/2026):** el "ánimo de hoy" se calcula solo con el `Clock` inyectado (5.2) y se define el anuncio al corregir (9.1, 11.5). En 5.7 se aclara que el 400 por cuerpo ilegible ya lo resuelve `GlobalExceptionHandler`.
- **v6 (04/11/2026):** las caritas del widget van de izquierda a derecha de "Muy bien" a "Muy mal", como en el Figma (11.1). Antes decía de `MUY_MAL` a `MUY_BIEN`. Solo cambia el orden en pantalla.
