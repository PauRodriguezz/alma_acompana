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
- Al terminar una tarea, decí en pocas líneas qué archivos tocaste, cómo probarlo (el comando exacto) y qué falta.

## Stack

- **Back:** Java 21, Spring Boot 4.1.1, Maven (`./mvnw`), Spring Security con JWT propio (OAuth2 Resource Server), JPA + PostgreSQL, Spring Data Neo4j, Spring AI + Ollama (modelo `gemma4:cloud`), springdoc 3.1.1 (Swagger en `/swagger-ui.html`), Actuator, Lombok.
- **Front:** React 19 + TypeScript, Vite, Tailwind, shadcn/ui, react-router-dom, TanStack Query, react-hook-form + zod, Hugeicons, fuentes Poppins y Source Sans 3, date-fns.
- **Gestor de paquetes del front: pnpm.** Nunca uses npm ni yarn.
- **Infra:** Docker Compose con PostgreSQL 17, Neo4j 5.26 y Ollama 0.40.6. La app todavía no está en el compose y el `Dockerfile` está vacío hasta la Etapa 3.
- **Base de datos:** el esquema lo crea Hibernate (`ddl-auto=update`). No hay Flyway. Los datos de prueba se cargan con código.

## Estructura

```
alma_acompana/
├── backend/alma/        Spring Boot (código en src/main/java/ar/edu/utn/frm/gelab/alma/)
├── frontend/            Vite + React (código en src/)
├── docs/                documentación (contrato-login.md, contrato-recordatorios.md)
├── docker-compose.yml   postgres, neo4j, ollama
├── Dockerfile           vacío hasta la Etapa 3
├── .env / .env.example  variables (el .env no se sube)
└── CLAUDE.md
```

- **Back:** un paquete por módulo (`auth`, `persona`, `administrador`, `bienestar`, `chat`, `ejercicios` (catálogo; el juego de la persona mayor irá después en `juegos`), `historiavida`, `juegos`, `microcapsulas`, `recordatorios`), cada uno con `controller/`, `service/`, `repository/`, `domain/`, `dto/`. Los paquetes transversales son `config/`, `common/` e `ia/`.
- **Front:** `src/` con `app/`, `styles/`, `components/ui/` (shadcn), `components/alma/`, `features/<modulo>/` y `lib/`.

## Comandos

Estamos en Windows y esta herramienta usa Git Bash: usá `./mvnw`, no `.\mvnw`. (Nosotros, en PowerShell, usamos `.\mvnw.cmd`.)

- **Bases de datos** (desde la raíz): `docker compose up -d`, `docker compose ps`, `docker compose stop`. **Nunca `docker compose down -v`:** borra los datos y el inicio de sesión de Ollama.
- **Back** (desde `backend/alma`): `./mvnw spring-boot:run`, `./mvnw clean compile`, `./mvnw test`.
- **Front** (desde `frontend`): `pnpm install`, `pnpm dev` (puerto 5173), `pnpm build`, `pnpm lint`.

## Convenciones

### Back
- Todos los endpoints cuelgan de `/api`.
- Los controladores reciben y devuelven DTOs, nunca entidades.
- Validación con Bean Validation. Errores en formato Problem Details.
- El usuario sale siempre del token, nunca de un id en la URL o en el cuerpo.
- Nombres de dominio en español, como en el diagrama de clases (`Cuenta`, `PersonaMayor`, `nombreUsuario`).
- No quites el `TimeZone.setDefault(...)` de `AlmaApplication.main()`: evita un error de zona horaria entre Java en Windows y PostgreSQL 17.
- Swagger (`/swagger-ui.html`, `/v3/api-docs`) y `/actuator/health` son públicos en el MVP.

### Front
- Alias `@` apunta a `src/`.
- Las llamadas al back pasan por `lib/api.ts`. En desarrollo el proxy de Vite manda `/api` al puerto 8080.
- Los colores y tipografías vienen del Figma: no inventes otros. Hoy el front usa Inter (valor por defecto de shadcn); hay que reemplazarla por Poppins y Source Sans 3.
- **El Figma es ilustrativo:** si difiere de los casos de uso o del diagrama de clases en funcionalidad, ganan los documentos.
- Hay layout de celular y de escritorio.
- **Accesibilidad primero** (el público son personas mayores): letra grande y legible, alto contraste, botones grandes (mínimo 44 px), etiquetas siempre visibles, todo usable con teclado y mensajes de error simples.

## Decisiones tomadas

- React 19 (el informe decía 18).
- pnpm como único gestor del front.
- Sin Flyway.
- El operador es el `ADMINISTRADOR`: hay una sola tabla de cuentas y un solo login.
- Login por nombre de usuario y contraseña, JWT HS256 sin estado. El detalle está en el contrato.
- Neo4j se usa recién en la Etapa 2 (historia de vida). El modelo de IA se usa desde el chat, en la Etapa 1.
- El contenedor de Ollama necesita `ollama signin` una vez (la sesión queda en el volumen).
- El modelo devuelve un bloque de razonamiento antes de la respuesta: nunca se le muestra a la persona mayor.
- La respuesta del chat se envía en streaming (SSE).

## Documentos

- `docs/contrato-login.md`: contrato de autenticación. Leelo antes de tocar el login, la seguridad o las rutas protegidas.
- `docs/contrato-recordatorios.md`: contrato del módulo de recordatorios (medicación, citas y avisos). Leelo antes de tocar `recordatorios/` o la agenda que usa el chat.
- Casos de uso (CU-xx) y diagrama de clases (`DC_PPS`) del informe: son la fuente de verdad funcional.

## Plan por etapas

1. **Base común:** repo, Docker, login, layout y accesibilidad.
2. **Dos frentes en paralelo, sin Neo4j.** A: recordatorios, ánimo y chat. B: juegos, microcápsulas y panel de operador.
3. **Neo4j e historia de vida:** grafo, extractor a JSON, línea de tiempo.
4. **Cierre:** 16 casos de guardrails, pruebas de accesibilidad, `docker compose up --build` y README.
