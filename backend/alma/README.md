# Alma Acompaña · Backend

API REST del sistema. Java 21, Spring Boot 4, Spring Security con JWT, Spring Data (PostgreSQL y Neo4j) y Spring AI (Gemini u Ollama).

Para levantar todo el sistema (bases de datos, frontend y variables de entorno) mirá el [README de la raíz](../../README.md).

## Comandos

Desde esta carpeta (`backend/alma/`), con las bases de datos arriba (`docker compose up -d` en la raíz) y el `.env` configurado:

```bash
./mvnw spring-boot:run   # levanta la API en http://localhost:8080
./mvnw test              # corre los tests (necesita Postgres y Neo4j arriba)
```

Documentación interactiva de la API (Swagger): `http://localhost:8080/swagger-ui.html`. Con el perfil `prod` queda apagada.

## Estructura

El código está en `src/main/java/ar/edu/utn/frm/gelab/alma/`, con un paquete por módulo:

| Paquete | Qué hace |
|---|---|
| `auth` | Inicio de sesión y tokens JWT |
| `persona`, `administrador` | Cuentas de la persona mayor y del operador |
| `chat` | Conversación con Alma y guardrails (límites de seguridad) |
| `ia` | Conexión con el modelo de lenguaje (Gemini u Ollama) |
| `historia` | Historia de vida: guion, extracción de recuerdos y grafo en Neo4j |
| `bienestar` | Registro de ánimo |
| `recordatorios` | Medicamentos y citas médicas |
| `ejercicios`, `juegos` | Catálogo de ejercicios y rondas de juego |
| `microcapsulas` | Lecturas cortas con imagen |
| `common` | Manejo de errores, imágenes, seguridad y utilidades |
| `config` | Seguridad, JWT, Swagger y carga de datos de demo |

Dentro de cada módulo, el código se separa en capas: `controller` (endpoints), `service` (reglas de negocio), `repository` (acceso a datos), `domain` (entidades) y `dto` (formas de entrada y salida).

Otras carpetas:
- `src/main/resources/` → configuración (`application.properties`, perfil `prod`), guion de preguntas, prompts del chat y textos de las microcápsulas.
- `src/test/` → tests, con la misma estructura de paquetes.
- `scripts/` → consultas Cypher para cargar o limpiar la historia de demo y script de pruebas de guardrails.

## Configuración

Se hace con variables de entorno, leídas del archivo `.env` de la raíz (plantilla: `.env.example`). Las más importantes: claves de las bases, `JWT_SECRET`, `IA_PROVEEDOR` y `GEMINI_API_KEY`. Los datos de demo solo se cargan con `ALMA_SEED_ENABLED=true`.

Si cambiás el `.env`, reiniciá el backend: lo lee únicamente al arrancar.

## Con Docker

El `Dockerfile` de esta carpeta construye el backend en dos etapas (Maven y luego JRE 21) y corre con un usuario sin privilegios. Se levanta junto con las bases con `docker compose up -d --build` desde la raíz (ver `../../README.md`). La imagen se construye sin correr los tests.