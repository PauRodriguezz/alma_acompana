# Contrato de Recordatorios · Alma Acompaña

**Estado:** propuesta para acordar entre los dos. Una vez aprobada, es la referencia única para el back y el front del módulo. Si algo cambia, se cambia primero acá y se avisa al otro.

**Fuente de verdad:** casos de uso CU-07 (recordatorio de medicación), CU-08 (recordatorio de cita médica) y CU-09 (aviso de recordatorio), en su versión v2, y el diagrama de clases (clases `Recordatorio`, `RecordatorioMedicacion`, `RecordatorioCita` y `NotificacionRecordatorio`, y los enums `FrecuenciaToma` y `EstadoNotificacion`). Si este contrato contradice esos documentos, ganan los documentos y se corrige el contrato. El Figma es ilustrativo: donde difiere en funcionalidad, ganan los documentos (ver sección 12).

**Dónde va:** `docs/contrato-recordatorios.md`. El `CLAUDE.md` debe enlazarlo en la sección "Documentos".

**Última revisión contra el repo:** commit `19764e1` (merge de `feature/login`, 28/10/2026).

**Contratos relacionados:** `docs/contrato-login.md`. Todo lo que no se redefine acá (token, roles, formato de errores, convenciones) sigue como está ahí.

---

## 1. Alcance

**Incluye:** alta, edición, baja y consulta de recordatorios de medicación y de citas médicas; generación de sus notificaciones; marcar una toma como tomada (y deshacerla); marcar una cita como vista; la agenda para la lista, el calendario y el aviso dentro de la app; los mensajes de error del módulo y los datos de prueba.

**No incluye:**
- Notificaciones push, por correo o del sistema operativo (CU-09 v2 habla solo de avisos dentro de la app).
- Un proceso programado en el servidor (`@Scheduled`).
- Avisos de días anteriores que quedaron sin tomar: siguen guardados como `PENDIENTE`, pero la tarjeta "Para hoy" solo mira el día actual.
- Repetición de citas, recordatorios de actividades y sincronización con calendarios externos.
- La tarjeta de recordatorios del saludo de Alma ni el chat: son del módulo de Chat (CU-02). Acá solo queda lista la API y un método de servicio que el chat puede usar (sección 9).
- La navegación y la pantalla Perfil: van en su propia tanda de trabajo.

---

## 2. Decisiones

| Tema | Decisión | Motivo |
|---|---|---|
| Notificaciones | Se guardan como filas `NotificacionRecordatorio` que el back genera al crear o editar un recordatorio. No hay proceso programado en el servidor | CU-07 y CU-08 piden "programar las notificaciones" al guardar; CU-09 v2 es solo dentro de la app. El informe inicial hablaba de un scheduler, pero los casos de uso v2 lo reemplazan |
| "Verificar periódicamente" (CU-09, paso 1) | Lo hace el front: con la app abierta vuelve a pedir la agenda de hoy cada 60 segundos | No hace falta nada en el servidor para avisar dentro de la app |
| Herencia | `Recordatorio` abstracta con estrategia `JOINED`; `RecordatorioMedicacion` y `RecordatorioCita` como subclases | Respeta el DC y permite columnas obligatorias. Claude Code puede proponer otra en el plan si la justifica |
| Baja | Eliminación física del recordatorio y de todas sus notificaciones | CU-07 y CU-08 dicen "eliminar"; el DC no tiene fecha de baja en `Recordatorio` |
| Dueño del dato | El recordatorio pertenece a la `PersonaMayor` del token. Si no es de quien lo pide, el back responde 404 (no revela que existe) | Contrato de login, sección 2 (identidad), y regla "cada módulo verifica que el dato pertenezca a quien lo pide" |
| "Atrasada" | No es un estado guardado: el DC solo tiene `PENDIENTE`, `TOMADA` y `VISTA`. El back la calcula y la manda como `atrasada` | El prototipo muestra "Pasó hace 2 h" |
| Estado de una notificación | Un solo endpoint cambia el estado (`PUT .../notificaciones/{id}/estado`). Sirve para marcar la toma, deshacerla y marcar la cita como vista | Menos rutas y no hace falta ningún método HTTP nuevo en el cliente |
| Frecuencia | Obligatoria al crear una medicación (CU-07 y DC). El front la ofrece como selector con "Todos los días" preseleccionado | El Figma no la muestra en el formulario (sección 12) |
| Fecha de inicio | Opcional en la API; por defecto, hoy. El formulario no la pide en esta etapa | Está en el DC (`fecha_inicio`) pero no en el formulario de CU-07 |
| Duración | Entre 1 y 365 días | CU-07 pide "mayor a cero"; el tope de 365 evita generar miles de filas por error. Propuesta: confirmar |
| Agrupar avisos | Los recordatorios con la misma `fechaHoraProgramada` (a nivel hora y minuto) forman una misma franja y se avisan juntos. Lo aplica quien muestra el aviso (tarjeta "Para hoy" y saludo de Alma), no la API | CU-09, regla de negocio |
| Fechas | `LocalDate`, `LocalTime` y `LocalDateTime` de Argentina, sin zona en el JSON | Convención de la API; `AlmaApplication` fija la zona y no se quita |

---

## 3. Convenciones de la API

Valen las del contrato de login (sección 3). Para este módulo:

- Todo cuelga de `/api/recordatorios`.
- Fechas: `"2026-10-28"` (`yyyy-MM-dd`). Fecha y hora: `"2026-10-28T09:00:00"`, sin zona ni desplazamiento. Hora: `"09:00:00"`. Las requests también aceptan `"09:00"` y `"2026-10-28T09:00"`.
- El front muestra las horas tal como llegan: no las convierte de zona. Para saber si algo está atrasado usa el campo `atrasada` del back, no el reloj del navegador.
- Enums en `MAYUSCULAS_CON_GUION_BAJO`: `FrecuenciaToma` (`DIARIA`, `DIA_POR_MEDIO`, `SEMANAL`) y `EstadoNotificacion` (`PENDIENTE`, `TOMADA`, `VISTA`).
- `tipo` es un campo de las respuestas que distingue las dos clases: `"MEDICACION"` o `"CITA"`. No es un atributo del DC: es solo un discriminador de la API.
- Los textos que manda la persona se guardan sin espacios al inicio y al final.

---

## 4. Modelo de datos

Según el DC. No se agregan campos; si se necesitan, primero se actualiza el DC.

- `Recordatorio` (abstracta): `id`, `fechaCreacion` (`LocalDateTime`, la completa el back al crear), `personaMayor` (muchos a uno).
- `RecordatorioMedicacion`: `nombreMedicamento`, `dosis`, `horaToma` (`LocalTime`), `frecuencia` (`FrecuenciaToma`), `fechaInicio` (`LocalDate`), `duracionDias` (`int`).
- `RecordatorioCita`: `titulo`, `especialidad`, `medico`, `lugar`, `fechaHora` (`LocalDateTime`), `indicacionesPrevias` (opcional).
- `NotificacionRecordatorio`: `id`, `recordatorio` (muchos a uno), `fechaHoraProgramada` (`LocalDateTime`), `estado` (`EstadoNotificacion`), `fechaHoraToma` (`LocalDateTime`, solo con `TOMADA`).

Recomendado (no está en el DC, pero es una restricción de base de datos, no un dato nuevo): clave única en `NotificacionRecordatorio` sobre (`recordatorio`, `fechaHoraProgramada`), para que una toma no se duplique.

---

## 5. Reglas de negocio

### 5.1 Calendario de tomas de una medicación

El salto entre tomas depende de la frecuencia: `DIARIA` = 1 día, `DIA_POR_MEDIO` = 2 días, `SEMANAL` = 7 días.

Hay una toma en `fechaInicio + k × salto` (a la hora `horaToma`) para cada `k = 0, 1, 2, …` mientras `k × salto < duracionDias`. Es decir, `duracionDias` es la ventana de días desde `fechaInicio`, y la cantidad de tomas es `ceil(duracionDias / salto)`.

| Ejemplo (inicio 28/10, 09:00) | Tomas |
|---|---|
| `DIARIA`, 31 días | 31 (del 28/10 al 27/11) |
| `DIA_POR_MEDIO`, 31 días | 16 (28/10, 30/10, …, 27/11) |
| `SEMANAL`, 31 días | 5 (28/10, 4/11, 11/11, 18/11, 25/11) |

### 5.2 Alta

- **Medicación:** se generan las tomas del 5.1 como `PENDIENTE`. Las tomas cuya hora ya pasó (por ejemplo, la de hoy a las 08:00 si se crea a las 10:00) no se generan.
- **Cita:** se genera una sola notificación `PENDIENTE` en `fechaHora`.
- `fechaCreacion` la pone el back.

### 5.3 Edición

- **Medicación:** se recalcula el calendario con los datos nuevos y:
  1. se borran las notificaciones `PENDIENTE` con `fechaHoraProgramada` posterior a ahora;
  2. se crean como `PENDIENTE` las tomas del calendario nuevo posteriores a ahora que no tengan ya una notificación en ese mismo instante;
  3. las notificaciones que ya pasaron o que ya están `TOMADA` no se tocan (queda el historial).

  > **Nota (implementación):** el resultado es el de los pasos 1 a 3, pero sin borrar y recrear todo: se calcula el calendario nuevo, se **conservan** las notificaciones `PENDIENTE` futuras que coinciden en el mismo instante (**mantienen su id**), se borran solo las `PENDIENTE` futuras que ya no están en el calendario y se crean las que faltan. Así un `notificacionId` que el front ya tenía sigue siendo válido después de editar si ese aviso sigue en el calendario.
- **Cita:** si cambió `fechaHora`, se borra su notificación y se crea una nueva `PENDIENTE`. Si no cambió, la notificación queda como estaba (con su estado).
- Un recordatorio se edita por la ruta de su tipo: editar como medicación una cita (o al revés) responde 404.

### 5.4 Baja

Se borra el recordatorio y todas sus notificaciones, pasadas o futuras. Es irreversible: el front pide confirmación antes (CU-07 y CU-08).

### 5.5 Estados de una notificación

| Tipo | Estados posibles | Cambio |
|---|---|---|
| Medicación | `PENDIENTE` ↔ `TOMADA` | Tomada: `estado = TOMADA` y `fechaHoraToma` = ahora. Deshacer: `estado = PENDIENTE` y `fechaHoraToma = null` |
| Cita | `PENDIENTE` ↔ `VISTA` | `fechaHoraToma` queda siempre en `null` |

- Pedir el estado que la notificación ya tiene no cambia nada y responde 200 (si ya estaba `TOMADA`, conserva su `fechaHoraToma` original).
- Se puede marcar una toma como tomada antes de su hora.
- Pedir un estado que no corresponde al tipo (por ejemplo, `VISTA` en una medicación) responde 400 con el error del campo `estado`.

### 5.6 `atrasada`

`atrasada` es `true` solo cuando la notificación es de una medicación, está `PENDIENTE` y su `fechaHoraProgramada` ya pasó. Para las citas es siempre `false`.

### 5.7 Avisos (CU-09)

Los avisos salen de la agenda de hoy. Lo que decide cada pantalla:

- La tarjeta "Para hoy" muestra la primera notificación `PENDIENTE` del día, en orden de hora (las atrasadas cuentan: "si no se marca, se vuelve a recordar"), con "Ya la tomé" para medicaciones o el acceso al detalle para citas.
- Si hay varias en la misma franja horaria, se muestran como un único aviso.
- Cerrar la tarjeta con la X la oculta "por ahora": no cambia ningún estado y vuelve a aparecer la próxima vez que se carga la agenda de hoy o se ingresa a la app.
- Un recordatorio sin atender no se pierde: sigue `PENDIENTE` y aparece en la agenda.

### 5.8 Validaciones

**Medicación (`MedicacionRequest`)**

| Campo | Regla | Mensaje |
|---|---|---|
| `nombreMedicamento` | Obligatorio, hasta 100 caracteres | "Escribí el nombre del medicamento." / "El nombre es demasiado largo." |
| `dosis` | Obligatoria, hasta 100 caracteres | "Indicá la dosis." / "La dosis es demasiado larga." |
| `horaToma` | Obligatoria | "Elegí la hora." |
| `frecuencia` | Obligatoria | "Elegí cada cuánto la tomás." |
| `fechaInicio` | Opcional. Por defecto, hoy. En el alta no puede ser anterior a hoy. En la edición se acepta la que ya tenía; si la cambia, no puede ser anterior a hoy | "La fecha de inicio no puede ser anterior a hoy." |
| `duracionDias` | Obligatoria, de 1 a 365 | "Indicá cuántos días." / "Los días tienen que ser entre 1 y 365." |

> **Nota (`fechaInicio` en la edición):** si el `PUT` no manda `fechaInicio`, se **conserva la que el recordatorio ya tenía** (no se cambia a hoy). "Por defecto, hoy" vale solo para el alta.

**Cita (`CitaRequest`)**

| Campo | Regla | Mensaje |
|---|---|---|
| `titulo` | Obligatorio, hasta 100 caracteres | "Escribí un título." |
| `especialidad` | Obligatoria, hasta 100 caracteres | "Escribí la especialidad." |
| `medico` | Obligatorio, hasta 100 caracteres | "Escribí el nombre del médico." |
| `lugar` | Obligatorio, hasta 150 caracteres | "Escribí el lugar." |
| `fechaHora` | Obligatoria y posterior al momento actual, tanto al crear como al editar (CU-08) | "Elegí la fecha y la hora." / "La fecha y la hora tienen que ser posteriores a este momento." |
| `indicacionesPrevias` | Opcional, hasta 500 caracteres | "Las indicaciones son demasiado largas." |

Un campo obligatorio con solo espacios cuenta como vacío. Una hora o fecha con formato inválido responde 400 con el texto general "Revisá los datos ingresados." (sin lista de campos).

---

## 6. Endpoints

Todos exigen sesión de `PERSONA_MAYOR`. La persona sale del token (`sub`), nunca de la URL ni del cuerpo.

### 6.1 `GET /api/recordatorios/agenda`

Notificaciones de un rango de fechas, con los datos de su recordatorio. Sirve a la lista, al calendario y al aviso de hoy.

**Parámetros**

| Parámetro | Obligatorio | Descripción |
|---|---|---|
| `desde` | Sí | Fecha (`yyyy-MM-dd`), inclusive |
| `hasta` | Sí | Fecha (`yyyy-MM-dd`), inclusive. No puede ser anterior a `desde` ni superar los 62 días de rango (contando los dos extremos: del 28/10 al 28/12 son 62 días y es válido) |
| `tipo` | No | `MEDICACION` o `CITA`. Sin este parámetro trae los dos |

**200 OK:** lista ordenada por `fechaHoraProgramada` (y por `notificacionId` si empatan). Vacía si no hay nada.

```json
[
  {
    "notificacionId": 41,
    "fechaHoraProgramada": "2026-10-28T09:00:00",
    "estado": "TOMADA",
    "fechaHoraToma": "2026-10-28T09:12:00",
    "atrasada": false,
    "recordatorio": {
      "id": 7,
      "tipo": "MEDICACION",
      "fechaCreacion": "2026-10-27T18:03:11",
      "nombreMedicamento": "Enalapril",
      "dosis": "1 pastilla",
      "horaToma": "09:00:00",
      "frecuencia": "DIARIA",
      "fechaInicio": "2026-10-27",
      "duracionDias": 31
    }
  },
  {
    "notificacionId": 58,
    "fechaHoraProgramada": "2026-10-29T10:30:00",
    "estado": "PENDIENTE",
    "fechaHoraToma": null,
    "atrasada": false,
    "recordatorio": {
      "id": 12,
      "tipo": "CITA",
      "fechaCreacion": "2026-10-26T11:00:00",
      "titulo": "Control anual",
      "especialidad": "Cardiología",
      "medico": "Dra. Pérez",
      "lugar": "Consultorio Centro",
      "fechaHora": "2026-10-29T10:30:00",
      "indicacionesPrevias": "Llevar los últimos estudios.",
      "notificacionId": 58,
      "estado": "PENDIENTE"
    }
  }
]
```

**Errores:** `400` si falta un parámetro, la fecha no tiene formato válido, `hasta` es anterior a `desde` o el rango supera los 62 días (en estos dos últimos casos, con `errores` sobre `hasta`).

### 6.2 `GET /api/recordatorios/{id}`

Detalle de un recordatorio, para editarlo o para mostrar el detalle de la cita.

**200 OK:** un objeto como el de `recordatorio` de arriba, según su `tipo`. Las citas incluyen `notificacionId` y `estado` (una cita tiene una sola notificación); las medicaciones no.

**Errores:** `404` si no existe o no es de la persona del token.

### 6.3 `POST /api/recordatorios/medicacion`

**Request**
```json
{
  "nombreMedicamento": "Enalapril",
  "dosis": "1 pastilla",
  "horaToma": "09:00",
  "frecuencia": "DIARIA",
  "duracionDias": 31
}
```
`fechaInicio` es opcional (por defecto, hoy).

**201 Created:** el recordatorio creado (`tipo: "MEDICACION"`), con las notificaciones ya generadas (5.2).

**Errores:** `400` por validación (5.8).

### 6.4 `POST /api/recordatorios/citas`

**Request**
```json
{
  "titulo": "Control anual",
  "especialidad": "Cardiología",
  "medico": "Dra. Pérez",
  "lugar": "Consultorio Centro",
  "fechaHora": "2026-10-29T10:30",
  "indicacionesPrevias": "Llevar los últimos estudios."
}
```

**201 Created:** la cita creada (`tipo: "CITA"`), con `notificacionId` y `estado: "PENDIENTE"`.

**Errores:** `400` por validación (5.8).

### 6.5 `PUT /api/recordatorios/medicacion/{id}` y `PUT /api/recordatorios/citas/{id}`

Reciben el objeto completo, con las mismas validaciones que el alta (con la excepción de `fechaInicio` de 5.8), y reprograman las notificaciones según 5.3.

> **Nota:** si el cuerpo no trae `fechaInicio`, se conserva la que ya tenía el recordatorio (ver la nota de 5.8).

**200 OK:** el recordatorio actualizado.

**Errores:** `400` por validación; `404` si no existe, no es de la persona o es del otro tipo.

### 6.6 `DELETE /api/recordatorios/{id}`

Borra el recordatorio y sus notificaciones (5.4).

**204 No Content.**

**Errores:** `404` si no existe o no es de la persona.

### 6.7 `PUT /api/recordatorios/notificaciones/{id}/estado`

Cambia el estado de una notificación (5.5). `{id}` es el `notificacionId`.

**Request**
```json
{ "estado": "TOMADA" }
```

**200 OK:** el ítem de la agenda actualizado (misma forma que los de 6.1).

**Errores:** `400` si falta `estado` ("Indicá el estado.") o no corresponde al tipo (campo `estado`: "Ese estado no corresponde a este recordatorio."); `404` si no existe o no es de la persona.

---

## 7. Reglas de acceso

| Ruta | Quién puede |
|---|---|
| `/api/recordatorios/**` | Solo `PERSONA_MAYOR`. Un `ADMINISTRADOR` recibe `403` |

Sin token o con token vencido: `401`, como en el resto de `/api`.

---

## 8. Errores y mensajes compartidos

Formato Problem Details, igual que en el contrato de login (sección 7). Este módulo usa estos códigos:

| Status | Cuándo | `detail` |
|---|---|---|
| 400 | Validación (campos, rango de fechas, estado que no corresponde) | "Revisá los datos ingresados." más la lista `errores` |
| 401 | Token faltante, inválido o vencido | "Tu sesión terminó. Ingresá de nuevo." |
| 403 | Rol sin permiso | "No tenés permiso para ver esta pantalla." |
| 404 | No existe o no es de la persona | "No encontramos lo que buscás." |
| 500 | Error interno | "Algo salió mal. Probá de nuevo en un rato." |

### Dónde vive cada texto (para no repetirlos)

**Regla general: un texto se escribe una sola vez, en un solo lugar, y el resto del código lo importa.**

**Back**

- Los textos generales (los de la tabla de arriba) ya viven en `common/MensajesError` y los usa `GlobalExceptionHandler`. El módulo no los vuelve a escribir.
- Los textos propios del módulo (los mensajes de 5.8 y los de la sección 8.1) van en una clase de constantes del módulo, `recordatorios/MensajesRecordatorio`. Los DTO las referencian (`message = MensajesRecordatorio.NOMBRE_OBLIGATORIO`), así no quedan textos sueltos dentro de las anotaciones.
- Las reglas que Bean Validation no puede expresar (rango de fechas inválido, estado que no corresponde, `fechaInicio` en la edición) se lanzan con una excepción compartida nueva, `common/exception/DatosInvalidosException`, que lleva la lista de `ErrorCampo`. `GlobalExceptionHandler` la convierte en el mismo 400 con `errores` que ya produce Bean Validation. Cualquier otro módulo (por ejemplo, el panel de operador) la puede usar.

**Front**

- Nuevo `src/lib/mensajes.ts`: los textos generales (sesión terminada, sin conexión, algo salió mal, no encontrado, sin permiso). Hoy están sueltos: `MENSAJE_SESION_VENCIDA` en `lib/api.ts` y el texto de conexión dentro de `LoginPage`. `lib/api.ts` y las pantallas importan de acá.
- Nuevo `src/lib/errores.ts`, con dos funciones reutilizables por cualquier módulo:
  - `mensajeDeError(error, porEstado?)`: recibe un `ApiError` y devuelve el texto a mostrar. `status` 0 = sin conexión, 401 = sesión terminada, 403 = sin permiso, 404 = no encontrado, 5xx = algo salió mal. Para el 400, usa el texto que le pase la pantalla (`porEstado`) o, si no, el `detalle` de la API.
  - `aplicarErroresDeCampos(error, setError)`: toma la lista `errores` de un `ApiError` y la vuelca en los campos de `react-hook-form`, para que cada mensaje aparezca junto a su campo.
- Los textos propios del módulo (los de 8.1 y las etiquetas de los chips) van en `features/recordatorios/mensajes.ts`.
- La presentación ya existe y se reutiliza: `Alert` (aviso arriba) y el prop `error` de `CampoTexto` (mensaje junto al campo). No se crean componentes de error nuevos.
- Los textos que ya están escritos a mano en el login se pueden pasar a `lib/mensajes.ts` en una tarea aparte; no se mezcla con este módulo.

### 8.1 Textos propios del módulo

| Cuándo | Texto |
|---|---|
| El alta o la edición de una medicación da 400 (aviso arriba del formulario, CU-07) | "Revisá los datos del recordatorio e intentá nuevamente." |
| El alta o la edición de una cita da 400 (aviso arriba del formulario, CU-08) | "Revisá los datos de la cita e intentá nuevamente." |
| Recordatorio creado | "Tu recordatorio quedó creado." |
| Recordatorio actualizado | "Tu recordatorio quedó actualizado." |
| Recordatorio eliminado | "Tu recordatorio fue eliminado." |
| Pregunta antes de eliminar | "¿Querés eliminar este recordatorio?" y debajo "También se van a borrar sus avisos." (botones "Sí, eliminar" y "Cancelar") |
| Lista sin medicaciones hoy | "Hoy no tenés medicación para tomar." |
| Lista sin citas próximas | "No tenés citas próximas." |

Los textos de confirmación de los CU ("confirmar al usuario que el recordatorio fue creado") no los fijan los documentos: los de la tabla son propuestos.

---

## 9. Back: qué implementar

Paquete base: `ar.edu.utn.frm.gelab.alma`. El módulo vive en `recordatorios/`, con el mismo patrón que `persona/` (`controller/`, `service/`, `repository/`, `domain/`, `dto/`).

- `domain/`: `Recordatorio`, `RecordatorioMedicacion`, `RecordatorioCita`, `NotificacionRecordatorio`, `FrecuenciaToma`, `EstadoNotificacion`.
- `repository/`: `RecordatorioRepository`, `NotificacionRecordatorioRepository` (siempre filtrados por persona).
- `dto/`: `MedicacionRequest`, `CitaRequest`, `EstadoNotificacionRequest`, `MedicacionResponse`, `CitaResponse`, `ItemAgendaResponse` y el enum `TipoRecordatorio` (`MEDICACION`, `CITA`), que usa el parámetro `tipo` de la agenda y el campo `tipo` de las respuestas.
- `service/`: `RecordatorioService` (alta, edición, baja, consulta y verificación de dueño) y `ProgramadorNotificaciones` (calcula el calendario del 5.1 y regenera las notificaciones del 5.3).
- `controller/`: `RecordatorioController`, con `@Tag(name = "Recordatorios")` y `@Operation` en cada endpoint.
- `MensajesRecordatorio`: constantes de la sección 8.

**Para el módulo de Chat:** `RecordatorioService` expone un método público (por ejemplo, `agenda(Long cuentaId, LocalDate desde, LocalDate hasta, TipoRecordatorio tipo)`) que devuelve los mismos `ItemAgendaResponse`. El chat arma el saludo de CU-02 llamando a ese servicio, sin pasar por HTTP: los módulos se comunican solo a través de sus servicios.

**Cambios en archivos compartidos (avisar antes de tocarlos)**

| Archivo | Cambio |
|---|---|
| `config/SecurityConfig.java` | Una línea: `.requestMatchers("/api/recordatorios/**").hasRole("PERSONA_MAYOR")`, **antes** de `.requestMatchers("/api/**").authenticated()` |
| `common/GlobalExceptionHandler.java` | Handler de `DatosInvalidosException` → 400 con `errores` |
| `common/exception/DatosInvalidosException.java` | Archivo nuevo (compartido) |
| `docs/casos-de-uso.md` | Estado de implementación: Back de CU-07, CU-08 y CU-09 |
| `CLAUDE.md` | Enlazar este contrato en "Documentos" |

**No se tocan:** `pom.xml`, `application.properties`, `AlmaApplication.java` (conserva el `TimeZone.setDefault`), ni `auth/`, `persona/` o `administrador/`. No se agrega `@EnableScheduling`.

**Checklist**

1. Entidades y herencia (4), con claves y restricciones.
2. `ProgramadorNotificaciones` con los ejemplos de 5.1 como casos de prueba.
3. Servicio y controlador con las rutas de la sección 6 y la verificación de dueño en cada consulta.
4. Validaciones y mensajes (5.8, sección 8), con `DatosInvalidosException` y su handler.
5. Línea de `SecurityConfig` (sección 7).
6. Datos de prueba (sección 11).
7. Pruebas de la sección 13.

---

## 10. Front: qué implementar

**Antes de empezar** (algunos son cambios en archivos compartidos: avisar):

- La navegación y el layout de la persona mayor (tanda aparte) tienen que existir, o al menos una ruta `/recordatorios` dentro del layout.
- `lib/api.ts`: agregar `"DELETE"` a los métodos permitidos.
- Montar el `QueryClientProvider` de `@tanstack/react-query` (está instalado pero sin usar).
- `components/ui/`: agregar `select`, `textarea` y `tabs` (shadcn). `@base-ui/react` ya está, así que no suman dependencias, pero igual se avisa.
- `lib/mensajes.ts` y `lib/errores.ts` (sección 8).

**Estructura sugerida** (`features/recordatorios/`)

```
tipos.ts                       tipos según las respuestas de la sección 6
api.ts                         agenda(), detalle(), crear…(), editar…(), eliminar(), cambiarEstado()
schemas.ts                     esquemas zod de los dos formularios (mismos textos que 5.8)
mensajes.ts                    textos del módulo (8.1) y etiquetas de frecuencia y estado
useRecordatorios.ts            consultas y acciones con TanStack Query
RecordatoriosPage.tsx          pestañas Lista y Calendario
ListaRecordatorios.tsx
TarjetaRecordatorio.tsx
CalendarioRecordatorios.tsx    armado con date-fns, sin librería nueva
NuevoRecordatorioPage.tsx      alta, con selector Medicación / Cita médica
EditarRecordatorioPage.tsx
FormularioMedicacion.tsx
FormularioCita.tsx
DetalleCitaPage.tsx
DialogoEliminarRecordatorio.tsx
```

**Rutas** (dentro del `ProtectedRoute` de `PERSONA_MAYOR`)

| Ruta | Pantalla |
|---|---|
| `/recordatorios` | Lista y Calendario (las pestañas no cambian la URL) |
| `/recordatorios/nuevo` | Alta, con las dos opciones arriba: "Medicación" y "Cita médica" |
| `/recordatorios/:id/editar` | Edición; el tipo sale del recordatorio cargado |
| `/recordatorios/:id` | Detalle de la cita (CU-09). Al abrirla, si está `PENDIENTE`, se marca `VISTA` |

**Datos**

- Agenda de hoy: `agenda?desde=hoy&hasta=hoy`, con nueva consulta cada 60 segundos y al volver a la ventana. Es la base del aviso (5.7) y de la sección "Medicación de hoy". Se expone como un hook reutilizable (por ejemplo, `useAgendaDeHoy`) para que el módulo de Chat arme su tarjeta "Para hoy" sin reescribirlo.
- Citas próximas de la lista: `agenda?tipo=CITA&desde=hoy&hasta=hoy+30 días`.
- Calendario: `agenda?tipo=CITA` con el rango del mes visible, para marcar con un punto los días con cita. Al elegir un día, se piden todas las notificaciones de ese día.
- Después de crear, editar, eliminar o cambiar un estado, se invalidan las consultas de agenda.

**Pantallas** (según el Figma, ajustado por la sección 12)

- **Lista:** "Medicación de hoy" y las citas médicas, cada una con su estado como texto e ícono (nunca solo color): "Pendiente", "Tomada a las 09:12" con botón "Deshacer", "Pasó hace 2 h" para las atrasadas, y "Vista" en las citas. Los botones: "Ya la tomé" y "Deshacer".
- **Calendario:** mes con flechas para cambiar, un punto en los días con cita y, debajo, el detalle del día elegido. Cada día es un botón alcanzable con el teclado.
- **Formularios:** etiquetas siempre visibles; errores junto al campo con ícono y texto y, arriba, el aviso del 8.1.
  - Medicación: nombre, dosis, hora, **frecuencia** (selector: "Todos los días", "Un día por medio", "Una vez por semana"; preseleccionado "Todos los días") y duración (botones − y + con un valor de 1 a 365; por defecto, 31 días). Botón "Guardar recordatorio".
  - Cita: título, especialidad, médico, lugar, fecha, hora e indicaciones previas (opcional).
- **Eliminar:** diálogo de confirmación (sobre el `Dialog` existente) con el texto del 8.1.
- Fechas con `date-fns` y la configuración regional en español: `dd/MM/yyyy` y `HH:mm`.

**Accesibilidad** (público: personas mayores)

- Controles de al menos 44 px (el prototipo usa 48 px).
- Todo se usa con teclado; foco visible; el diálogo devuelve el foco al botón que lo abrió.
- Las pestañas, el calendario y los estados se anuncian bien a un lector de pantalla (rol y nombre accesible).
- Que se vea bien con letra "Muy grande" y con alto contraste.
- Los errores se escriben en lenguaje simple, sin jerga ni códigos.

---

## 11. Datos de prueba

Los carga el back al arrancar si `alma.seed.enabled=true` (solo en local), en una clase aparte de `DatosDePrueba` (que no se edita) y que corre después de esta. Es idempotente: solo carga si la persona todavía no tiene recordatorios. Las fechas son relativas al día de arranque.

| Persona | Datos |
|---|---|
| `ana` | Medicaciones "Pastilla de la mañana" (09:00), "Pastilla del almuerzo" (13:00) y "Pastilla de la noche" (21:00), todas `DIARIA` por 31 días, dosis "1 pastilla"; y una cita "Control anual" (Cardiología, Dra. Pérez, Consultorio Centro) dentro de 3 días a las 10:30 |
| `carlos` | Una medicación `DIARIA` por 31 días a las 08:00. Sirve para probar que no ve los recordatorios de `ana` |

---

## 12. Diferencias con el Figma

El Figma es una referencia: puede cambiar para mejor, pero donde difiere en funcionalidad ganan los documentos.

| El Figma | Los documentos | Qué se hace |
|---|---|---|
| El formulario de medicación no tiene "frecuencia" (aunque la lista dice "todos los días") | CU-07 y DC: la frecuencia es obligatoria | Se agrega el selector con "Todos los días" preseleccionado. Conviene actualizar el Figma |
| El formulario no tiene fecha de inicio | DC: `fecha_inicio` | Opcional en la API; por defecto, hoy |
| El calendario marca con un punto solo las citas | — | Igual: el punto marca un día con cita |
| No hay formulario de cita en escritorio, ni detalle de la cita ni confirmación al eliminar | CU-08 y CU-09 lo indican como "sin crear todavía en el Figma" | Se arman con los mismos componentes y reglas |

---

## 13. Pruebas y criterio de "listo"

**Back (automáticas o con Swagger/curl)**

- Sin token: `401`. Token de `admin` en `/api/recordatorios/**`: `403`.
- Alta de medicación `DIARIA` por 31 días: `201` y se generan las notificaciones (sin las de hoy que ya pasaron). `DIA_POR_MEDIO` y `SEMANAL`: 16 y 5 notificaciones (5.1).
- Medicación con duración 0 o 366, sin hora o sin frecuencia: `400` con `errores`.
- Cita con fecha pasada: `400`. Cita futura: `201` con una notificación `PENDIENTE`.
- `carlos` pide, edita o borra un recordatorio de `ana`: `404`.
- Editar una medicación: las notificaciones futuras `PENDIENTE` se regeneran, y las tomadas y las pasadas quedan.
- Editar una cita cambiando la fecha: la notificación nueva queda `PENDIENTE`.
- `DELETE`: `204` y desaparecen sus notificaciones.
- Estado: medicación → `TOMADA` (con `fechaHoraToma`) y vuelta a `PENDIENTE` (sin `fechaHoraToma`); cita → `VISTA`; `VISTA` en una medicación: `400`; repetir el mismo estado: `200` sin cambios.
- `agenda`: filtra por rango y por `tipo`, solo trae lo de la persona del token, y rechaza rangos inválidos con `400`. `atrasada` es `true` solo en medicaciones `PENDIENTE` ya pasadas.
- Las pruebas de login que ya existen siguen pasando.

**Front (manuales)**

- Alta de medicación y de cita, con el aviso y los errores por campo cuando los datos son inválidos.
- La lista muestra pendiente, tomada (con "Deshacer") y atrasada; las citas muestran "Vista" después de abrirlas.
- El calendario marca los días con cita, se cambia de mes y se recorre con el teclado.
- Editar carga los datos actuales; eliminar pide confirmación.
- Una toma que vence con la app abierta aparece sin recargar la página (hasta 60 segundos).
- Sesión vencida: vuelve a `/login` con "Tu sesión terminó. Ingresá de nuevo."
- Con el back apagado: mensaje simple y sin pantalla en blanco.
- Todo funciona en celular y en escritorio, con letra "Muy grande" y con alto contraste.

**Listo cuando:** con `docker compose up -d`, el back corriendo y `pnpm dev`, `ana` ve sus recordatorios de prueba, puede crear, editar y eliminar uno de cada tipo, marcar y deshacer una toma, y `carlos` no ve nada de lo de `ana`.

---

## 14. Pendientes para decidir

1. **Tope de duración (365 días):** propuesta, a confirmar.
2. **Textos de confirmación** de alta, edición y baja (8.1): propuestos, a revisar.
3. **Formato de las citas en escritorio y detalle de la cita:** falta diseñarlos en el Figma.
4. **Avisos de días anteriores sin tomar:** quedan fuera de esta etapa; si se quieren, se agrega un rango en la tarjeta o un aviso aparte.

*(Resueltos: no hay scheduler ni push; `atrasada` se calcula; herencia `JOINED`; el alcance de la navegación es solo la persona mayor; el panel de operador lo arma la otra persona del equipo, en paralelo.)*

---

## 15. Cómo usar este documento con Claude Code

**Back**
> Leé `CLAUDE.md` y `docs/contrato-recordatorios.md`. Mostrame primero el plan para implementar la parte del back según la sección 9, y no escribas código hasta que lo apruebe.

**Front**
> Leé `CLAUDE.md` y `docs/contrato-recordatorios.md`. Mostrame primero el plan para implementar la parte del front según la sección 10, y no escribas código hasta que lo apruebe.

Si Claude Code propone algo que contradice el contrato, gana el contrato. Si hay que cambiarlo, se edita este archivo.

---

## 16. Historial de cambios

- **v1 (28/10/2026):** primera propuesta, contra el commit `19764e1`.
- **v2 (28/10/2026):** notas de implementación del back: `fechaInicio` se conserva si el `PUT` no la manda (5.8 y 6.5); la edición de una medicación conserva el id de las notificaciones que coinciden (5.3); el rango de la agenda cuenta los dos extremos (6.1).
