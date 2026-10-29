# Contrato del panel de operador · Alma Acompaña

**Estado:** propuesta para acordar. Una vez aprobada, es la referencia única para el back y el front del panel de operador (CU-10 y CU-11). Si algo cambia, se cambia primero acá.

**Fuente de verdad:** casos de uso CU-10, CU-11 y CU-12 (`docs/casos-de-uso.md`) y el diagrama de clases (`docs/Diagrama_de_clases/`). El Figma (pantallas "Desktop · Operador · …") es ilustrativo: orienta el formato, los colores y los componentes, pero si difiere de los documentos ganan los documentos.

**Rama:** `feature/panel-operador`, creada desde `main`. Esta rama cubre CU-10 (catálogo de ejercicios) y CU-11 (microcápsulas), el menú lateral del panel y la sección Perfil. El login (CU-12) ya está hecho.

**Convenciones heredadas** de `docs/contrato-login.md`: rutas bajo `/api`, JSON en UTF-8 con `camelCase`, enums en `MAYUSCULAS_CON_GUION_BAJO`, fechas ISO 8601 (`yyyy-MM-dd` para fechas sin hora), ids `Long`, errores en Problem Details (`application/problem+json`), textos para la persona en español con voseo, token Bearer.

---

## 1. Decisiones

| Tema | Decisión | Motivo |
|---|---|---|
| Acceso | Todo `/api/admin/**` es solo `ADMINISTRADOR` (ya está en `SecurityConfig`) | Contrato de login, sección 6 |
| Baja de ejercicio | **Baja lógica**: se marca `fechaBaja` con la fecha de hoy. No se borra la fila | DC (`Ejercicio.fecha_baja`), CU-05 ("ejercicios activos") y el historial de intentos (`IntentoEjercicio`) |
| Baja de microcápsula | Baja lógica con `fechaBaja` | CU-11: no se elimina físicamente; los favoritos siguen viéndola |
| Qué lista el panel | Solo ejercicios y microcápsulas sin `fechaBaja` | Lo dado de baja ya no se gestiona |
| Publicación de microcápsula | Inmediata al guardar. No hay borrador. `fechaPublicacion` es obligatoria y no puede ser futura; el front la propone con la fecha de hoy | CU-11 |
| Explicación del ejercicio | Opcional. Es el texto que ve la persona después de responder. Ejemplo: "Este ejercicio trata sobre completar con la palabra correcta el refrán que te aparece." | CU-10 / DC |
| Texto a memorizar | Obligatorio solo si la categoría es `MEMORIA`. En las demás categorías el back lo guarda como `null` | CU-10 |
| Imágenes | Se suben desde la computadora de la persona operadora (selector de archivos). El back las guarda en una carpeta del servidor y la base guarda solo la ruta | DC (`imagen: String, ruta`) y Figma ("Elegir imagen") |
| Listados | Sin paginación en el MVP (el catálogo es chico). El filtro por categoría es del lado del back | Simple |
| Perfil | Sección del menú lateral que muestra los datos de la persona administradora con `GET /api/auth/me`. No hay endpoint nuevo. El diseño es propuesto (no hay pantalla en el Figma) | Pedido del equipo |
| Imágenes en las opciones | Cada opción de un ejercicio puede tener una imagen opcional. Aunque las pantallas del Figma muestran las opciones solo con texto, CU-10 y el DC la contemplan, y el juego "Intruso con fotos" la necesita. El texto de la opción sigue siendo obligatorio | CU-10, DC |
| Pantallas adaptables | El panel se ve bien en computadora y en celular. El Figma del operador es solo de escritorio; la versión de celular es propuesta (sección 6) | Pedido del equipo |
| Explicación y publicación | La propuesta de CU-05 (mostrar la explicación junto al aviso de acierto o error) queda aceptada y se documenta al actualizar los casos de uso | Equipo |

---

## 2. Enumeraciones

| Enum | Valores | Texto en pantalla |
|---|---|---|
| `CategoriaEjercicio` | `REFRAN`, `SERIE`, `ASOCIACION`, `MEMORIA` | Refranes, Series, Asociaciones, Memoria |
| `FormatoEjercicio` | `OPCION_MULTIPLE`, `ORDENAR` | Elegir una opción (en la tabla: "Opción múltiple"), Ordenar los pasos |
| `NivelDificultad` | `BASICO`, `INTERMEDIO`, `AVANZADO` | Básico, Intermedio, Avanzado |
| `CategoriaMicrocapsula` | `PREVENCION_CAIDAS`, `CUIDADO_HOGAR`, `ACTIVIDAD_FISICA`, `ALIMENTACION`, `SUENIO_DESCANSO`, `BIENESTAR_EMOCIONAL`, `RECOMENDACIONES` | Prevención de caídas, Cuidado del hogar, Actividad física, Alimentación, Sueño y descanso, Bienestar emocional, Recomendaciones |

> El Figma muestra la categoría "Lista de palabras"; los documentos dicen `MEMORIA`. Se usa `MEMORIA`.

---

## 3. Endpoints

### 3.1 `GET /api/admin/panel/resumen`

**200 OK**
```json
{ "ejerciciosActivos": 48, "microcapsulasVigentes": 12 }
```
Cuenta los ejercicios y las microcápsulas sin `fechaBaja`.

### 3.2 Ejercicios (CU-10)

**`GET /api/admin/ejercicios`** · Parámetro opcional `categoria` (valor del enum). Devuelve los ejercicios activos, del más nuevo al más viejo (por `fechaAlta` y luego por `id`).

```json
[
  {
    "id": 7,
    "categoria": "REFRAN",
    "consigna": "Completá el refrán: «Más vale pájaro en mano…»",
    "formato": "OPCION_MULTIPLE",
    "nivelDificultad": "BASICO"
  }
]
```

**`GET /api/admin/ejercicios/{id}`** · Devuelve el detalle completo, con las opciones. `404` si no existe o está dado de baja.

```json
{
  "id": 7,
  "categoria": "REFRAN",
  "formato": "OPCION_MULTIPLE",
  "consigna": "Completá el refrán: «Más vale pájaro en mano…»",
  "nivelDificultad": "BASICO",
  "explicacion": "Este ejercicio trata sobre completar con la palabra correcta el refrán que te aparece.",
  "textoAMemorizar": null,
  "fechaAlta": "2026-10-28",
  "opciones": [
    { "id": 21, "texto": "…que cien volando", "esCorrecta": true,  "imagen": null, "ordenCorrecto": null },
    { "id": 22, "texto": "…que dos en el aire", "esCorrecta": false, "imagen": null, "ordenCorrecto": null },
    { "id": 23, "texto": "…que uno en la mano", "esCorrecta": false, "imagen": null, "ordenCorrecto": null }
  ]
}
```

> En `opciones[].imagen`: `null` significa que esa opción no tiene imagen (lo normal en un refrán). Si la tiene, es la ruta que devolvió la subida de imágenes (sección 3.4), por ejemplo `"/imagenes/3f2a9c1e-….png"`. Las pantallas del Figma muestran las opciones solo con texto, pero CU-10 dice que cada opción admite una imagen opcional y el juego "Intruso con fotos" las usa. `ordenCorrecto` es `null` en opción múltiple y un número en ordenar; `esCorrecta` solo importa en opción múltiple.

**`POST /api/admin/ejercicios`** · Crea un ejercicio. **201 Created** con el mismo cuerpo que el detalle. `fechaAlta` la pone el back (hoy).

Cuerpo:
```json
{
  "categoria": "REFRAN",
  "formato": "OPCION_MULTIPLE",
  "consigna": "Completá el refrán: «Más vale pájaro en mano…»",
  "nivelDificultad": "BASICO",
  "explicacion": "…",
  "textoAMemorizar": null,
  "opciones": [
    { "texto": "…que cien volando", "esCorrecta": true,  "imagen": null, "ordenCorrecto": null },
    { "texto": "…que dos en el aire", "esCorrecta": false, "imagen": null, "ordenCorrecto": null },
    { "texto": "…que uno en la mano", "esCorrecta": false, "imagen": null, "ordenCorrecto": null }
  ]
}
```

**`PUT /api/admin/ejercicios/{id}`** · Reemplaza el ejercicio completo (mismo cuerpo que el `POST`). Las opciones se reemplazan todas. **200 OK** con el detalle. `404` si no existe o está dado de baja.

**`DELETE /api/admin/ejercicios/{id}`** · Baja lógica: guarda `fechaBaja` con la fecha de hoy. **204 No Content**. `404` si no existe o ya está dado de baja. No se tocan los intentos ya registrados.

**Validaciones** (todas del lado del back; el front las repite para avisar antes)

| Campo | Regla |
|---|---|
| `categoria`, `formato`, `nivelDificultad` | Obligatorios, valores del enum |
| `consigna` | Obligatoria, hasta 300 caracteres |
| `explicacion` | Opcional, hasta 500 caracteres |
| `textoAMemorizar` | Obligatorio si `categoria` es `MEMORIA` (hasta 1000 caracteres); en las demás categorías se guarda `null` |
| `opciones[].texto` | Obligatorio aunque tenga imagen (sirve de descripción accesible), hasta 150 caracteres |
| `opciones[].imagen` | Opcional. Si viene, tiene que ser una ruta devuelta por la subida de imágenes (empieza con `/imagenes/`) |
| `OPCION_MULTIPLE` | De 3 a 4 opciones, **exactamente una** con `esCorrecta: true`. `ordenCorrecto` es `null` |
| `ORDENAR` | De 3 a 5 pasos. `ordenCorrecto` obligatorio en cada uno, con las posiciones `1…n` **todas distintas** (sin repetir ni saltear). `esCorrecta` es `false` o `null`. En la lista se guardan en el orden de `ordenCorrecto` |

> Cambiar el formato de un ejercicio existente obliga a revisar sus opciones: el `PUT` aplica las reglas del formato nuevo.

### 3.3 Microcápsulas (CU-11)

**`GET /api/admin/microcapsulas`** · Devuelve las microcápsulas vigentes (sin `fechaBaja`), de la más reciente a la más antigua (por `fechaPublicacion` y luego por `id`).

```json
[
  {
    "id": 3,
    "titulo": "Dormir mejor",
    "categoria": "SUENIO_DESCANSO",
    "fechaPublicacion": "2026-10-05",
    "imagen": null
  }
]
```

**`GET /api/admin/microcapsulas/{id}`** · Detalle: `id`, `titulo`, `texto`, `categoria`, `imagen`, `fechaPublicacion`. `404` si no existe o está dada de baja.

**`POST /api/admin/microcapsulas`** · **201 Created** con el detalle.
```json
{
  "titulo": "Dormir mejor",
  "texto": "Texto completo de la lectura…",
  "categoria": "SUENIO_DESCANSO",
  "imagen": "/imagenes/3f2a9c1e-….png",
  "fechaPublicacion": "2026-10-28"
}
```

**`PUT /api/admin/microcapsulas/{id}`** · Mismo cuerpo que el `POST`. **200 OK** con el detalle.

**`DELETE /api/admin/microcapsulas/{id}`** · Baja lógica: guarda `fechaBaja` con la fecha de hoy. **204 No Content**. `404` si no existe o ya está dada de baja. No se tocan las marcas de favorita de las personas usuarias.

**Validaciones**

| Campo | Regla |
|---|---|
| `titulo` | Obligatorio, hasta 120 caracteres |
| `texto` | Obligatorio, hasta 5000 caracteres |
| `categoria` | Obligatoria, valor del enum |
| `imagen` | **Opcional**. Si viene, ruta devuelta por la subida de imágenes |
| `fechaPublicacion` | Obligatoria, `yyyy-MM-dd`, no puede ser posterior a hoy (hora de Argentina) |

### 3.4 Imágenes

**Cómo se usa (dos pasos):**
1. La persona operadora toca "Elegir imagen" (o "Agregar imagen" en una opción) y elige un archivo de su computadora. El front lo sube a `POST /api/admin/imagenes` y recibe una ruta, por ejemplo `/imagenes/3f2a….png`. Muestra una miniatura.
2. Al guardar el formulario, el front manda esa ruta en el campo `imagen` del ejercicio (dentro de la opción) o de la microcápsula. La base guarda solo la ruta, no el archivo.

Se usa en dos lugares: la imagen de la microcápsula y la imagen opcional de cada opción de un ejercicio.

**`POST /api/admin/imagenes`** · Sube una imagen. Es `multipart/form-data` con una parte llamada `archivo`. **201 Created**:

```json
{ "ruta": "/imagenes/3f2a9c1e-7b1d-4c0e-9a55-0c2d1f6e8a10.png" }
```

- Formatos permitidos: JPG, PNG y WEBP. El back verifica el contenido real del archivo, no solo la extensión ni el `Content-Type`.
- Tamaño máximo: 2 MB.
- El nombre del archivo guardado lo genera el back (UUID + extensión según el tipo detectado). Nunca se usa el nombre que manda el navegador.
- Carpeta de destino: propiedad `alma.uploads.dir`, con la variable `ALMA_UPLOADS_DIR` (por defecto `./uploads`, relativa a donde corre el back). Esa carpeta va en `.gitignore`.
- Errores: `400` con detail "La imagen tiene que ser JPG, PNG o WEBP." si el tipo no es válido o el archivo está vacío; `413` con detail "La imagen no puede pesar más de 2 MB." si se pasa del tamaño.

**`GET /imagenes/{nombre}`** · Sirve el archivo guardado. Es **público** (no empieza con `/api`), porque la persona mayor tiene que ver las imágenes en una etiqueta `<img>`, que no puede mandar el token.

> Limitación conocida: si se sube una imagen y después no se guarda el formulario, o se la reemplaza, el archivo queda en la carpeta sin usar. No se limpia automáticamente en el MVP. Más adelante, al armar el `Dockerfile`, esa carpeta tiene que ser un volumen.

### 3.5 Perfil

No hay endpoint nuevo: se usa `GET /api/auth/me` (contrato de login, sección 4.2). La pantalla muestra nombre, apellido, nombre de usuario y rol de la persona administradora.

---

## 4. Errores

Mismo formato Problem Details que el contrato de login (sección 7), con `errores` en los `400`.

**400 de ejercicios**
- Si falta algún campo obligatorio: `detail` = "Completá todos los campos requeridos del ejercicio." y `errores` con cada campo (por ejemplo `{ "campo": "consigna", "mensaje": "Ingresá la consigna." }`).
- Si las opciones no cumplen el formato (y los campos obligatorios están completos): `errores` con `campo: "opciones"` y el `detail`:
  - Opción múltiple: "Cargá de 3 a 4 opciones y marcá una sola como correcta."
  - Ordenar: "Cargá de 3 a 5 pasos y asignale a cada uno una posición distinta."

**400 de microcápsulas**
- Si falta algún campo obligatorio: `detail` = "Completá todos los campos requeridos de la microcápsula." y `errores` por campo.
- Si `fechaPublicacion` es futura: `detail` "Revisá los datos ingresados." y `errores` con `{ "campo": "fechaPublicacion", "mensaje": "La fecha de publicación no puede ser futura." }`.

**Otros:** `401` sin token o vencido, `403` con un token que no es de `ADMINISTRADOR`, `404` "No encontramos lo que buscás.", `500` genérico.

---

## 5. Back: qué implementar

Paquete base: `ar.edu.utn.frm.gelab.alma`. Un paquete por módulo, con `controller/ service/ repository/ domain/ dto/` (ver `CLAUDE.md`).

- `ejercicios/` (el **catálogo** que carga la persona administradora: `Ejercicio` y `OpcionRespuesta`). El juego de la persona mayor (CU-05, con `ProgresoJuego` e `IntentoEjercicio`) va más adelante en `juegos/` y lee de este catálogo; en este módulo no se crea `juegos/`. Hay que sumar `ejercicios` a la lista de paquetes de `CLAUDE.md`: `domain/Ejercicio`, `domain/OpcionRespuesta`, enums `CategoriaEjercicio`, `FormatoEjercicio` y `NivelDificultad`; `repository/EjercicioRepository`; `service/EjercicioService`; `controller/EjercicioAdminController`; `dto/`.
- `microcapsulas/`: `domain/Microcapsula`, enum `CategoriaMicrocapsula`; `repository`; `service/MicrocapsulaService`; `controller/MicrocapsulaAdminController`; `dto/`.
- `administrador/controller/PanelAdminController` y su servicio, para el resumen.
- Imágenes: servicio de almacenamiento y `ImagenAdminController` (`POST /api/admin/imagenes`), más la configuración que sirve `/imagenes/**`. Se ubican en `common/` o `config/` (lo decide el plan).
- `Ejercicio` ↔ `OpcionRespuesta`: composición (`@OneToMany` con `cascade = ALL` y `orphanRemoval = true`).
- Entidades según el DC. `fechaAlta` y `fechaBaja` son `LocalDate`.
- Las reglas de validación van en el servicio (o en validadores propios), para poder devolver los textos exactos de la sección 4.
- **Archivos compartidos** (se modifican solo agregando, sin cambiar lo que ya existe): `application.properties` (carpeta de imágenes y tamaño máximo de subida), `SecurityConfig` (ruta pública `/imagenes/**`), `GlobalExceptionHandler` (si hace falta), `.gitignore` (carpeta `uploads/`) y `.env.example` (variable comentada).
- **Seed de ejemplo** en una clase nueva, `config/DatosDeCatalogo`, con `@ConditionalOnProperty("alma.seed.enabled")` e idempotente: al menos un ejercicio por categoría (incluido uno de `ORDENAR`, y uno de `MEMORIA` con texto a memorizar) y 5 microcápsulas. No se edita `DatosDePrueba`.
- Tests de integración con `@SpringBootTest` y MockMvc, igual que los del login.

---

## 6. Front: qué implementar

- **Layout del panel:** menú lateral con Panel, Ejercicios, Microcápsulas y **Perfil**, y "Cerrar sesión" abajo, como en el Figma. La sección activa se marca. **Adaptable a computadora y a celular.** En escritorio (desde 1024 px, el corte que ya usa la app) el menú ocupa `--sidebar`, como en el Figma. En pantallas más chicas (**propuesto**, el Figma del operador es solo de escritorio): el menú lateral pasa a una barra superior con un botón "Menú" que abre el mismo menú en un panel (con las mismas opciones y "Cerrar sesión"), las tablas pasan a tarjetas apiladas (una por ejercicio o microcápsula, con los botones de editar y eliminar), los formularios van en una columna y la vista previa "Así lo ve la persona" queda debajo del formulario.
- **Cerrar sesión:** abre un diálogo "¿Querés cerrar sesión?" con Cancelar y "Sí, cerrar sesión". Reemplaza el botón "Salir" temporal de `PanelOperadorPage`.
- **Rutas** (todas dentro de `ProtectedRoute` con rol `ADMINISTRADOR`):

| Ruta | Pantalla |
|---|---|
| `/admin` | Panel: dos tarjetas con los conteos y botones "Ver" y "Nuevo" |
| `/admin/ejercicios` | Lista con filtros por categoría (Todas + 4), tabla y botón "Nuevo ejercicio" |
| `/admin/ejercicios/nuevo` y `/admin/ejercicios/:id/editar` | Formulario de ejercicio |
| `/admin/microcapsulas` | Lista con tabla y botón "Nueva microcápsula" |
| `/admin/microcapsulas/nueva` y `/admin/microcapsulas/:id/editar` | Formulario de microcápsula |
| `/admin/perfil` | Datos de la persona administradora |

- **Las rutas del admin viven en un archivo propio** (`features/admin/`), y `App.tsx` solo suma una línea que lo incluye.
- **Formulario de ejercicio:** categoría, consigna, formato, opciones (según el formato: opción múltiple con "Correcta" y 3 a 4 opciones; ordenar con la posición de cada paso y 3 a 5 pasos), nivel de dificultad, explicación (opcional), texto a memorizar (solo en `MEMORIA`) y, en cada opción, una imagen opcional (un botón "Agregar imagen" junto al texto de la opción, con miniatura y "Quitar imagen"; el Figma no lo dibuja, sale de CU-10). A la derecha, la vista previa "Así lo ve la persona", que se actualiza mientras se escribe. Abajo: Cancelar y "Guardar ejercicio".
- **Formulario de microcápsula:** título, texto, categoría, imagen (opcional), fecha de publicación (con hoy como valor inicial), el aviso "Se publica apenas la guardás. No queda como borrador.", y la vista previa. Abajo: Cancelar y "Guardar y publicar".
- **Errores de formulario:** franja roja arriba con el `detail` que devuelve el back, y los campos con error marcados en rojo, con ícono y texto (no solo color). Las mismas validaciones se hacen también en el front con zod para avisar antes de enviar.
- **Confirmaciones:** diálogo al eliminar un ejercicio ("¿Eliminar este ejercicio?", con el texto ajustado a baja lógica: "Se va a eliminar del catálogo. Esta acción no se puede deshacer.") y al dar de baja una microcápsula ("¿Dar de baja esta microcápsula?", "Los usuarios que la guardaron como favorita seguirán viéndola.").
- **Avisos de éxito** (breves, accesibles): "El ejercicio fue creado." / "…actualizado." / "…eliminado." y "La microcápsula fue publicada." / "…actualizada." / "…dada de baja."
- **Datos:** con TanStack Query (falta el `QueryClientProvider`). Después de crear, editar o dar de baja, se refrescan las listas y el resumen.
- **`lib/api.ts`:** se suma soporte para `DELETE` y para enviar archivos (`FormData`), sin cambiar el comportamiento actual de `GET`, `POST` y `PUT` con JSON.
- **Componentes:** se usan los de shadcn que ya están en el repo (botón, input, label, alert, checkbox, radio-group, dialog, casilla, campo-texto). Los que falten (textarea, tabla, badge, migas de pan, aviso de éxito) se agregan con la CLI de shadcn, avisando antes, y se adaptan a los tokens de `index.css`.
- **Accesibilidad** (como en el resto de la app): etiquetas siempre visibles, botones de al menos 44 px, todo usable con teclado, foco visible, tabla con encabezados semánticos, botones de ícono con nombre accesible ("Editar ejercicio…", "Eliminar ejercicio…"), y que se vea bien con letra muy grande y alto contraste.

---

## 7. Cambios en archivos compartidos

Todo cambio en un archivo que también puede tocar otra rama (`App.tsx`, `lib/api.ts`, `PanelOperadorPage.tsx`, `SecurityConfig`, `application.properties`, `GlobalExceptionHandler`, `.gitignore`, `.env.example`, `CLAUDE.md`) tiene que ser **solo agregar**: no renombrar ni quitar nada que ya exista, y no cambiar el comportamiento actual del login ni de las pantallas de otras personas. Al terminar cada paso se anota qué archivo compartido se tocó, qué se cambió y por qué. Esa lista va a la descripción del pull request.

---

## 8. Pruebas y criterio de "listo"

**Back** (tests automáticos y Swagger)
- Con el token de `admin`: crear, listar, ver, editar y dar de baja ejercicios y microcápsulas. Con el token de `ana`: `403`. Sin token: `401`.
- Ejercicio con campos vacíos, con 2 opciones, con dos correctas, o de ordenar con posiciones repetidas: `400` con los textos de la sección 4.
- Categoría `MEMORIA` sin texto a memorizar: `400`.
- Dar de baja: `fechaBaja` queda guardada y el elemento desaparece de la lista y del conteo del panel.
- Microcápsula con fecha futura: `400`.
- Subida de imagen: un PNG válido da `201` y se abre en `/imagenes/…`; un archivo de otro tipo da `400`; uno de más de 2 MB da `413`.

**Front** (manual, con back y front corriendo juntos)
- Entrar con `admin` y recorrer Panel, Ejercicios, Microcápsulas y Perfil.
- Crear, editar y dar de baja un ejercicio de cada formato y una microcápsula (con y sin imagen).
- Probar las dos pantallas de error del Figma (formularios vacíos).
- Cerrar sesión con el diálogo de confirmación.
- `ana` no puede entrar a `/admin`.
- Todo se puede usar solo con teclado, con letra muy grande y con alto contraste.
- Se ve y se usa bien en computadora y en celular (achicar la ventana a menos de 1024 px): menú en panel, tablas como tarjetas y formularios en una columna.
- `pnpm build` y `pnpm lint` sin errores.

**Listo cuando:** con `docker compose up -d`, el back y `pnpm dev` corriendo, la persona administradora puede gestionar el catálogo de ejercicios y de microcápsulas completo desde el panel, y lo que carga se ve reflejado en la base.

---

## 9. Para documentar al terminar

Estas decisiones ya están tomadas; falta reflejarlas en `docs/casos-de-uso.md` y en el diagrama de clases:

1. **CU-05** no dice en qué momento se muestra la explicación del ejercicio. Se muestra junto con el aviso de acierto o error.
2. **Perfil:** el Figma ni los casos de uso nombran esta pantalla. Hay que sumarla a la documentación del panel de operador.
3. **Panel en celular:** el Figma del operador es solo de escritorio. Se documenta que el panel es adaptable.
4. **CU-10:** pasar "eliminar" a baja lógica (se marca la fecha de baja), ajustar el texto del diálogo y los mensajes de error con voseo ("Cargá…", "asignale…").
5. **Figma:** la categoría "Lista de palabras" es `MEMORIA` en los documentos.

---

## 10. Historial de cambios

- **v1 (28/10/2026):** primera propuesta.
- **v2 (28/10/2026):** se aclaró que el catálogo va en `ejercicios/` y el juego de la persona mayor (CU-05) en `juegos/`; se explicó el uso de las imágenes (dos pasos) y por qué las opciones pueden tener imagen aunque el Figma no la dibuje; el panel es adaptable a celular y a computadora; se aceptaron las propuestas pendientes (explicación en CU-05, Perfil y panel en celular), que pasan a la lista de cosas a documentar.
