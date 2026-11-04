# Contrato de Juegos de estimulación · Alma Acompaña

**Estado:** propuesta para acordar entre los dos. Una vez aprobada, es la referencia única para el back y el front del módulo. Si algo cambia, se cambia primero acá y se avisa al otro.

**Fuente de verdad:** caso de uso CU-05 (Realizar ejercicio de estimulación) y el diagrama de clases (clases `Ejercicio`, `OpcionRespuesta`, `IntentoEjercicio` y `ProgresoJuego`, y los enums `CategoriaEjercicio`, `FormatoEjercicio` y `NivelDificultad`). Si este contrato contradice esos documentos, ganan los documentos y se corrige el contrato. El Figma es ilustrativo: donde difiere en funcionalidad, ganan los documentos (ver sección 12).

**Dónde va:** `docs/contrato-juegos.md`. El `CLAUDE.md` debe enlazarlo en la sección "Documentos".

**Última revisión contra el repo:** commit `321a63f` (merge del PR #3, `feature/recordatorios`, 29/10/2026), `main` del 04/11/2026.

**Contratos relacionados:** `docs/contrato-login.md` (token, roles, formato de errores, convenciones), `docs/contrato-panel-operador.md` (el catálogo de ejercicios y las imágenes que consume el juego) y `docs/contrato-recordatorios.md` (fechas y reloj). Todo lo que no se redefine acá sigue como está ahí.

---

## 1. Alcance

**Incluye:**
- El juego de la persona mayor: elegir el próximo ejercicio, mostrarlo, validar la respuesta, registrar el intento, ajustar el nivel y la ronda de 10.
- La pantalla Entretenimiento (con "Jugar" y "Leer"), la de inicio de Juegos, la del ejercicio con su respuesta y la del final.
- Las entidades `ProgresoJuego` e `IntentoEjercicio`.
- La ampliación de los datos de prueba (anexo A).
- Pruebas automáticas del back.

**No incluye:**
- La lectura de microcápsulas (CU-06): "Leer" queda como tarjeta que lleva a una pantalla provisoria.
- Puntajes, rankings, historial visible o estadísticas (el CU lo prohíbe: nada clínico ni diagnóstico).
- Un consumidor del progreso en el chat u otros módulos.
- El alta de ejercicios: ya está en el panel de operador (CU-10).
- Temporizadores o límites de tiempo.

---

## 2. Decisiones

| Tema | Decisión | Motivo |
|---|---|---|
| Quién elige | El back. `GET /api/juegos/siguiente` lo calcula cada vez a partir de `ProgresoJuego`; no se guarda un "ejercicio en curso" | CU-05 C.A. Nº2: abandonar no deja nada que limpiar |
| Categoría | Rotación fija `REFRAN → SERIE → ASOCIACION → MEMORIA → REFRAN…`, a partir de la categoría del último ejercicio jugado. Sin ejercicio previo, empieza en `REFRAN` | CU-05 2.2 ("secuencia definida por el sistema") |
| Ejercicio dentro de la categoría | Entre los activos del nivel, el que la persona jugó hace más tiempo; los nunca jugados van primero y, si hay empate, al azar | Resuelve C.A. Nº1 sin lógica aparte |
| Último ejercicio | Se excluye siempre que haya otro activo en el nivel. Si es el único del nivel, se repite | CU-05 2.3 y C.A. Nº1 |
| Falta de ejercicios | Si en el nivel actual no hay ninguno en una categoría, se sigue con la categoría siguiente. Si no hay ninguno en todo el nivel, se prueba el nivel más cercano (sección 5.3). Si no hay ningún ejercicio activo, 404 | El catálogo lo carga el operador y puede estar incompleto |
| Nivel | Sube con 3 aciertos seguidos y baja con 2 errores seguidos. Al cambiar de nivel, los dos contadores vuelven a 0. En `BASICO` los errores no bajan más y en `AVANZADO` los aciertos no suben más (los contadores igual se reinician) | CU-05, regla interna de dificultad |
| Validación | En el back (`POST /api/juegos/respuesta`). El front nunca recibe `esCorrecta` ni `ordenCorrecto` antes de responder | Si no, la respuesta viaja en el JSON |
| ORDENAR | El front manda los ids de las opciones en el orden elegido. Sin respuesta parcial | CU-05, regla de negocio |
| "Casi" | Es cualquier error, con tono suave (panel "¡Casi!"). No hay criterio de puntaje aparte | El Figma tiene "respuesta casi"; el CU no tiene respuesta parcial |
| Mezcla | El back mezcla las opciones en cada `siguiente` (opción múltiple y ordenar). En ordenar, nunca devuelve el orden correcto | En el seed la correcta es la primera y los ids de ordenar van en orden |
| Ronda de 10 | La calcula el back con los intentos del día (zona de Argentina): `posicion = intentosDeHoy % 10 + 1` | Sobrevive a cerrar la app. El CU no la menciona: se agrega al CU-05 (sección 11) |
| Memoria | El back manda `textoAMemorizar`; el front lo muestra solo, con "Ya lo recordé", y después lo oculta | CU-05 3.1; no necesita estado en el back |
| Efecto de `siguiente` | `GET /siguiente` no escribe nada. `ProgresoJuego` se crea con el primer intento | Un GET sin efectos; abandonar no deja filas |
| Abandonar | Sin endpoint ni diálogo de confirmación: la persona sale y listo | "Podés dejar cuando quieras" |
| Ejercicio dado de baja a mitad del juego | Si el operador lo da de baja entre el `siguiente` y la respuesta, la respuesta se acepta igual | No castigar a la persona por algo ajeno |
| Dueño del dato | Todo sale del token. No hay ids de persona en URL ni body | Contrato de login, sección 2 |
| Fechas | `LocalDate` y `LocalDateTime` de Argentina, sin zona en el JSON. Se usa el `Clock` / `Fechas` que ya existen | Mismo criterio que recordatorios |
| Seed | Se amplía a 30 ejercicios con un componente nuevo (anexo A). Se apaga con `ALMA_SEED_ENABLED=false` | Pedido del equipo; no se toca `DatosDeCatalogo` |

---

## 3. Convenciones de la API

Valen las del contrato de login (sección 3). Para este módulo:

- Todo cuelga de `/api/juegos`.
- Enums en `MAYUSCULAS_CON_GUION_BAJO` (los que ya existen en `ejercicios/domain`).
- Las imágenes de las opciones llegan como ruta (`"/imagenes/..."`) o `null`, igual que en el panel.
- El nivel **nunca** viaja al front: ni en `siguiente` ni en la respuesta.

---

## 4. Modelo de datos

Según el DC. No se agregan campos; si se necesitan, primero se actualiza el DC.

- `ProgresoJuego`: `id`, `personaMayor` (uno a uno, único), `nivelActual` (`NivelDificultad`, por defecto `BASICO`), `aciertosConsecutivos` (`int`), `erroresConsecutivos` (`int`), `fechaUltimaSesion` (`LocalDate`), `ultimoEjercicio` (muchos a uno, opcional).
- `IntentoEjercicio`: `id`, `personaMayor` (muchos a uno), `ejercicio` (muchos a uno, opcional), `fechaHora` (`LocalDateTime`; el DC dice `Date` pero hace falta la hora para la ronda y para "jugado hace más tiempo"), `acierto` (`Boolean`).
- Índice recomendado sobre `IntentoEjercicio` (`personaMayor`, `fechaHora`). No es un dato nuevo.

Paquete nuevo `juegos` con `controller/`, `service/`, `repository/`, `domain/` y `dto/`. Los dominios existentes (`Ejercicio`, `OpcionRespuesta`) se importan de `ejercicios.domain`: no se tocan.

---

## 5. Reglas de negocio

### 5.1 Qué ejercicio sigue (`siguiente`)

1. Se busca el `ProgresoJuego` de la persona. Si no existe, se parte de `BASICO` y sin ejercicio previo (sin crearlo).
2. Categoría de partida: la que sigue a la del `ultimoEjercicio` en la rotación; sin último, `REFRAN`.
3. Se arma el orden de niveles a probar (5.3).
4. Para cada nivel, en ese orden:
   1. `pool` = ejercicios activos (sin `fechaBaja`) de ese nivel. Si está vacío, se pasa al nivel siguiente.
   2. `elegibles` = `pool` menos el `ultimoEjercicio`. Si queda vacío, `elegibles` = `pool` (C.A. Nº1).
   3. Se recorren las categorías desde la de partida, dando la vuelta completa. En la primera que tenga elegibles se elige el que se jugó hace más tiempo (último `IntentoEjercicio` más viejo; los nunca jugados primero; empate al azar).
5. Si ningún nivel tiene ejercicios, 404 (sección 8).

### 5.2 Qué se devuelve

El ejercicio sin respuestas correctas, las opciones mezcladas y la posición en la ronda (sección 6.1).

### 5.3 Orden de niveles

| Nivel actual | Se prueba en este orden |
|---|---|
| `BASICO` | `BASICO`, `INTERMEDIO`, `AVANZADO` |
| `INTERMEDIO` | `INTERMEDIO`, `BASICO`, `AVANZADO` |
| `AVANZADO` | `AVANZADO`, `INTERMEDIO`, `BASICO` |

### 5.4 Responder

1. Se busca el ejercicio por id (incluye los dados de baja). Si no existe: 404.
2. Se valida el cuerpo según el formato (5.6). Si es inválido: 400, y no se registra nada.
3. Se calcula `acierto`:
   - `OPCION_MULTIPLE`: la opción elegida tiene `esCorrecta`.
   - `ORDENAR`: la lista de ids coincide, posición por posición, con las opciones ordenadas por `ordenCorrecto`.
4. Se guarda un `IntentoEjercicio` (`fechaHora` = ahora, `acierto`).
5. Se crea o actualiza el `ProgresoJuego`: `ultimoEjercicio` = este, `fechaUltimaSesion` = hoy y los contadores y el nivel según 5.5.
6. Se devuelve el resultado (6.2).

Todo en una sola transacción.

### 5.5 Contadores y nivel

- **Acierto:** `aciertosConsecutivos + 1`, `erroresConsecutivos = 0`. Si `aciertosConsecutivos` llega a 3: el nivel sube uno (si no es `AVANZADO`) y los dos contadores vuelven a 0.
- **Error:** `erroresConsecutivos + 1`, `aciertosConsecutivos = 0`. Si `erroresConsecutivos` llega a 2: el nivel baja uno (si no es `BASICO`) y los dos contadores vuelven a 0.

Esta lógica vive en métodos de `ProgresoJuego` (`registrarAcierto()`, `registrarError()`), sin dependencias, para probarla sin base.

### 5.6 Validación del cuerpo de la respuesta

- `ejercicioId` obligatorio.
- `OPCION_MULTIPLE`: `opcionId` obligatorio y de ese ejercicio. `ordenOpcionIds` debe venir vacío o ausente.
- `ORDENAR`: `ordenOpcionIds` obligatorio, con **todos** los ids de las opciones del ejercicio, sin repetir ni sobrar. `opcionId` debe venir ausente.

### 5.7 Ronda

- `intentosDeHoy` = intentos de la persona con `fechaHora` entre las 00:00 de hoy y las 00:00 de mañana (Argentina).
- `posicion = intentosDeHoy % 10 + 1` y `totalRonda = 10`.
- `rondaCompleta` = el intento recién guardado hizo que `intentosDeHoy % 10 == 0`.
- `jugadosHoy` = `intentosDeHoy` después de guardar. El final dice "Jugaste N juegos hoy".

---

## 6. Endpoints

### 6.1 `GET /api/juegos/siguiente`

Respuesta `200`:

```json
{
  "ejercicio": {
    "id": 12,
    "categoria": "REFRAN",
    "formato": "OPCION_MULTIPLE",
    "consigna": "Completá el refrán: «A caballo regalado…»",
    "textoAMemorizar": null,
    "opciones": [
      { "id": 41, "texto": "…se le pone montura", "imagen": null },
      { "id": 39, "texto": "…no se le miran los dientes", "imagen": null },
      { "id": 40, "texto": "…se le da de comer", "imagen": null }
    ]
  },
  "posicion": 2,
  "totalRonda": 10
}
```

- `textoAMemorizar` viene solo en `MEMORIA`; en el resto, `null`.
- En `ORDENAR`, `opciones` son los pasos, mezclados y nunca en el orden correcto.
- No hay `esCorrecta`, `ordenCorrecto`, `explicacion` ni nivel.
- Errores: `404` si no hay ningún ejercicio activo (sección 8).

### 6.2 `POST /api/juegos/respuesta`

Cuerpo, opción múltiple:

```json
{ "ejercicioId": 12, "opcionId": 39 }
```

Cuerpo, ordenar:

```json
{ "ejercicioId": 7, "ordenOpcionIds": [20, 22, 21, 19] }
```

Respuesta `200`:

```json
{
  "acierto": false,
  "explicacion": "Quiere decir que un regalo se agradece sin fijarse en sus defectos.",
  "respuestaCorrecta": ["…no se le miran los dientes"],
  "rondaCompleta": false,
  "jugadosHoy": 2
}
```

- `respuestaCorrecta` es una lista de textos: de un solo elemento en opción múltiple y en el orden correcto en ordenar.
- `explicacion` puede ser `null` (es opcional en el catálogo).
- Se devuelve tanto en acierto como en error.
- Errores: `400` (5.6), `404` (el ejercicio no existe).

---

## 7. Reglas de acceso

| Ruta | Quién puede |
|---|---|
| `/api/juegos/**` | Solo `PERSONA_MAYOR`. Un `ADMINISTRADOR` recibe `403` |

Sin token o con token vencido: `401`, como en el resto de `/api`.

---

## 8. Errores y mensajes

Formato Problem Details, igual que en el contrato de login. Los textos generales viven en `common/MensajesError` y los de este módulo en `juegos/MensajesJuego`; nadie los repite.

| Status | Cuándo | `detail` |
|---|---|---|
| 400 | Cuerpo inválido (5.6) | "Revisá los datos ingresados." más la lista `errores` |
| 401 | Token faltante, inválido o vencido | "Tu sesión terminó. Ingresá de nuevo." |
| 403 | Rol sin permiso | "No tenés permiso para ver esta pantalla." |
| 404 | El ejercicio de la respuesta no existe | "No encontramos lo que buscás." |
| 404 | No hay ejercicios activos para jugar | "Por ahora no hay juegos disponibles. Probá más tarde." |
| 500 | Error interno | "Algo salió mal. Probá de nuevo en un rato." |

Textos de `errores` propios del módulo: `opcionId` → "Elegí una opción para responder."; `ordenOpcionIds` → "Ordená todos los pasos para responder.".

---

## 9. Back: qué implementar

```
juegos/
  controller/JuegoController
  service/JuegoService            siguiente(), responder()
  service/SelectorDeEjercicio     lógica pura de 5.1 (sin base), probada con tests unitarios
  domain/ProgresoJuego            con registrarAcierto() y registrarError()
  domain/IntentoEjercicio
  repository/ProgresoJuegoRepository
  repository/IntentoEjercicioRepository
  dto/EjercicioJuegoResponse, OpcionJuegoResponse, SiguienteResponse, RespuestaRequest, ResultadoResponse
  MensajesJuego
config/DatosDeCatalogoJuegos      seed del anexo A
```

Orden de implementación, con pausa de prueba al final de cada paso:

1. **Entidades y repositorios.** `ProgresoJuego`, `IntentoEjercicio` y sus repositorios. Consultas que hacen falta: intentos de la persona desde / hasta (para contar la ronda) y último intento por ejercicio de la persona. En `EjercicioRepository` se agrega un método (cambio compartido): ejercicios activos por nivel.
2. **Nivel.** `registrarAcierto()` y `registrarError()` con tests unitarios (3 aciertos suben, 2 errores bajan, tope en ambos extremos, los contadores se reinician).
3. **Selector.** `SelectorDeEjercicio` y sus tests unitarios (rotación, exclusión del último, C.A. Nº1, categorías vacías, nivel más cercano, sin ejercicios).
4. **Servicio y endpoints.** `siguiente` y `responder`, DTO sin respuestas correctas, mezcla de opciones, `MensajesJuego`, regla en `SecurityConfig` y documentación en Swagger.
5. **Seed.** `DatosDeCatalogoJuegos` con el anexo A, idempotente por consigna.
6. **Pruebas de integración** (sección 13) y resumen.

### Cambios en archivos compartidos (para la descripción del PR)

- `SecurityConfig.java`: una línea, `.requestMatchers("/api/juegos/**").hasRole("PERSONA_MAYOR")`, antes de la regla general de `/api/**`.
- `EjercicioRepository.java`: un método nuevo (ejercicios activos por nivel). Es aditivo.

---

## 10. Front: qué implementar

Carpetas nuevas (no se tocan `features/ejercicios`, que es del panel de operador):

- `features/entretenimiento/`: `EntretenimientoPage` (el hub con "Jugar" y "Leer") y `mensajes.ts`.
- `features/juegos/`: `JuegosInicioPage`, `JuegoPage` (ejercicio, respuesta y final), `api.ts`, `tipos.ts`, `mensajes.ts` y los componentes del juego. Cuando pase de unos 10 archivos se parte en `pages/` y `components/` (CLAUDE.md).

### 10.1 Rutas (dentro del `LayoutPersona`)

| Ruta | Pantalla |
|---|---|
| `/entretenimiento` | Hub (reemplaza la provisoria) |
| `/entretenimiento/juegos` | Inicio de Juegos |
| `/entretenimiento/juegos/jugar` | Ejercicio, respuesta y final (estados de la misma pantalla) |
| `/entretenimiento/lecturas` | `PaginaProvisoria` "Lecturas" (hasta CU-06) |

### 10.2 Pantallas

- **Hub.** Dos tarjetas: "Jugar" ("Juegos cortos de memoria, sin apuros.") lleva a `/entretenimiento/juegos`; "Leer" ("Lecturas cortas para cuidarte todos los días.") lleva a la provisoria.
- **Inicio de Juegos.** Volver a Entretenimiento, título "Juegos", tarjeta con Alma saludando, "Hoy te traje diez juegos cortos", "Vamos a ejercitar la memoria. Podés dejar cuando quieras." y el botón "Empezar".
- **Ejercicio.**
  - Barra superior con "Volver" y el progreso "N de 10" (texto más barra, con `role="progressbar"` y valores).
  - Chip de categoría: "Refranes", "Series", "Asociaciones" o "Memoria".
  - Consigna.
  - Si hay `textoAMemorizar`: primero solo el texto y "Ya lo recordé"; al tocarlo se oculta y aparecen consigna y opciones. No hay vuelta atrás.
  - `OPCION_MULTIPLE`: opciones grandes (alto mínimo de 56 px en celular, como en el Figma), con imagen si la tienen. **Tocar una opción es la respuesta**: no hay botón de confirmación. Al tocarla se envía, todas las opciones quedan deshabilitadas mientras viaja la respuesta y la elegida queda marcada.
  - `ORDENAR`: se toca cada paso en el orden que se quiere; cada uno muestra su número, se puede destocar y hay "Empezar de nuevo". "Listo" se habilita con todos ordenados. Sin arrastrar (accesibilidad).
- **Respuesta.** Panel inferior que reemplaza la zona de botones: "¡Muy bien!" (acierto) o "¡Casi!" (error) con "La respuesta era: «…»" (en ordenar, la lista numerada), la explicación si hay y el botón "Continuar". El foco pasa al panel y se anuncia con `role="status"`. Nunca se muestra puntaje.
- **Final.** Si `rondaCompleta`: Alma festejando, "Jugaste N juegos hoy", "¡Muy buen trabajo! Cuando quieras, seguimos.", "Seguir jugando" y "Salir" (vuelve a Entretenimiento).

### 10.3 Datos

- `useSiguienteEjercicio`: `staleTime: 0`, `gcTime: 0`, **sin** refetch al volver a la ventana ni al reconectar (si no, el ejercicio cambia mientras la persona lo mira).
- `useResponder`: mutación; con el resultado se muestra el panel. "Continuar" o "Seguir jugando" vuelven a pedir `siguiente`.
- Si la persona sale en medio de un ejercicio no se llama a nada (ver sección 2).

### 10.4 Accesibilidad

Botones y opciones de 44 px como mínimo, etiquetas visibles, todo usable con teclado, sin tiempos, sin color como única señal (el acierto y el error llevan texto e ícono), alto contraste (`contraste-alto`) y tamaño de letra de 100/125/150 %. Tamaños y unidades en `rem`, como el resto de la app.

### 10.5 Textos

Todos los textos de las pantallas en `features/juegos/mensajes.ts` y `features/entretenimiento/mensajes.ts`; los generales siguen en `lib/mensajes.ts` y `lib/errores.ts`. Propuestos: "Empezar", "Listo", "Ya lo recordé", "Empezar de nuevo", "Continuar", "Seguir jugando", "Salir", "No pudimos cargar el juego. Probá de nuevo.".

### Cambios en archivos compartidos (para la descripción del PR)

- `App.tsx`: las cuatro rutas de 10.1 (la de `/entretenimiento` reemplaza a la provisoria).
- `lib/mensajes.ts`: no se espera cambio; si hace falta un texto general, se agrega sin tocar los existentes.

---

## 11. Documentos a actualizar

- `docs/casos-de-uso.md`, CU-05: agregar la ronda de 10 con la pantalla final ("Jugaste N juegos hoy", "Seguir jugando" / "Salir"), el feedback con la explicación y la respuesta correcta, y la regla "sin puntaje" ya existente. Marcar CU-05 como hecho en la tabla de estado.
- `CLAUDE.md`: enlazar este contrato en "Documentos".
- Diagrama de clases: aclarar que `IntentoEjercicio.fecha_hora` se guarda con hora (`LocalDateTime`).

---

## 12. Diferencias con el Figma

| Figma | Contrato | Motivo |
|---|---|---|
| Pantallas de ejercicio y final solo en celular | Se arman también en escritorio, adaptando las de celular | Pedido del equipo |
| La pantalla del ejercicio no muestra la navegación inferior | Se mantiene la del `LayoutPersona` | Layout compartido; no se toca en este módulo (pendiente 1) |
| Título "Acordate cómo sigue." y debajo la frase del refrán | Chip de categoría + `consigna` del catálogo (una sola línea grande) | Ganan los documentos: la consigna es un dato del ejercicio |
| Panel "¡Muy bien!" muestra la frase completa del refrán | Muestra la explicación del ejercicio, que en el seed incluye la frase completa | La frase completa no es un dato del DC |
| Panel de acierto igual al de error en estructura | "¡Muy bien!" y "¡Casi!" con "La respuesta era…"; solo cambia el tono | Una sola pieza de interfaz |
| Un botón fijo en la zona inferior de la pantalla del ejercicio | En opción múltiple no hay botón (tocar la opción responde); en ordenar, "Listo"; con texto a memorizar, "Ya lo recordé". Después de responder, el panel trae "Continuar" | Decisión del equipo; CU-05 3.1 y 3.3 |
| La opción múltiple mantiene las opciones visibles debajo del panel de respuesta | Igual: el panel de respuesta ocupa la zona inferior y las opciones quedan arriba, sin poder tocarse | Así está en "respuesta casi" |
| "Jugaste 10 juegos hoy" | "Jugaste N juegos hoy", con N real | Varias rondas el mismo día |
| Pantallas de ordenar y de memoria sin diseño | Se arman con los mismos componentes | No estaban diseñadas |

---

## 13. Pruebas y criterio de "listo"

**Tests unitarios (sin base):** `ProgresoJuego` (nivel y contadores) y `SelectorDeEjercicio` (todo lo de 5.1).

**Tests de integración** (`@SpringBootTest` y MockMvc; necesitan `docker compose up -d`):

- `GET /siguiente` con persona mayor: 200, sin `esCorrecta`, sin `ordenCorrecto`, sin nivel.
- Sin token: 401. Con token de administrador: 403.
- `POST /respuesta` correcta: acierto `true`, intento guardado, progreso creado.
- `POST /respuesta` incorrecta: acierto `false`, `respuestaCorrecta` presente.
- Ordenar: orden completo y correcto acierta; con un par invertido, error; con ids faltantes o repetidos, 400.
- Tres aciertos seguidos suben de nivel y dos errores seguidos bajan.
- `rondaCompleta` en el intento 10 y `posicion` que vuelve a 1.
- Una persona no ve ni altera el progreso de otra (ana y carlos).
- `POST` con un ejercicio inexistente: 404. Sin ejercicios activos en `siguiente`: 404.

**Listo cuando:**
- Se juegan 10 ejercicios seguidos con ana, con las cuatro categorías rotando y sin repetir el anterior.
- Se cierra el navegador a mitad de un ejercicio y al volver sigue desde el último completado, sin registro del abandonado.
- Con 3 aciertos seguidos aparecen ejercicios más difíciles, y con 2 errores seguidos, más fáciles.
- Un ejercicio dado de baja desde el panel deja de aparecer.
- Anda con teclado, en celular, en escritorio, con letra 150 % y con alto contraste.

---

## 14. Pendientes para decidir

1. **Navegación en el ejercicio:** el Figma no muestra la barra inferior. Hoy queda la del layout. Si se quiere ocultar, es un cambio en un archivo compartido y va en una tarea aparte.
2. **Confirmación al salir:** no hay; si se ve que se sale sin querer, se agrega.
3. **Doble envío:** si la persona responde dos veces el mismo ejercicio (por ejemplo, con el botón Atrás), se registran dos intentos. El front deshabilita las opciones (o "Listo") mientras envía. No hay guarda en el back.
4. **"Intruso con fotos":** necesita imágenes; se cargan desde el panel. El seed solo trae versiones de texto.
5. **Ids de ordenar:** los ids de las opciones se asignan en el orden correcto, así que alguien que mire la red podría deducirlo. Es aceptable para el MVP (no es un dato sensible); si no, se usan ids opacos.
6. **Pruebas del front:** no hay automáticas; las de la sección 13 son manuales.

---

## 15. Cómo usar este documento con Claude Code

**Back**
> Leé `CLAUDE.md` y `docs/contrato-juegos.md`. Mostrame primero el plan del back según las secciones 5, 6 y 9, y no escribas código hasta que lo apruebe.

**Front**
> Leé `CLAUDE.md` y `docs/contrato-juegos.md`. Mostrame primero el plan del front según la sección 10, y no escribas código hasta que lo apruebe.

Si Claude Code propone algo que contradice el contrato, gana el contrato. Si hay que cambiarlo, se edita este archivo.

---

## 16. Historial de cambios

- **v1 (04/11/2026):** primera propuesta, contra el commit `321a63f`.
- **v1.1 (04/11/2026):** en opción múltiple, tocar la opción es la respuesta (sin botón "Responder"); se confirma ordenar sin arrastrar; diferencias con el Figma ajustadas tras revisar las pantallas.

---

## Anexo A. Datos de prueba (seed)

Componente nuevo `config/DatosDeCatalogoJuegos`, con `alma.seed.enabled=true`, idempotente por consigna (como `DatosDeCatalogo`, que no se toca). **En cada ejercicio de opción múltiple la primera opción es la correcta** (el back las mezcla). En ordenar, los pasos van en el orden correcto. Con los 6 que ya existen quedan 30 ejercicios: cada categoría tiene al menos 2 por nivel.

### REFRAN (opción múltiple)

| Nivel | Consigna | Opciones (la primera es la correcta) | Explicación |
|---|---|---|---|
| BASICO | Completá el refrán: «A caballo regalado…» | …no se le miran los dientes · …se le da de comer · …se le pone montura | Quiere decir que un regalo se agradece sin fijarse en sus defectos. El refrán completo es: «A caballo regalado no se le miran los dientes». |
| BASICO | Completá el refrán: «Al que madruga…» | …Dios lo ayuda · …se le hace tarde · …le da sueño | Quiere decir que levantarse temprano ayuda a hacer más cosas. El refrán completo es: «Al que madruga Dios lo ayuda». |
| INTERMEDIO | Completá el refrán: «Camarón que se duerme…» | …se lo lleva la corriente · …sueña con el mar · …nada más rápido · …encuentra más comida | Quiere decir que quien se descuida pierde la oportunidad. El refrán completo es: «Camarón que se duerme se lo lleva la corriente». |
| INTERMEDIO | Completá el refrán: «En casa de herrero…» | …cuchillo de palo · …siempre hay fuego · …todo está en orden · …nunca falta trabajo | Quiere decir que a veces quien sabe hacer algo no lo tiene resuelto en su propia casa. El refrán completo es: «En casa de herrero, cuchillo de palo». |
| AVANZADO | Completá el refrán: «Quien a buen árbol se arrima…» | …buena sombra lo cobija · …buen fruto le cae · …nunca le falta el sol | Quiere decir que acercarse a buenas personas trae beneficios. El refrán completo es: «Quien a buen árbol se arrima, buena sombra lo cobija». |
| AVANZADO | Completá el refrán: «El que mucho abarca…» | …poco aprieta · …todo lo pierde · …nunca descansa · …más gana | Quiere decir que quien quiere hacer demasiado a la vez termina haciendo poco. El refrán completo es: «El que mucho abarca poco aprieta». |

### SERIE

| Nivel | Formato | Consigna | Opciones / pasos | Explicación |
|---|---|---|---|---|
| BASICO | Opción múltiple | ¿Qué número sigue? 5, 10, 15, 20… | 25 · 30 · 24 | Cada número es 5 más que el anterior. |
| BASICO | Ordenar | Ordená las comidas del día, de la mañana a la noche | Desayuno · Almuerzo · Merienda · Cena | Durante el día comemos en este orden: primero el desayuno y al final la cena. |
| INTERMEDIO | Opción múltiple | ¿Qué número sigue? 3, 6, 12, 24… | 48 · 36 · 30 · 27 | Cada número es el doble del anterior. |
| INTERMEDIO | Ordenar | Ordená estos meses, del primero al último del año | Marzo · Junio · Septiembre · Diciembre | En el año vienen en este orden: marzo, junio, septiembre y diciembre. |
| AVANZADO | Opción múltiple | ¿Qué número sigue? 1, 4, 9, 16… | 25 · 20 · 24 · 32 | Son los números multiplicados por sí mismos: 1×1, 2×2, 3×3, 4×4. Sigue 5×5, que es 25. |
| AVANZADO | Opción múltiple | ¿Qué número sigue? 2, 3, 5, 8, 12… | 17 · 16 · 15 · 18 | Se suma 1, después 2, después 3, después 4, y ahora toca sumar 5. |

### ASOCIACION (opción múltiple)

| Nivel | Consigna | Opciones (la primera es la correcta) | Explicación |
|---|---|---|---|
| BASICO | ¿Con qué se toma la sopa? | Cuchara · Tenedor · Cuchillo | La sopa es líquida y se toma con cuchara. |
| BASICO | ¿Qué objeto usás para saber la hora? | Reloj · Brújula · Balanza | El reloj nos dice qué hora es. |
| INTERMEDIO | El pan se compra en la panadería. ¿Y los medicamentos? ¿Dónde se compran? | En la farmacia · En la carnicería · En la biblioteca | Los medicamentos se compran en la farmacia. |
| INTERMEDIO | ¿Cuál es el intruso? Lunes, Martes, Julio, Viernes | Julio · Lunes · Martes · Viernes | Lunes, martes y viernes son días de la semana. Julio es un mes. |
| AVANZADO | El pájaro vive en un nido. ¿Dónde vive la abeja? | En una colmena · En una cueva · En una jaula | Las abejas viven en la colmena. |
| AVANZADO | ¿Cuál es el intruso? Rosa, Margarita, Tulipán, Pino | Pino · Rosa · Margarita · Tulipán | Rosa, margarita y tulipán son flores. El pino es un árbol. |

### MEMORIA (opción múltiple, con texto a memorizar)

| Nivel | Texto a memorizar | Consigna | Opciones (la primera es la correcta) | Explicación |
|---|---|---|---|---|
| BASICO | Leé con atención: tu cita con el médico es el martes a las cuatro de la tarde. | ¿Qué día es la cita con el médico? | Martes · Jueves · Sábado | La cita era el martes. Repetir en voz alta ayuda a recordar. |
| BASICO | Leé con atención: hoy Marta compró manzanas, pan y queso. | ¿Cuál de estas cosas compró Marta? | Queso · Pescado · Fideos · Leche | Marta compró manzanas, pan y queso. |
| INTERMEDIO | Leé con atención: Lucía tiene un perro que se llama Bruno y un gato que se llama Tito. | ¿Cómo se llama el perro de Lucía? | Bruno · Tito · Lucho | El perro de Lucía se llama Bruno. El gato es Tito. |
| INTERMEDIO | Leé con atención esta lista: lápiz, tijera, cinta, regla y sobre. | ¿Cuál de estas cosas estaba en la lista? | Tijera · Pincel · Goma · Compás | En la lista estaban el lápiz, la tijera, la cinta, la regla y el sobre. |
| AVANZADO | Leé con atención: el domingo vamos a almorzar a lo de Rosa. Llevamos un postre de durazno y una botella de agua. | ¿Qué postre llevamos? | De durazno · De frutilla · De manzana · De banana | El postre era de durazno. |
| AVANZADO | Leé con atención esta lista: harina, azúcar, huevos, manteca, vainilla y limón. | ¿Cuál de estos ingredientes estaba en la lista? | Vainilla · Chocolate · Canela · Leche | En la lista estaban la harina, el azúcar, los huevos, la manteca, la vainilla y el limón. |
