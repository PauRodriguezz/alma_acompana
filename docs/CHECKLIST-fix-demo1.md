# Checklist · rama `fix/demo1` · Alma Acompaña (cierre y entrega a OSEP)

Actualizado el 19/11/2026. Front: `pnpm lint`, `pnpm build` y `pnpm test` en verde (56/56). El back no está verificado: falta correr `./mvnw test` con las bases arriba.

Convenciones: [ ] pendiente · [x] hecho · (D) necesita decisión de Pau.
**Regla para lo que queda: trabajar sobre los archivos que ya existen y no crear archivos nuevos salvo que sea imprescindible (y justificarlo antes en el plan).**
Regla del repo (CLAUDE.md): un módulo por vez, mostrar el plan antes de escribir código, avisar antes de tocar archivos compartidos o sumar dependencias, pnpm y nunca npm, sin commits ni push por parte de Claude.

## CAMINO AL CIERRE

1. Front que falta, en este orden: 1.1e chips, 1.5 texto enriquecido (front y back), 1.6 historia de vida, 1.9 limpieza. El gráfico del panel (1.7) queda fuera: trabajo futuro.
2. Backend (limpieza de scripts y seed, tests).
3. Datos de ejemplo.
4. Teléfono y despliegue (Docker).
5. README y documentación de entrega.
6. Figma y presentación.
7. Pasada final de accesibilidad y pruebas, con los datos de demo cargados.
8. Documentación a detalle (informe, manual).

---

## 0. REFERENCIAS DEL FIGMA

Archivo: https://www.figma.com/design/B343pyG1hmfGsj9eKmFwsK/Alma-Acompa%C3%B1a--PPS (fileKey `B343pyG1hmfGsj9eKmFwsK`). Para abrir una pantalla, agregar `?node-id=ID` con guion en lugar de dos puntos (`50:1395` pasa a `?node-id=50-1395`).

**El Figma es solo una referencia de estructura y está algo desactualizado. Manda el código: se mantienen los componentes, colores, espaciados y estilo actuales.** Los íconos son Hugeicons y sus nombres están en la página Componentes.

Páginas: Prototipo `29:2` · Documentación `59:2` · Componentes `20:2` · Diagramas `114:2259`.
Pantallas: Recordatorios calendario `50:1395` (celular `32:868`), lista `46:1244` (celular `31:650`) · Historia de vida `46:1157` (celular `31:585`) · Accesibilidad `50:1315` (celular `31:898`) · Operador: login `51:1364`, panel `51:1409`, ejercicios `51:1493`, ejercicio nuevo `52:1765`, microcápsulas `51:1694`.

---

## 1. FRONT

### Hecho (lint, build y test OK)
- [x] 1.1 Consistencia visual: título de sección igual en todas, margen del panel del operador, Perfil (espaciado, columnas, textos del operador).
- [x] 1.1b Menú compartido persona/operador y `AvatarAlma`. Barra inferior de la persona de solo íconos (decisión de Pau, excepción documentada en `contrato-login.md`). Íconos del Figma (Hugeicons). Contratos actualizados.
- [x] 1.1c Desbordes con letra muy grande en el panel y botón de accesibilidad fuera del flujo fijo en celular. "Seguir mi historia de vida" queda como está (decisión de Pau).
- [x] 1.1d Scroll horizontal a 360 px (ilustración del saludo y círculo de Entretenimiento en px fijos, chips con `max-w-full` y `wrap-anywhere`, botón "Seguir escribiendo").
- [x] 1.2 Calendario de Recordatorios en dos columnas iguales en escritorio.
- [x] 1.3 Tarjeta de lectura completa clickeable.
- [x] 1.4 Aviso "Listo, lo dejo para más tarde" al cerrar "Para hoy" (card, opción B; si no convence, alternativa C: burbuja de Alma en el chat).
- [x] 1.8 Avatar de Alma en el chat.

### Lo que falta, en orden
- [x] **1.1e Chips que se parten o desbordan** (una sola corrida corta):
  - En Leer, a 360 px con letra muy grande, "Alimentación" se corta como "Alimentaci / ón". Salidas: menos relleno de la tarjeta en celular (`p-4`) o ícono del chip más chico.
  - En `/admin/microcapsulas`, a 390 px con letra muy grande, el chip "Prevención de caídas" del `Badge` tiene 3 px de scroll. Misma salida (`max-w-full` y `wrap-anywhere`).
- [x] **1.5 Texto enriquecido e imágenes en microcápsulas** (toca front y back; el back está en 2.4):
  - (D) Elegir editor: Markdown con barra simple o editor con formatos acotados (título, negrita, cursiva, listas y "destacado"; sin tamaños libres, chocan con el tamaño de letra de la persona). Dependencia nueva: avisar antes.
  - Formulario del operador (`MicrocapsulaFormPage`) con las herramientas de texto y vista previa fiel; vista de lectura (`LecturaDetallePage`) que muestre el formato y soporte textos largos.
  - Imagen en tarjetas y detalle (ya existe la subida del panel: `subir-imagen.tsx`, `ImagenAdminController`; verificar si la microcápsula tiene campo de imagen).
- [x] **1.6 Historia de vida** (D, necesita la captura de Pau):
  - Rediseñar el detalle del recuerdo (no convence).
  - Descarga del mapa como imagen: más ancha y con cada recuerdo mejor desglosado (hoy se ve mal con recuerdos cortados).
- [ ] **1.9 Limpieza del front (al final, para no refactorizar dos veces):**
  - Ordenar los archivos del menú: `destinos-menu.ts`, `menu-estilos.ts` e `iconos.ts` no son componentes y hoy están mezclados con los de `components/alma`. Propuesta: carpeta `components/alma/menu/` y mover o integrar `iconos.ts`. Decidir cuando se revise el código real.
  - `EnlaceVolver` duplicado (`components/alma/enlace-volver.tsx` y `features/recordatorios/components/EnlaceVolver.tsx`).
  - Comentarios PENDIENTE y restos: `alma-marca.svg` sin uso, "Inicio provisorio" en `mensaje-alma.tsx`, comentarios que hablan de etapas del proyecto.
  - `ChatProvider.tsx` (712 líneas): partirlo en estado, acciones del guion y efectos.
  - Código repetido entre ejercicios y microcápsulas (formularios, listas, vistas previas).
  - Paquete `cn` en `package.json` además de `lib/utils.ts`: confirmar y sacarlo. Ver si `shadcn` (CLI) va en `devDependencies`.
  - Textos de pantalla fuera de `mensajes.ts`.
  - Opcional: code splitting por ruta con `React.lazy` (el bundle de 886 kB supera el umbral de aviso).
  - Tests nuevos para lo que se cambie, y `pnpm lint`, `build` y `test` en verde.
  - “Mover BOTON_TEXTO_LARGO de features/historia/estilos.ts a components/alma/ o lib/, porque ahora lo usan chat, historia y microcápsulas”.

---

## 2. BACKEND

### 2.1 Limpieza de Neo4j y scripts
- [ ] Hay 4 scripts en `backend/alma/scripts/` (dos pares de vista previa y borrado de datos de prueba de bugs ya arreglados). Reemplazarlos por **un solo script** de vista previa + borrado por persona (recuerdos, relaciones RESPONDIO/SALTEO, y nodos Persona y Lugar huérfanos), sin tocar el guion. Leer antes `docs/Modelo_de_grafo/` para no borrar nodos compartidos.
- [ ] Al borrar los 4: sacar `ScriptsDeLimpiezaTest` y las referencias en `docs/contrato-grafo.md`.
- [ ] Borrar lo de `ana` y `carlos` en Neo4j para recargar desde cero. Con `alma.seed.enabled=true`, `CargaInicialDelGrafo` puede volver a cargar la historia de ejemplo de `ana`: apagar el seed (`ALMA_SEED_ENABLED=false` en el `.env`) durante la limpieza o decidir si esa carga se elimina.

### 2.2 Seed y valores por defecto
- [ ] ⚠ `alma.seed.enabled` es `true` por defecto y crea cuentas con contraseña fija (`Alma2026!`). Definir perfil de producción con seed apagado.
- [ ] ⚠ Swagger y `/v3/api-docs` son públicos: protegerlos o apagarlos en producción.
- [ ] ⚠ Alinear la documentación del proveedor de IA: `CLAUDE.md` y el plan hablan de Ollama `gemma4:cloud`, el default real es Gemini (`IA_PROVEEDOR=gemini`).
- [ ] Documentar cómo conectarse a las bases con VS Code (Postgres `localhost:5432`, base `alma`, usuario `alma`; Neo4j `bolt://localhost:7687` o `http://localhost:7474`; claves en el `.env`, nunca en el repo).

### 2.3 Código y tests
- [ ] Revisar por responsabilidades mezcladas o código repetido: `EscrituraDelGrafo` (475 líneas), `ExtractorDeRecuerdos` (357), `GeminiModeloConversacional` (308).
- [ ] Todos los tests en verde (`./mvnw test`, necesita Postgres y Neo4j arriba), sin `@Disabled`. Tests para lo que se cambie.
- [ ] Mensajes de error y textos centralizados (`Mensajes*`).

### 2.4 Soporte para 1.5 (microcápsulas con formato e imagen)
- [ ] Campo de contenido con formato, validación y **sanitización** (porque se va a mostrar), y soporte de imagen. Actualizar `docs/contrato-microcapsulas.md` (si algo contradice el contrato, se edita el documento y se avisa). Trabajar sobre las clases existentes.

### 2.5 Pruebas al correr el sistema
- [ ] Chat con Gemini real: saludo, ánimo, historia de vida, repreguntas y guardrails (16 casos).
- [ ] Panel: alta, edición y baja de ejercicios y microcápsulas, subida de imagen.
- [ ] Probar con `ana`, `carlos` y una persona sin recuerdos.

---

## 3. DATOS DE EJEMPLO

- [ ] Microcápsulas: hoy 10 (todas fechadas del 1 al 10/10/2026). Apuntar a 3 o 4 por categoría (CUIDADO_HOGAR, RECOMENDACIONES, PREVENCION_CAIDAS, ALIMENTACION, ACTIVIDAD_FISICA), con fechas escalonadas.
- [ ] Ejercicios: hoy unos 30. Llegar a 50 o 60 (la ronda es de 10), cubriendo categorías (REFRAN, SERIE, ASOCIACION, MEMORIA), niveles (BASICO, INTERMEDIO, AVANZADO) y todos los formatos que existen.
- [ ] (D) Textos de salud revisados por alguien con criterio sanitario antes de mostrarlos a OSEP.
- [ ] (D) Decidir si los datos son seed por código o una carga documentada. Para OSEP, mejor un script que no dependa de `alma.seed.enabled`.
- [ ] ⚠ Nombre de demo coherente: el seed tiene `ana` y `carlos`; el Figma usa "Laura". Elegir uno para seed, Figma, capturas y presentación.
- [ ] Cargar una historia de vida de demo (`docs/Modelo_de_grafo/datos-de-ejemplo.cypher`) con recuerdos en varias etapas, una vez limpiado Neo4j.

---

## 4. TELÉFONO Y DESPLIEGUE

No hace falta tocar el back: el front llama a `/api` por el proxy de Vite.
- [ ] Misma wifi: `pnpm dev --host`, ver la IP con `ipconfig`, abrir `http://IP:5173` en el celular (permitir Node en el firewall de Windows).
- [ ] Para mostrar en OSEP: túnel (Cloudflare Tunnel o ngrok) al puerto 5173. Queda público mientras dure: con el seed prendido hay usuarios con contraseña conocida.
- [ ] Definitivo: completar el Dockerfile (hoy vacío en la raíz). Back con build en dos etapas (Maven + JDK 21, luego JRE 21) y front con `pnpm build` servido por nginx con proxy a `/api`; sumarlos a `docker-compose.yml` para que `docker compose up --build` deje todo andando.
- [ ] Opcional: `manifest.webmanifest` e íconos para "Agregar a la pantalla de inicio".

---

## 5. README Y DOCUMENTACIÓN DE ENTREGA

- [ ] README raíz (hoy una línea): qué es, captura, arquitectura, stack, requisitos, variables de entorno, cómo levantar todo (con y sin Docker), usuarios de demo, tests, estructura de carpetas, enlaces a `docs/`, autoría (UTN FRM, PPS).
- [ ] Reemplazar `frontend/README.md` (es el de la plantilla de Vite).
- [ ] ⚠ README de integración para OSEP: autenticación, endpoints principales, exportar el OpenAPI, datos que maneja, cómo se desplegaría.
- [ ] ⚠ Ficha de datos personales y salud: qué se guarda y dónde (Postgres y Neo4j), qué sale a Gemini y que el nivel gratuito no se usa con datos reales (ya está en `docs/contrato-ia.md`). El encuadre legal (Ley 25.326) lo definen ustedes.
- [ ] ⚠ Limitaciones conocidas y trabajo futuro: cola de extracciones en memoria, detección de recuerdos por listas de nombres, sin integración real con sistemas de OSEP (más el gráfico de estadísticas del panel, 1.7, que queda como trabajo futuro).
- [ ] ⚠ Sacar `.vscode/settings.json` del índice de Git (el `.gitignore` ya lo excluye).
- [ ] ⚠ Decidir qué se entrega de `CLAUDE.md` y `.claude/settings.json`. `CLAUDE.md` tiene datos viejos (dice que el front usa Inter y que el Dockerfile está vacío "hasta la Etapa 3").
- [ ] ⚠ CI mínimo con GitHub Actions (lint, build y test del front), etiqueta `v1.0` y LICENSE acordado con la facultad.
- [ ] `docs/casos-de-uso.md`, fila CU-09 (~línea 875): agregar la card "Listo, lo dejo para más tarde" y corregir "hoy se monta de forma provisoria en el Inicio" (ya se monta en `ChatPage`).
- [ ] `aria-pressed`: `contrato-microcapsulas.md` lo pide en el botón Guardar y el código dice "sin aria-pressed, decisión del equipo". Decidir cuál manda y alinear.
- [ ] Revisar `docs/`: contratos al día con el código (login, chat, juegos, microcápsulas, historia, mapa de vida), `casos-de-uso.md`, diagrama de clases y modelo de grafo.

---

## 6. FIGMA Y PRESENTACIÓN (después de cerrar front y back)

### Figma
- [ ] Avatar de Alma: reemplazar los círculos lisos ("Avatar de Alma (provisorio)", elipses "Alma") en chat móvil y escritorio, pregunta de Alma, entretenimiento, historia de vida y login del operador.
- [ ] Menú: Ejercicios con `puzzle` (el Figma le da `note-01`), Perfil del operador (`user-02`) que no está en el Figma, marca del operador de 40 px sin enlace, celular del operador sin "Alma / Panel de operador" arriba. Los nombres `arrow-left-01-round` y `arrow-right-01-round` no existen en el paquete gratuito (los íconos del código ya son el trazo redondeado).
- [ ] Reflejar lo que cambió en el front: calendario en dos columnas, mensaje "más tarde", Perfil, tarjetas de lectura, y texto enriquecido.
- [ ] Historia de vida: el Figma tiene una línea de tiempo simple; el código tiene mapa por etapas, panel de etapa, detalle del recuerdo y descarga del mapa (`contrato-mapa-de-vida.md`).
- [ ] Textos de prototipo: el login del operador dice "con cualquier usuario y la contraseña «alma»"; "Completar con una palabra" muestra el texto "Ordenar los pasos"; "Laura" vs `ana`/`carlos`; panel con "48 ejercicios" y "12 microcápsulas".
- [ ] Confirmar que están: Perfil, repreguntas en el chat, estado vacío de historia, detalle de recuerdo, detalle de lectura, detalle de cita, diálogos de cerrar sesión y eliminar, aviso flotante.
- [ ] Variables y componentes coherentes con `index.css` (primary #8A199C, accent #C41E7A, fondo #FFFCF5, texto #2B4150, títulos #124E73, base de 17 px). Botones: el Figma dice 48 px y `CLAUDE.md` pide 44 px mínimo.
- [ ] Probar los 6 recorridos del prototipo con Play ▶.

### Presentación (18 diapositivas de "Documentación")
- [ ] Cifras: la lámina 2 dice "20 pantallas móvil, 25 de escritorio, 6 recorridos" y la 4 lo repite. Recontar al final.
- [ ] La portada dice "Octubre 2026"; ahora es noviembre.
- [ ] Lámina 13 ("Nueve pantallas... Perfil"): verificar contra el prototipo. Revisar las láminas 14 a 18 (no pude leerlas).
- [ ] Sumar o actualizar láminas de arquitectura real (Spring Boot, Postgres, Neo4j, Gemini), historia de vida con mapa por etapas, guardrails e integración con OSEP.
- [ ] Reemplazar los círculos lisos de Alma en las capturas del collage de portada.
- [ ] Guion de demo de 5 a 7 minutos con el usuario y los datos de las secciones 3 y 2.5.

---

## 7. PASADA FINAL DE ACCESIBILIDAD Y PRUEBAS

Con los datos de demo cargados, una sola vez, al terminar front, back y datos:
- [ ] Letra al 100, 125 y 150 % y alto contraste en todas las pantallas (el 125 % no se probó en ningún ítem hecho hasta ahora; en alto contraste solo se midieron los menús y los desbordes).
- [ ] Pantallas con datos: detalle de una cita, detalle de un recuerdo, ronda de juego con un ejercicio, diálogos y panel de etapa de la historia.
- [ ] Título "Entretenimiento" a 360 px con letra muy grande: mide 307 px en una columna de 273 px y llega a 343 de 345 px (sin scroll, pero al borde).
- [ ] En el teléfono real: calendario de Recordatorios, tocar "Leer" y un borde de la tarjeta, la barra inferior, el teclado del chat (Android y, si se puede, iPhone).
- [ ] Con una llamada real al modelo: mensajes de Alma en la conversación con el avatar nuevo, "sin pendientes" y el guion.
- [ ] A 390 px con letra muy grande, el botón flotante "Seguir mi historia de vida" se superpone con la card "Para hoy". Es la decisión de Pau; solo ver si molesta.

---

## 8. DESPUÉS: DOCUMENTACIÓN A DETALLE
- [ ] Informe final de la PPS con cifras y capturas del sistema terminado.
- [ ] Manual de uso para la persona mayor y para el operador.
- [ ] Documentación de arquitectura actualizada (los PDF del proyecto son anteriores al cierre).