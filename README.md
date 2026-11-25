# Alma Acompaña 🤖💜

Compañero digital para personas mayores. Una asistente llamada **Alma** conversa con la persona, le recuerda sus medicamentos y citas médicas, registra su ánimo, le propone ejercicios de memoria, le ofrece lecturas cortas (microcápsulas) y la ayuda a armar su **historia de vida** como un mapa de recuerdos.

El sistema se diseñó como una **API REST que puede integrarse a otros sistemas**, con una interfaz web pensada para accesibilidad (letra grande, alto contraste, botones grandes, lenguaje simple).

> Proyecto de Práctica Profesional Supervisada (PPS) · UTN Facultad Regional Mendoza · GE-Lab, Laboratorio de Gobierno Electrónico.
Autoría: Rodrigo Sanz y Paula Rodríguez.

---
## Resumen

El envejecimiento de la población plantea un desafío concreto: acompañar a las personas mayores en su día a día, sostener su autonomía y mantener activa su memoria, sin exigirles destrezas tecnológicas. Alma Acompaña responde a ese desafío con una aplicación centrada en la persona y construida como una API reutilizable.

### Qué la distingue

- **Una asistente conversacional segura.** Alma conversa con un modelo de lenguaje acotado por guardrails: detecta frases de riesgo, ofrece líneas de ayuda, no da consejos médicos y no inventa información que no tiene. El comportamiento está validado con un conjunto de 20 casos de prueba automatizados.
- **La historia de vida como grafo.** Las respuestas de la persona a un guion de 21 preguntas se transforman en recuerdos, personas y lugares conectados entre sí en una base de grafos (Neo4j). Esa historia se muestra como un mapa por etapas y el chat la usa para conversar con más cercanía.
- **Estimulación cognitiva y bienestar en un solo lugar.** Ejercicios de memoria con niveles y progreso, lecturas cortas de salud, registro diario de ánimo y recordatorios de medicamentos y citas médicas.
- **Accesibilidad desde el diseño.** Letra ajustable al 100, 125 o 150 %, alto contraste, botones grandes y lenguaje simple, pensados para quien no está habituado a usar aplicaciones.
- **Autonomía del equipo de contenido.** Un panel de operador permite crear, editar y dar de baja ejercicios y microcápsulas, con imágenes, sin intervención de desarrolladores.
- **Lista para integrarse.** Expone una API REST con autenticación JWT y documentación OpenAPI, de modo que puede incorporarse a otros sistemas como un servicio más.
- **Fácil de desplegar.** Todo el sistema (frontend, backend, bases de datos y modelo de lenguaje) se levanta con un solo comando de Docker.
- **Privacidad como criterio de diseño.** El proyecto documenta qué datos recibe el modelo de lenguaje, advierte sobre el uso del nivel gratuito con datos reales y se desarrolló y probó íntegramente con datos sintéticos.

## Qué incluye

| Módulo | Qué hace |
|---|---|
| Chat con Alma | Conversación con un modelo de lenguaje, con guardrails (límites de seguridad) y contexto del día |
| Recordatorios | Medicamentos y citas médicas, con avisos |
| Ánimo | Registro diario de cómo se siente la persona |
| Ejercicios | Catálogo de ejercicios de memoria (refranes, series, asociaciones, memoria), con niveles y progreso |
| Microcápsulas | Lecturas cortas de salud y bienestar, con imagen, que la persona puede guardar |
| Historia de vida | Guion de 21 preguntas; las respuestas se transforman en recuerdos, personas y lugares dentro de un grafo, y se muestran como mapa por etapas |
| Panel de operador | Alta, edición y baja de ejercicios y microcápsulas, con subida de imágenes |

## Arquitectura

- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS, shadcn/ui. Gestor de paquetes: **pnpm**.
- **Backend:** Java 21, Spring Boot 4, Spring Security con JWT, Spring AI. API REST documentada con Swagger/OpenAPI.
- **PostgreSQL 17:** cuentas, recordatorios, ánimos, ejercicios, intentos, microcápsulas.
- **Neo4j 5 (Community):** la historia de vida como grafo (personas mayores, recuerdos, personas, lugares, preguntas del guion).
- **Modelo de lenguaje:** Gemini (por defecto) u Ollama, según `IA_PROVEEDOR`.

El `docker-compose.yml` levanta PostgreSQL, Neo4j, Ollama (opcional), el backend y el frontend. Para desarrollar también es posible ejecutar el backend y el frontend por separado (ver "Cómo levantar el sistema").

## Requisitos

- Docker Desktop (alcanza para levantar todo, opción A)
- Para desarrollar (opción B): Java 21, Node.js y pnpm
- Python 3 (solo para el script de pruebas de guardrails)
- Una clave de Gemini (https://aistudio.google.com/apikey), si se usa `IA_PROVEEDOR=gemini`

## Configuración

1. Copiar la plantilla de variables de entorno en la raíz del proyecto:
   ```bash
   cp .env.example .env
   ```
2. Completar en `.env`, como mínimo:
   - `POSTGRES_PASSWORD` y `NEO4J_PASSWORD` (Neo4j pide al menos 8 caracteres)
   - `JWT_SECRET`: texto aleatorio de 32 caracteres o más (`openssl rand -base64 48`)
   - `GEMINI_API_KEY`
3. Para cargar las cuentas y los datos de demostración, definir `ALMA_SEED_ENABLED=true` (ver "Datos de demo").

El archivo `.env` no se sube a Git. La clave de Gemini nunca debe escribirse en el código ni en `.env.example`.

## Cómo levantar el sistema

Hay dos modalidades. La **A** es la recomendada para usar la aplicación. La **B** está pensada para desarrollar.

### A. Todo con Docker

Requisito: Docker Desktop abierto y corriendo.

```bash
docker compose up -d --build
```

- Tarda cerca de un minuto la primera vez: el backend espera a que Postgres y Neo4j estén sanos, y el frontend espera al backend. Con `docker compose ps` se ve el estado.
- La app queda en **http://localhost:8081** (o el puerto de `FRONTEND_PORT` en el `.env`).
- Solo se publica el frontend. El backend y Swagger no son accesibles desde afuera: nginx solo reenvía `/api` y `/imagenes`.
- Los tests del backend no se corren al construir la imagen, porque usan las bases de desarrollo.
- Si cambia el código, se reconstruye con `docker compose up -d --build`.

**Apagar y volver a prender**
```bash
docker compose stop      # apaga y deja todo como está
docker compose start     # lo vuelve a prender
docker compose down      # apaga y borra los contenedores; los datos se conservan
```

> **No debe ejecutarse nunca `docker compose down -v`.** El `-v` borra los volúmenes: las bases de datos, las imágenes subidas desde el panel y la sesión de Ollama.

**Qué se guarda y dónde**
- Postgres, Neo4j y las imágenes subidas desde el panel viven en volúmenes de Docker, y sobreviven a `down` y a reiniciar la computadora.
- El volumen de imágenes (`alma_uploads`, que Docker llama `<proyecto>_alma_uploads`) arranca vacío. Si se parte de un entorno de desarrollo local con imágenes en `backend/alma/uploads`, deben copiarse una sola vez (en Git Bash, desde la raíz del proyecto):

```bash
  MSYS_NO_PATHCONV=1 docker run --rm -v alma_alma_uploads:/destino -v "$(pwd)/backend/alma/uploads:/origen:ro" alpine sh -c "cp -a /origen/. /destino/ && chown -R 100:101 /destino"
```
  
  El nombre real del volumen puede verificarse con `docker volume ls`.

**Opciones útiles del `.env`**
- `ALMA_SEED_ENABLED`: carga las cuentas y los datos de demostración (ana, carlos y admin). Está desactivado por defecto. En un despliegue real debe permanecer en `false`, ya que esas cuentas tienen contraseñas conocidas.
- `SPRING_PROFILES_ACTIVE=prod`: apaga Swagger.
- `IA_PROVEEDOR=ollama`: usa Ollama en vez de Gemini (opcional). Necesita iniciar sesión una vez: `docker exec -it alma-ollama ollama signin`.

### B. Para desarrollar

**1. Solo las bases de datos** (desde la raíz)
```bash
docker compose up -d postgres neo4j
```

**2. Backend** (desde `backend/alma`)
```bash
./mvnw spring-boot:run
```
Queda en `http://localhost:8080`. Swagger: `http://localhost:8080/swagger-ui.html`.

Si se modifica el `.env`, es necesario reiniciar el backend: lo lee únicamente al arrancar.

**3. Frontend** (desde `frontend`)
```bash
pnpm install
pnpm dev
```
Queda en `http://localhost:5173`. Las llamadas a `/api` y a `/imagenes` pasan por el proxy de Vite hacia el backend.

> No deben ejecutarse a la vez el backend del IDE y el de Docker sobre las mismas bases de datos.

**Ver el grafo:** Neo4j Browser en `http://localhost:7474`, con el usuario `neo4j` y la clave del `.env`.

> Si hay un Neo4j instalado en el equipo, debe detenerse antes: comparte el puerto 7687 con el de Docker.

## Datos de demo

Con `ALMA_SEED_ENABLED=true`, al arrancar el backend se cargan (de forma idempotente, sin duplicar):

- Cuentas de demo: `ana`, `carlos` (personas mayores) y `admin` (panel de operador), con una contraseña conocida de demostración. **No usar nunca este seed en un despliegue real.**
- El guion de 21 preguntas de la historia de vida.
- 10 microcápsulas y 57 ejercicios activos.

Con el seed apagado (`false`, valor por defecto) no se crea ninguna cuenta ni contenido.

## Pruebas

```bash
# Backend (necesita Postgres y Neo4j arriba)
cd backend/alma
./mvnw test

# Frontend
cd frontend
pnpm lint
pnpm test
pnpm build
```

Los tests del backend usan las bases de desarrollo y crean cuentas `test_*` que limpian al terminar.

**Guardrails del chat** (usa Gemini y necesita el backend levantado):
```bash
cd backend/alma
python scripts/guardrails_gemini.py --extras
```
Ejecuta los casos de seguridad del chat. El caso T3 es intermitente porque depende de la respuesta del modelo; un error 503 de Gemini no es una falla del sistema.

## Estructura de carpetas

```
alma_acompana/
├── backend/alma/      Spring Boot + Dockerfile (código en src/main/java/ar/edu/utn/frm/gelab/alma/)
│   ├── scripts/       Cypher de la historia de demo y de limpieza, script de guardrails
│   └── src/main/resources/   guion, prompts del chat y textos de microcápsulas
├── frontend/          React + Vite + Dockerfile y nginx.conf
├── docs/              Contratos de cada módulo, casos de uso, modelo de grafo y diagrama de clases
├── docker-compose.yml PostgreSQL, Neo4j, Ollama, backend y frontend
└── .env.example       Plantilla de configuración
```

## Integración con otros sistemas

La aplicación expone una API REST que cualquier sistema puede consumir. La interfaz web es un cliente más de esa misma API.

**Autenticación.** Se inicia sesión con `POST /api/auth/login` (usuario y contraseña), que devuelve un token JWT. Ese token se envía en cada pedido posterior en el encabezado `Authorization: Bearer <token>`.

**Permisos por rol.**
- `PERSONA_MAYOR`: chat, ánimo, recordatorios, ejercicios, microcápsulas, historia de vida, perfil y accesibilidad (`/api/chat`, `/api/animo`, `/api/recordatorios`, `/api/juegos`, `/api/microcapsulas`, `/api/historia`, `/api/perfil`, `/api/accesibilidad`).
- `ADMINISTRADOR`: gestión de ejercicios, microcápsulas e imágenes (`/api/admin/...`).

**Documentación de la API.** En un entorno de desarrollo, la especificación OpenAPI con todos los endpoints y sus datos de entrada y salida está en:

- Interfaz: `http://localhost:8080/swagger-ui.html`
- JSON: `http://localhost:8080/v3/api-docs`

Con el perfil `prod` (`SPRING_PROFILES_ACTIVE=prod`) Swagger y `/v3/api-docs` se apagan.

**Alcance actual.** La API está lista para integrarse. Las cuentas se crean y se autentican dentro de Alma; la conexión con el sistema de identidad o con los datos de una institución concreta no está implementada y dependerá de cada caso.

## Datos personales y salud

Alma maneja datos sensibles: ánimo, medicamentos, citas médicas y relatos de vida.

- **El nivel gratuito de Gemini no debe usarse con datos reales.** Google puede usar y leer ese contenido. Con personas reales hay que usar Gemini con facturación activa, un modelo propio (`IA_PROVEEDOR=ollama` con un modelo local) o un servicio con contrato de empresa.
- Todas las pruebas y capturas del proyecto se hicieron con datos sintéticos.

## Limitaciones conocidas

**Del asistente**
- **Los recuerdos llegan al chat por coincidencia de nombres, no por significado.** El chat busca en el mensaje los nombres de personas y lugares del grafo y unas frases fijas de etapa ("de chica", "cuando era joven"…). Si la persona nombra a alguien o usa una de esas frases, Alma usa el recuerdo. Si solo menciona el vínculo ("¿cómo se llamaba mi mamá?"), responde que no tiene esa información, sin inventar.
- Si Neo4j acaba de arrancar, la lectura de recuerdos puede superar el tope de 2 segundos. En ese caso el chat responde sin recuerdos; desde el segundo mensaje funciona con normalidad.
- La detección automática de riesgo cubre las frases directas. Una frase indirecta de malestar recibe la respuesta con las líneas de ayuda, pero sin activar la bandera `alarma`.
- El nivel gratuito de Gemini tiene límites de uso y puede devolver errores 503 en momentos de alta demanda. Además, algunas respuestas del modelo varían entre ejecuciones, por lo que un caso de las pruebas de guardrails (T3) es intermitente.

**De la infraestructura y las pruebas**
- Las imágenes se cargan desde el panel, no por seed. Viven en el volumen `alma_uploads` (en Docker) o en `backend/alma/uploads/` (en desarrollo), y deben respaldarse aparte.
- Los tests del backend usan las bases de desarrollo, no bases aisladas; por eso no se ejecutan al construir la imagen de Docker.
- No hay integración real con sistemas externos: la API está lista para integrarse, pero la integración no se implementó.

---
> Desarrollado por Rodrigo Sanz y Paula Rodríguez · PPS, UTN Facultad Regional Mendoza · GE-Lab · 2026
