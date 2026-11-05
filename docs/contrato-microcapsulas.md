# Contrato de Lectura de microcápsulas · Alma Acompaña

**Estado:** acordado entre los dos. Es la referencia única para el back y el front del módulo. Si algo cambia, se cambia primero acá y se avisa al otro.

**Fuente de verdad:** caso de uso CU-06 (Consultar microcápsulas disponibles) y el diagrama de clases (clases `Microcapsula` y `MicrocapsulaGuardada`, y el enum `CategoriaMicrocapsula`). Si este contrato contradice esos documentos, ganan los documentos y se corrige el contrato. El Figma es ilustrativo: donde difiere en funcionalidad, ganan los documentos (ver sección 12).

**Dónde va:** `docs/contrato-microcapsulas.md`. El `CLAUDE.md` debe enlazarlo en la sección "Documentos".

**Última revisión contra el repo:** commit `3009290` (merge del PR #5, `feature/juegos`), `main` del 05/11/2026.

**Contratos relacionados:** `docs/contrato-login.md` (token, roles, errores, convenciones), `docs/contrato-panel-operador.md` (el alta, la edición y la baja de las microcápsulas, CU-11, y las imágenes) y `docs/contrato-juegos.md` (la pantalla Entretenimiento y la estructura de un módulo de la persona mayor). Todo lo que no se redefine acá sigue como está ahí.

---

## 1. Alcance

**Incluye:**
- Para la persona mayor: el listado de microcápsulas vigentes, el detalle de cada una, guardarla y quitarla de guardadas, y la lista de guardadas.
- La entidad `MicrocapsulaGuardada`.
- La pantalla Leer (reemplaza la provisoria "Lecturas"), con las pestañas "Todas" y "Guardadas", y el detalle.
- La ampliación de los datos de prueba (anexo A).
- Pruebas automáticas del back.

**No incluye:**
- El alta, la edición y la baja de microcápsulas: ya están en el panel (CU-11).
- Filtros, búsqueda ni orden elegible (el CU los excluye en esta etapa).
- Lectura en voz alta, compartir o imprimir.
- Que el chat (CU-02) ofrezca microcápsulas.
- Notificaciones de contenido nuevo.

---

## 2. Decisiones

| Tema | Decisión | Motivo |
|---|---|---|
| Vigente | Una microcápsula sin `fechaBaja`. El panel ya impide fechas de publicación futuras, así que no se filtra por fecha | CU-06 y contrato del panel |
| Orden | Todas: `fechaPublicacion` descendente (la más nueva primero) y, si empatan, `id` descendente. Guardadas: `fechaGuardado` descendente (la última guardada primero) | El CU no permite orden elegible; es el orden que ya usa el panel |
| Guardadas | Dos pestañas dentro de Leer, "Todas" y "Guardadas". La pestaña elegida viaja en la URL (`?ver=guardadas`) | Mismo patrón que Lista y Calendario en recordatorios; "Volver" regresa a la pestaña de origen |
| Una guardada dada de baja | Se ve **igual** que las demás en "Guardadas": sin marca ni aviso | Decisión del equipo; el CU solo exige que siga accesible |
| Acceso al detalle | Responde si está vigente o si la persona la tiene guardada. Si no, 404 (también si no existe) | CU-06, regla de negocio: la guardada sigue accesible aunque ya no esté vigente |
| Guardar y quitar | `PUT` y `DELETE` sobre `/{id}/guardada`. Son idempotentes: repetirlas no falla ni duplica | Menos casos de error y no hace falta saber el estado anterior |
| Clave de la guardada | Clave propia (`id`) y restricción única sobre (`personaMayor`, `microcapsula`) | El DC solo lista `fechaGuardado`; la clave y la restricción son técnicas, no un dato nuevo |
| Ver si está guardada | El listado y el detalle traen `guardada` (verdadero o falso) para la persona del token | Evita pedir las guardadas aparte para pintar cada tarjeta |
| Contenido de la tarjeta | Chip de categoría, título y botón "Leer"; con una marca "Guardada" (texto e ícono) si lo está. Sin vista previa del texto ni imagen | Figma "Leer"; la marca "Guardada" evita que la persona tenga que abrir cada una para saberlo |
| Texto | Se muestra con sus saltos de línea y ancho de lectura limitado. La imagen, si hay, es decorativa (`alt=""`): el título ya la describe | El dato no tiene texto alternativo (DC); los textos de hasta 5000 caracteres llevan párrafos |
| Después de quitar desde el detalle | La pantalla sigue mostrando el texto ya cargado (el front actualiza su copia local); no vuelve a pedir el detalle | Si la microcápsula estaba dada de baja, un nuevo pedido daría 404 justo después de quitarla |
| Doble guardado simultáneo | No se agrega una guarda: la restricción única lo impide y, en el caso improbable de dos pedidos a la vez, uno puede dar 500 | Mismo criterio que juegos |
| Dueño del dato | Todo sale del token. No hay ids de persona en URL ni body | Contrato de login, sección 2 |
| Fechas | `LocalDateTime` de Argentina para `fechaGuardado`, con el `Clock` existente | Mismo criterio que recordatorios y juegos |
| Seed | Se suman 5 microcápsulas con un componente nuevo (anexo A). Se apaga con `ALMA_SEED_ENABLED=false` | Faltan Cuidado del hogar y Recomendaciones, y hacen falta textos de distinto largo |
| Carpeta del front | `features/lecturas/`. No se toca `features/microcapsulas` (es el panel) | Mismo criterio que `juegos` y `ejercicios` |

---

## 3. Convenciones de la API

Valen las del contrato de login (sección 3). Para este módulo:

- Todo cuelga de `/api/microcapsulas`. El panel sigue en `/api/admin/microcapsulas`; no hay solapamiento.
- Enum `CategoriaMicrocapsula` tal como ya existe.
- Las imágenes llegan como ruta (`"/imagenes/..."`) o `null`.
- Las respuestas no traen `fechaPublicacion` ni `fechaBaja`: la persona no los usa.

---

## 4. Modelo de datos

Según el DC. No se agregan campos de negocio.

- `MicrocapsulaGuardada`: `id` (técnico), `personaMayor` (muchos a uno, obligatorio), `microcapsula` (muchos a uno, obligatorio), `fechaGuardado` (`LocalDateTime`). Restricción única sobre (`personaMayor`, `microcapsula`).
- `Microcapsula` no cambia.

Todo vive en el paquete `microcapsulas` ya existente.

---

## 5. Reglas de negocio

### 5.1 Listado (`GET /api/microcapsulas`)

Las microcápsulas sin `fechaBaja`, en el orden de la sección 2, cada una con `guardada` según la persona del token. Si no hay ninguna, lista vacía (200).

### 5.2 Guardadas (`GET /api/microcapsulas/guardadas`)

Las microcápsulas que la persona del token tiene guardadas, **estén vigentes o no**, de la última guardada a la primera. Todas con `guardada` verdadero. Si no hay ninguna, lista vacía.

### 5.3 Detalle (`GET /api/microcapsulas/{id}`)

- Existe y está vigente: se devuelve, con `guardada` según la persona.
- Existe, no está vigente y la persona la tiene guardada: se devuelve, con `guardada` verdadero.
- Cualquier otro caso (no existe, o está dada de baja y la persona no la guardó): 404.

### 5.4 Guardar (`PUT /api/microcapsulas/{id}/guardada`)

- Si la microcápsula no es accesible para la persona (5.3): 404.
- Si ya estaba guardada: no hace nada (204).
- Si no: crea la `MicrocapsulaGuardada` con `fechaGuardado` = ahora (204).

### 5.5 Quitar (`DELETE /api/microcapsulas/{id}/guardada`)

- Si la microcápsula no existe: 404.
- Si existe: borra la marca de la persona si la hay y, si no la había, no hace nada (204 en ambos casos). Sirve también para una dada de baja que la persona había guardado.

---

## 6. Endpoints

### 6.1 `GET /api/microcapsulas` y `GET /api/microcapsulas/guardadas`

Respuesta `200` (lista, mismo formato en las dos):

```json
[
  {
    "id": 8,
    "titulo": "Cómo prevenir caídas en casa",
    "categoria": "PREVENCION_CAIDAS",
    "imagen": null,
    "guardada": true
  }
]
```

### 6.2 `GET /api/microcapsulas/{id}`

Respuesta `200`:

```json
{
  "id": 8,
  "titulo": "Cómo prevenir caídas en casa",
  "texto": "Sacá las alfombras chicas que se mueven…\n\nDejá una luz encendida de noche…",
  "categoria": "PREVENCION_CAIDAS",
  "imagen": null,
  "guardada": false
}
```

Errores: `404` (5.3).

### 6.3 `PUT /api/microcapsulas/{id}/guardada` y `DELETE /api/microcapsulas/{id}/guardada`

Sin cuerpo. Respuesta `204` sin contenido. Errores: `404` (5.4 y 5.5).

---

## 7. Reglas de acceso

| Ruta | Quién puede |
|---|---|
| `/api/microcapsulas/**` | Solo `PERSONA_MAYOR`. Un `ADMINISTRADOR` recibe `403` |

Sin token o con token vencido: `401`, como en el resto de `/api`.

---

## 8. Errores y mensajes

Formato Problem Details, igual que en el contrato de login. Este módulo no define textos de error nuevos: usa los generales de `common/MensajesError`.

| Status | Cuándo | `detail` |
|---|---|---|
| 401 | Token faltante, inválido o vencido | "Tu sesión terminó. Ingresá de nuevo." |
| 403 | Rol sin permiso | "No tenés permiso para ver esta pantalla." |
| 404 | La microcápsula no existe o no es accesible para la persona | "No encontramos lo que buscás." |
| 500 | Error interno | "Algo salió mal. Probá de nuevo en un rato." |

---

## 9. Back: qué implementar

```
microcapsulas/
  controller/MicrocapsulaController           persona mayor (Tag "Microcápsulas")
  service/LecturaMicrocapsulaService          listar(), guardadas(), detalle(), guardar(), quitar()
  domain/MicrocapsulaGuardada
  repository/MicrocapsulaGuardadaRepository
  dto/MicrocapsulaLecturaResumenResponse, MicrocapsulaLecturaDetalleResponse
config/DatosDeCatalogoMicrocapsulas           seed del anexo A
```

Se agrega junto a lo del panel sin modificarlo (no se toca `MicrocapsulaAdminController` ni `MicrocapsulaService`). Los DTO son records.

Orden de implementación, con pausa de prueba al final de cada paso:

1. **Entidad y repositorio.** `MicrocapsulaGuardada` con la restricción única y su repositorio (existencia, borrado, lista de la persona con su microcápsula y los ids guardados por una persona). Si hace falta una consulta nueva en `MicrocapsulaRepository`, se agrega sin tocar lo existente.
2. **Servicio y endpoints.** Las reglas 5.1 a 5.5, los DTO de la sección 6, el controlador documentado en Swagger y la línea de `SecurityConfig`. El usuario sale del token (claim `sub` = id de cuenta, buscar su `PersonaMayor`).
3. **Seed.** `DatosDeCatalogoMicrocapsulas` con el anexo A, idempotente por título (`existsByTitulo`), con `@ConditionalOnProperty("alma.seed.enabled", havingValue = "true")`. No se toca `DatosDeCatalogo`.
4. **Pruebas** (sección 13) y resumen.

### Cambios en archivos compartidos (para la descripción del PR)

- `SecurityConfig.java`: una línea, `.requestMatchers("/api/microcapsulas/**").hasRole("PERSONA_MAYOR")`, antes de la regla general de `/api/**`.

---

## 10. Front: qué implementar

Carpeta nueva `features/lecturas/` (no se toca `features/microcapsulas`, que es del panel):

```
features/lecturas/
  index.ts                     exporta LecturasPage y LecturaDetallePage
  tipos.ts · api.ts · useLecturas.ts · mensajes.ts
  pages/LecturasPage.tsx       listado con pestañas
  pages/LecturaDetallePage.tsx
  components/TarjetaLectura.tsx · BotonGuardar.tsx
```

La separación en `pages/` y `components/` sigue la convención del CLAUDE.md.

### 10.1 Rutas (dentro del `LayoutPersona`)

| Ruta | Pantalla |
|---|---|
| `/entretenimiento/lecturas` | Leer: pestañas "Todas" y "Guardadas" (`?ver=guardadas` abre la segunda). Reemplaza a la provisoria |
| `/entretenimiento/lecturas/:id` | Detalle de una microcápsula |

La ruta `/entretenimiento/lecturas` ya existe en `entretenimiento/rutas.ts`; el detalle se agrega ahí también.

### 10.2 Pantallas

- **Leer.**
  - "‹ Entretenimiento" arriba del título (hacia `/entretenimiento`) y título "Leer".
  - Pestañas "Todas" y "Guardadas", con el mismo componente `Tabs` que recordatorios.
  - Tarjetas en una columna en celular y en tres en escritorio, con el chip de categoría, el título y el botón "Leer". Si está guardada, una marca "Guardada" con ícono y texto.
  - Tocar "Leer" (o la tarjeta) abre el detalle.
  - Estados: cargando, error (`Alert` con `mensajeDeError` y "Probar de nuevo"), "Por ahora no hay lecturas disponibles." (Todas vacía) y "Todavía no guardaste ninguna lectura." (Guardadas vacía).
- **Detalle.**
  - "‹ Leer" (hacia el listado; si la persona llegó desde Guardadas, vuelve a esa pestaña).
  - Chip de categoría, título (`h1`), imagen si hay, el texto con sus párrafos y ancho de lectura limitado.
  - Botón con texto visible: "Guardar" y, si ya está guardada, "Quitar de guardadas", con ícono y `aria-pressed`.
  - Se deshabilita mientras viaja el pedido.
  - Estados: cargando y error; en 404, "No encontramos lo que buscás." con el botón de volver.
- **Avisos.** Al guardar: "Guardaste esta lectura." Al quitar: "Quitaste esta lectura de guardadas." Se muestran con el aviso flotante existente (`AvisoProvider`).

### 10.3 Datos

- Claves de consulta con el id del usuario, como juegos: listado, guardadas y detalle.
- Guardar y quitar son mutaciones. Al terminar bien: se actualiza la copia local del detalle (`guardada`) y se invalidan el listado y las guardadas. No se vuelve a pedir el detalle (sección 2).
- Si la mutación falla, `Alert` arriba del botón con `mensajeDeError`, y el botón se vuelve a habilitar.

### 10.4 Accesibilidad

Botones y tarjetas de 44 px como mínimo, etiquetas visibles (nada de ícono solo), todo usable con teclado y foco visible, el estado "Guardada" siempre con texto además del ícono, alto contraste (`contraste-alto`: borde en tarjetas y chip), letra de 100/125/150 % con unidades en `rem`, el `h1` de cada pantalla con el foco al entrar, y lectura cómoda del detalle (interlineado amplio y ancho limitado).

### 10.5 Textos

Todos en `features/lecturas/mensajes.ts`; los generales siguen en `lib/mensajes.ts` y `lib/errores.ts`. Propuestos: "Leer", "Todas", "Guardadas", "Guardar", "Quitar de guardadas", "Guardada", "Por ahora no hay lecturas disponibles.", "Todavía no guardaste ninguna lectura.", "Guardaste esta lectura.", "Quitaste esta lectura de guardadas.". Los nombres de las categorías ya existen en `features/admin/etiquetas.ts` y se importan, sin modificarlos.

### Cambios en archivos compartidos (para la descripción del PR)

- `App.tsx`: las dos rutas de 10.1 (la de `/entretenimiento/lecturas` reemplaza a la provisoria).
- `features/entretenimiento/rutas.ts`: una constante nueva para el detalle (aditivo).
- `features/entretenimiento/mensajes.ts`: si el título "Lecturas" de la provisoria queda sin uso, se puede quitar; si no, se deja.

---

## 11. Documentos a actualizar

- `docs/casos-de-uso.md`, CU-06: la sección de guardadas como pestaña "Guardadas" dentro de Leer y las pantallas (la sección "Sin crear todavía en el Figma" pasa a decir cuáles se armaron); en el camino básico, el mensaje de lista vacía y el de guardadas vacías; marcar CU-06 como hecho en la tabla de estado.
- `CLAUDE.md`: enlazar este contrato en "Documentos".
- Diagrama de clases: aclarar que `MicrocapsulaGuardada` lleva una clave propia y una restricción única sobre (persona, microcápsula).

---

## 12. Diferencias con el Figma

| Figma | Contrato | Motivo |
|---|---|---|
| En celular, "‹ Entretenimiento" en la barra de arriba | Al principio del contenido, encima del título | La barra de arriba es del `LayoutPersona` (igual que en juegos) |
| No hay pestañas | "Todas" y "Guardadas" | CU-06 pide la sección de guardadas y el Figma no la diseñó |
| No hay detalle | Se arma con los componentes existentes (chip, texto, imagen, botón "Guardar") | No estaba diseñado |
| Tarjeta sin marca de guardada | Marca "Guardada" con texto e ícono en las guardadas | Que se sepa sin abrir la tarjeta |
| Chip de categoría de las 7 categorías | Los de `features/admin/etiquetas.ts` | Ya existen para el panel |

---

## 13. Pruebas y criterio de "listo"

**Tests de integración** (`@SpringBootTest` y MockMvc; necesitan `docker compose up -d`), con cuentas propias (`test_micro_a` y `test_micro_b`) y microcápsulas propias ("TEST MICRO …"), sin tocar el catálogo real ni los datos del seed. Limpieza en `@AfterEach`, en este orden: guardadas, microcápsulas de prueba, personas y cuentas de prueba.

- Listado: trae solo las vigentes, en el orden de la sección 2 y con `guardada` correcto por persona.
- Una microcápsula dada de baja no aparece en el listado.
- Guardadas: incluye las dadas de baja, de la última guardada a la primera.
- Detalle: de una vigente, 200; de una dada de baja no guardada, 404; de una dada de baja guardada, 200; de una inexistente, 404.
- `PUT guardada`: crea una sola marca aunque se repita; sobre una dada de baja no guardada, 404; sobre una inexistente, 404.
- `DELETE guardada`: borra la marca; repetirlo da 204; sobre una inexistente, 404.
- Aislamiento: lo que guarda la persona A no cambia nada a la persona B.
- Sin token, 401. Con token de administrador, 403.

**Listo cuando:**
- Entrando con ana, se ven las microcápsulas del seed en tarjetas, el detalle se abre y se puede guardar y quitar.
- La pestaña "Guardadas" muestra lo guardado y, al darle de baja una desde el panel, esa sigue visible para ana en "Guardadas" y desaparece de "Todas".
- "Volver" del detalle regresa a la pestaña de origen.
- Con carpetas vacías aparecen los mensajes de la sección 10.2.
- Se usa con teclado, en celular y escritorio, con letra al 150 % y con alto contraste.

---

## 14. Límites conocidos (ya decididos)

1. Sin filtros, búsqueda ni paginación: lo pide así el CU y el volumen es chico.
2. Después de quitar una dada de baja desde el detalle, recargar la pantalla da 404 (sección 2).
3. Dos guardados simultáneos del mismo par pueden dar un 500 (sección 2).
4. La imagen no tiene texto alternativo propio: se trata como decorativa.
5. Sin pruebas automáticas del front; las de la sección 13 son manuales. Falta la verificación con lector de pantalla.

---

## 15. Cómo usar este documento con Claude Code

**Back**
> Leé `CLAUDE.md` y `docs/contrato-microcapsulas.md`. Mostrame primero el plan del back según las secciones 5, 6 y 9, y no escribas código hasta que lo apruebe.

**Front**
> Leé `CLAUDE.md` y `docs/contrato-microcapsulas.md`. Mostrame primero el plan del front según la sección 10, y no escribas código hasta que lo apruebe.

Si Claude Code propone algo que contradice el contrato, gana el contrato. Si hay que cambiarlo, se edita este archivo.

---

## 16. Historial de cambios

- **v1 (05/11/2026):** primera versión acordada, contra el commit `3009290`.

---

## Anexo A. Datos de prueba (seed)

Componente nuevo `config/DatosDeCatalogoMicrocapsulas`, con `alma.seed.enabled=true`, idempotente por título (como `DatosDeCatalogo`, que no se toca). Los textos llevan párrafos separados por una línea en blanco (`\n\n`). Con las 5 que ya existen quedan 10: cada categoría tiene al menos una.

| Fecha de publicación | Categoría | Título | Texto |
|---|---|---|---|
| 2026-10-06 | CUIDADO_HOGAR | Un botiquín listo en casa | Tené en un lugar fijo y a mano un termómetro, gasas, curitas y alcohol en gel. Sumá una lista con los teléfonos de tu médico y de un familiar.\n\nRevisá de vez en cuando las fechas de vencimiento de lo que guardás.\n\nGuardá los medicamentos en su caja original, lejos del calor y de la humedad. |
| 2026-10-07 | RECOMENDACIONES | Preguntá todo lo que quieras en la consulta | Antes de ir al médico, anotá tus dudas y los medicamentos que tomás. Llevá esa lista y mostrásela.\n\nSi no entendés algo, pedí que te lo expliquen de nuevo con palabras simples. Es tu derecho y es parte de cuidarte.\n\nPodés ir acompañado por alguien de confianza. Al terminar, pedí que te dejen las indicaciones por escrito para no olvidarte de nada. |
| 2026-10-08 | PREVENCION_CAIDAS | Levantate despacio | Al salir de la cama o de una silla, quedate un momento sentado en el borde y respirá tranquilo.\n\nDespués apoyá bien los pies en el piso y ponete de pie despacio, con algo firme para agarrarte.\n\nAsí se evitan los mareos y los tropiezos. Si te mareás seguido, contáselo a tu médico. |
| 2026-10-09 | ALIMENTACION | Comer bien todos los días | Tratá de sumar verduras y frutas de distintos colores en tus comidas.\n\nRepartí lo que comés en varias veces al día, sin saltearte ninguna.\n\nPara darle sabor a la comida, probá con limón, ajo o hierbas en lugar de sal.\n\nSi tenés una dieta indicada por tu médico, seguí lo que te recomendó. |
| 2026-10-10 | ACTIVIDAD_FISICA | Ejercicios suaves para empezar el día | Antes de empezar el día, mové con suavidad el cuello, los hombros, los brazos y las piernas. Podés hacerlo sentado o parado con un apoyo.\n\nNo hace falta forzar nada ni sentir dolor: unos pocos minutos alcanzan.\n\nSi algún movimiento te duele, dejalo y consultá con tu médico. |
