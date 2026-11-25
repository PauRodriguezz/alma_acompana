# Alma Acompaña

Compañero digital para personas mayores: chat con "Alma", recordatorios de medicación y citas, registro de ánimo, juegos, microcápsulas de contenido e historia de vida (grafo en Neo4j). Proyecto académico (UTN FRM) de dos personas. Todo el texto que ve el usuario está en español rioplatense, con voseo y frases simples.

## Reglas de trabajo (leer primero)

- **GitHub y Git remoto: no hagas nada.** No hagas `git push`, no uses `gh`, no abras ni fusiones pull requests, no crees ni borres ramas remotas, no toques los remotes y no uses conectores de GitHub. Podés leer: `git status`, `git diff`, `git log`.
- No hagas `git commit` ni cambies de rama salvo que te lo pida explícitamente. Los commits los hacemos nosotros.
- **Antes de escribir código, mostrá el plan y esperá que lo aprobemos.**
- Un módulo por vez. No toques archivos de otros módulos. Los archivos compartidos (`docker-compose.yml`, `pom.xml`, `application.properties`, `SecurityConfig.java`, `vite.config.ts`, `package.json`) se modifican solo después de avisar qué cambio y por qué.
- No instales dependencias nuevas sin avisar antes.
- **Secretos:** nunca leas ni muestres el `.env`, y nunca pongas claves reales en archivos que se suben a Git. Las variables entran por `${VARIABLE}` en `application.properties`.
- **Si algo contradice el contrato o los documentos del informe, ganan ellos.** Si hay que cambiarlos, se edita el documento y se avisa. No cambies el comportamiento por tu cuenta.
- **Neo4j y Postgres de desarrollo: no borres datos.** El script de limpieza del grafo lo corremos nosotros desde el navegador de Neo4j; no lo ejecutes ni corras borrados por tu cuenta.
- Al terminar una tarea, decí en pocas líneas qué archivos tocaste, cómo probarlo (el comando exacto) y qué falta.

## Stack

- **Back:** Java 21, Spring Boot 4.1.1, Maven (`./mvnw`), Spring Security con JWT propio (OAuth2 Resource Server), JPA + PostgreSQL, Spring Data Neo4j, Spring AI 2.0.1 con **Gemini** (proveedor por defecto, modelo `gemini-3.5-flash-lite`) u Ollama (alternativa, `gemma4:cloud`; se elige con `IA_PROVEEDOR`), springdoc 3.1.1 (Swagger en `/swagger-ui.html`), Actuator, Lombok.
- **Front:** React 19 + TypeScript, Vite, Tailwind, shadcn/ui, react-router-dom, TanStack Query, react-hook-form + zod, Hugeicons, fuentes Poppins y Source Sans 3, date-fns.
- **Gestor de paquetes del front: pnpm.** Nunca uses npm ni yarn.
- **Infra:** Docker Compose con PostgreSQL 17.11, Neo4j 5.26.29 y Ollama 0.40.6 (Ollama solo se usa con `IA_PROVEEDOR=ollama`). La app no está en el compose y el `Dockerfile` está vacío (queda para el cierre, Etapa 4).
- **Base de datos:** el esquema lo crea Hibernate (`ddl-auto=update`). No hay Flyway. Los datos de prueba se cargan con código y **solo si `ALMA_SEED_ENABLED=true`** (ver "Datos de demo, perfiles y pruebas").

## Estructura

```
alma_acompana/
├── backend/alma/        Spring Boot (código en src/main/java/ar/edu/utn/frm/gelab/alma/)
│   └── scripts/         ver-y-limpiar-historia.cypher (se corre a mano en el navegador de Neo4j)
├── frontend/            Vite + React (código en src/)
├── docs/                contratos, casos de uso y diagramas (ver "Documentos")
├── docker-compose.yml   postgres, neo4j, ollama
├── Dockerfile           vacío
├── .env / .env.example  variables (el .env no se sube)
└── CLAUDE.md
```

- **Back:** un paquete por módulo (`auth`, `persona`, `administrador`, `bienestar`, `chat`, `ejercicios` (catálogo; el juego de la persona mayor irá después en `juegos`), `historia`, `juegos`, `microcapsulas`, `recordatorios`), cada uno con `controller/`, `service/`, `repository/`, `domain/`, `dto/`. Los paquetes transversales son `config/`, `common/` (incluye `common/imagenes/`) e `ia/`. Las imágenes subidas desde el panel van a `alma.uploads.dir` (`./uploads` por defecto, fuera de Git).
- **Front:** `src/` con `App.tsx`, `main.tsx` e `index.css`, `components/ui/` (shadcn), `components/<tema>/` (`accesibilidad/`, `alma/`, `formularios/`, `layout/`), `features/<modulo>/` y `lib/` (ver "Carpetas del front"). El front ya está cerrado.

## Comandos

Estamos en Windows y esta herramienta usa Git Bash: usá `./mvnw`, no `.\mvnw`. (Nosotros, en PowerShell, usamos `.\mvnw.cmd`.) Los archivos del repo están en CRLF: no uses `sed -i` sobre ellos.

- **Bases de datos** (desde la raíz): `docker compose up -d`, `docker compose ps`, `docker compose stop`. **Nunca `docker compose down -v`:** borra los datos y el inicio de sesión de Ollama. Con Docker apagado el back y los tests no arrancan.
- **Back** (desde `backend/alma`): `./mvnw spring-boot:run`, `./mvnw clean compile`, `./mvnw test`.
- **Front** (desde `frontend`): `pnpm install`, `pnpm dev` (puerto 5173), `pnpm build`, `pnpm lint`, `pnpm test` (Vitest).

## Convenciones

### Back
- Todos los endpoints cuelgan de `/api`.
- Los controladores reciben y devuelven DTOs, nunca entidades.
- Validación con Bean Validation. Errores en formato Problem Details.
- El usuario sale siempre del token, nunca de un id en la URL o en el cuerpo.
- Nombres de dominio en español, como en el diagrama de clases (`Cuenta`, `PersonaMayor`, `nombreUsuario`).
- No quites el `TimeZone.setDefault(...)` de `AlmaApplication.main()`: evita un error de zona horaria entre Java en Windows y PostgreSQL 17.
- Swagger (`/swagger-ui.html`, `/v3/api-docs`) y `/actuator/health` son públicos en el MVP. Con el perfil `prod` Swagger y `/v3/api-docs` no existen (404); la salud sigue abierta.
- Los textos de error y de validación que ve la persona van en las clases `Mensajes*` (`MensajesError`, `MensajesPanel`, `MensajesChat`, etc.), no sueltos en el código.

### Front
- Alias `@` apunta a `src/`.
- Las llamadas al back pasan por `lib/api.ts`. En desarrollo el proxy de Vite manda `/api` al puerto 8080.
- Los colores y tipografías vienen del Figma: no inventes otros (única excepción: Lora cursiva, `@fontsource/lora`, para el nombre en el título de la imagen descargable del mapa de vida). Tipografías: Poppins para títulos y Source Sans 3 para el texto (`index.css`).
- **El Figma es ilustrativo:** si difiere de los casos de uso o del diagrama de clases en funcionalidad, ganan los documentos.
- Hay layout de celular y de escritorio.
- **Accesibilidad primero** (el público son personas mayores): letra grande y legible, alto contraste, botones grandes (mínimo 44 px), etiquetas siempre visibles, todo usable con teclado y mensajes de error simples.

### Carpetas del front
- Un módulo chico queda **plano**: `features/<modulo>/` con sus páginas, componentes, `api.ts`, `tipos.ts`, `schemas.ts` y `mensajes.ts` juntos.
- Con más de unos 10 archivos, se separa en `pages/` (una por ruta) y `components/` (piezas de esas páginas). Los hooks, `api.ts`, `tipos.ts`, `schemas.ts` y `mensajes.ts` quedan en la raíz del módulo. Hoy lo cumplen `recordatorios/`, `chat/`, `historia/`, `juegos/` y `lecturas/`.
- Lo que usan **varios módulos** va fuera de `features/`: componentes en `components/` (los genéricos sueltos, como `Chip.tsx`, y los demás en una carpeta por tema: `accesibilidad/`, `alma/` —solo el personaje—, `formularios/` y `layout/`; los de shadcn, en `components/ui/`) y funciones y textos generales en `lib/` (`api.ts`, `errores.ts`, `fechas.ts`, `mensajes.ts`).
- `index.ts` es la **entrada del módulo** y es **opcional**: solo hace falta cuando otro módulo o `App.tsx` importan varias cosas del módulo (por ejemplo, `recordatorios/`). Exporta solo lo que se usa desde afuera. Los módulos que usa únicamente su módulo padre o únicamente `App.tsx` no lo necesitan: se importan por su ruta (`@/features/<modulo>/…`). Dentro del módulo se importa con rutas relativas.
- Los textos que ve la persona viven en `mensajes.ts`, uno por módulo; los generales, en `lib/mensajes.ts`. Los esquemas de validación se llaman `schemas.ts`.
- Un componente o hook que ya existe en otro módulo no se duplica: se mueve a `components/` o `lib/` y se actualizan los imports.

## Decisiones tomadas

- React 19 (el informe decía 18).
- pnpm como único gestor del front.
- Sin Flyway.
- El operador es el `ADMINISTRADOR`: hay una sola tabla de cuentas y un solo login.
- Login por nombre de usuario y contraseña, JWT HS256 sin estado. El detalle está en el contrato.
- Neo4j guarda la historia de vida (paquete `historia/`). El modelo de IA lo usan el chat y el extractor de recuerdos, y solo se habla con él desde `ia/`.
- El proveedor de IA es Gemini por defecto (`GEMINI_API_KEY` en el `.env`). El nivel gratuito de Gemini no debe usarse con datos reales de personas. Con `IA_PROVEEDOR=ollama`, el contenedor de Ollama necesita `ollama signin` una vez (la sesión queda en el volumen).
- El modelo devuelve un bloque de razonamiento antes de la respuesta: nunca se le muestra a la persona mayor.
- La respuesta del chat se envía completa, con el indicador "Alma está pensando…" mientras tanto (sin streaming ni SSE).

## Datos de demo, perfiles y pruebas

- **Seed apagado por defecto** (`alma.seed.enabled`). Para tener las cuentas de demo `ana`, `carlos` y `admin`, poné `ALMA_SEED_ENABLED=true` en el `.env`. Con el seed prendido y sin recuerdos ni progreso para `ana`, también se carga su historia de ejemplo en Neo4j. En un despliegue real dejalo apagado: las cuentas de demo tienen contraseña conocida.
- **Perfil `prod`:** `SPRING_PROFILES_ACTIVE=prod` suma `application-prod.properties` y apaga Swagger. En desarrollo no se define ningún perfil.
- **Script de Neo4j:** `backend/alma/scripts/ver-y-limpiar-historia.cypher` es el único script: ver el grafo, contar, vista previa y borrado por persona (pide `$persona` y `$confirmar = true`; no borra a la `PersonaMayor` ni al guion). Se corre a mano en el navegador de Neo4j.
- **Pruebas del back:** corren contra las mismas bases de desarrollo (no hay base de pruebas). Usan cuentas `test_*` e ids de grafo negativos para no pisar los datos de `ana` y `carlos`, y el chat usa un modelo falso (no llama a Gemini). `src/test/resources/config/application.properties` prende el seed por su cuenta.

## Documentos

- `docs/especificaciones/contrato-login.md`: contrato de autenticación. Leelo antes de tocar el login, la seguridad o las rutas protegidas.
- `docs/especificaciones/contrato-recordatorios.md`: contrato del módulo de recordatorios (medicación, citas y avisos). Leelo antes de tocar `recordatorios/` o la agenda que usa el chat.
- `docs/especificaciones/contrato-panel-operador.md`: contrato del panel de operador (resumen, ejercicios, microcápsulas, imágenes y perfil del administrador). Leelo antes de tocar `features/admin/`, `features/ejercicios/`, `features/microcapsulas/`, `common/imagenes/` o `/api/admin/**`.
- `docs/especificaciones/contrato-animo.md`: contrato del registro de ánimo (CU-01: niveles, "Cambiar cómo me siento", pregunta contextual y widget). Leelo antes de tocar `bienestar/` o `features/bienestar/`.
- Casos de uso (CU-xx) y diagrama de clases (`DC_PPS`) del informe: son la fuente de verdad funcional. En el repo: `docs/casos-de-uso.md`, `docs/Diagrama_de_clases/` y `docs/Modelo_de_grafo/`.
- `docs/especificaciones/contrato-ia.md`: contrato del proveedor de IA (Gemini u Ollama, interruptor `IA_PROVEEDOR`). Leelo antes de tocar `ia/`.
- `docs/especificaciones/contrato-fecha-nacimiento.md`: fecha de nacimiento obligatoria de la persona mayor.
- `docs/especificaciones/contrato-juegos.md`: contrato del juego de estimulación (CU-05). Leelo antes de tocar el juego, el selector de ejercicios o el hub de Entretenimiento.
- `docs/especificaciones/contrato-chat.md`: contrato del chat con Alma (CU-02: saludo, guardrails, contexto del modelo y pantalla del chat). Leelo antes de tocar `chat/`, `ia/` o `features/chat/`.
- `docs/especificaciones/contrato-grafo.md`: contrato de la historia de vida (CU-03: grafo en Neo4j, guion y progreso, extractor, cola y repreguntas). Leelo antes de tocar `historia/` o el límite de pedidos de `ia/`.
- `docs/especificaciones/contrato-microcapsulas.md`: contrato de la lectura de microcápsulas (CU-06). Leelo antes de tocar el listado, el detalle o las guardadas de la persona mayor.
- `docs/especificaciones/contrato-historia-de-vida.md`: contrato de lo que la persona mayor ve y hace con su historia de vida (CU-02 criterio Nº 4, CU-03 y CU-04: guion y repreguntas en el chat, detalle del recuerdo, recuerdos en el prompt). Está en la v5: las secciones que reemplaza el mapa de vida llevan un aviso. Leelo antes de tocar `features/historia/`, el modo guion de `features/chat/` o `historia/`.
- `docs/especificaciones/contrato-mapa-de-vida.md`: contrato del mapa de vida (v5 de la historia de vida: mapa por etapas con panel, lista para letra grande, imagen descargable, "Seguir completando este recuerdo", botón fijo e invitación a pedido, burbujas y respuestas fijas del chat). **Gana sobre el contrato de historia de vida** donde difieren. Leelo antes de tocar la pantalla Historia de vida, `features/historia/` o el modo guion del chat.

## Plan por etapas

1. **Base común:** repo, Docker, login, layout y accesibilidad.
2. **Dos frentes en paralelo, sin Neo4j.** A: recordatorios, ánimo y chat. B: juegos, microcápsulas y panel de operador.
3. **Neo4j e historia de vida:** grafo, extractor a JSON, línea de tiempo.
4. **Cierre:** 16 casos de guardrails, pruebas de accesibilidad, `docker compose up --build` y README.
