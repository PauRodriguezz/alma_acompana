# Contrato: fecha de nacimiento obligatoria · Alma Acompaña

**Estado:** propuesta para revisar entre los dos. Es un cambio chico y previo al resto del trabajo de `feature/grafo`. Se hace en esa misma rama, **en un commit propio** (separado de los del grafo, para poder aislarlo si hace falta), y antes de empezar con el grafo. Si algo cambia, se cambia primero acá.

**Fuente de verdad:** el diagrama de clases (`docs/Diagrama_de_clases/diagrama-de-clases.md`), donde `PersonaMayor.fecha_nacimiento` es un atributo de tipo `Date` sin marca de opcional, y `docs/contrato-login.md` (sección 4.4, perfil). Hoy la entidad y el perfil permiten que la fecha sea nula; este contrato lo corrige para que coincida con el diagrama.

**Rama:** `feature/grafo` (decisión del equipo; no se crea una rama aparte).

**Responsable:** quien tenga la rama libre; el cambio es mecánico.

---

## 1. Por qué

El módulo de historia de vida calcula el año de un recuerdo a partir de la edad que la persona menciona ("tenía como 15 años") y de su fecha de nacimiento, y también deriva la etapa de vida y estima el año con la franja de edad de la pregunta. Sin fecha de nacimiento no se puede hacer nada de eso. Ninguna pantalla ni endpoint crea personas mayores: solo el código de datos de prueba, que ya les pone fecha. Por eso se puede exigir sin romper ningún flujo.

## 2. Alcance

**Incluye:** volver obligatoria la fecha de nacimiento de `PersonaMayor` en el back, ajustar las pruebas que crean personas sin fecha, quitar el caso "sin fecha cargada" del perfil del front y actualizar los documentos que lo mencionan.

**No incluye:** crear personas desde la aplicación, editar la fecha, validar que sea una fecha razonable (por ejemplo, no futura), ni ningún cambio en el módulo de historia de vida.

## 3. Decisión

`PersonaMayor.fechaNacimiento` pasa a ser **obligatoria** (`@Column(nullable = false)`). El tipo sigue siendo `LocalDate`. `GET /api/perfil` devuelve siempre `fechaNacimiento` como `yyyy-MM-dd`, nunca `null`.

## 4. Qué cambia

**Back**

| Archivo | Cambio |
|---|---|
| `persona/domain/PersonaMayor.java` | `@Column(nullable = false)` en `fechaNacimiento` |
| `persona/dto/PerfilDto.java` | Quitar del comentario la frase "puede ser null" |
| `config/DatosDePrueba.java` | Sin cambios: `ana` y `carlos` ya tienen fecha |

**Pruebas del back.** Cada una crea una `PersonaMayor` sin fecha y, con la columna obligatoria, fallaría al guardarla. Agregar `persona.setFechaNacimiento(LocalDate.of(1950, 5, 12))` antes de guardar en:

- `recordatorios/RecordatorioIntegrationTests.java`
- `bienestar/AnimoIntegrationTests.java`
- `microcapsulas/LecturaMicrocapsulaIntegrationTests.java`
- `chat/ChatIntegrationTests.java`
- `juegos/JuegoIntegrationTests.java`

`juegos/service/JuegoServiceTest.java` crea una persona sin guardarla en la base, así que no necesita cambios.

**`persona/PerfilIntegrationTests.java`:** la única `PersonaMayor` sin fecha que crea es la del helper `crearPersona`, y solo la usa la prueba `sinFechaDeNacimientoDevuelveNull`. Por eso se **borran** esa prueba, la constante `SIN_FECHA`, su uso en la limpieza, el helper `crearPersona` y los imports que queden sin uso. No se agrega una línea de fecha a código que ya no se usa.

**Front**

| Archivo | Cambio |
|---|---|
| `features/perfil/api.ts` | `fechaNacimiento: string` (sin `null`) y ajustar el comentario |
| `features/perfil/PerfilPage.tsx` | Mostrar siempre `fechaCorta(estado.perfil.fechaNacimiento)`, sin la rama "sin cargar" ni el import de `FECHA_SIN_CARGAR` |
| `features/perfil/mensajes.ts` | Borrar `FECHA_SIN_CARGAR` si ya no se usa en ningún lado |

**Documentos**

- `docs/contrato-login.md`, sección 4.4: la fecha viene siempre como `yyyy-MM-dd`; quitar "o `null` si la persona no la tiene cargada". Agregar una entrada al historial de cambios.
- `docs/Diagrama_de_clases/diagrama-de-clases.md`: sin cambios (ya la muestra como obligatoria).

**No se tocan:** `SecurityConfig`, `application.properties`, `pom.xml`, los demás módulos ni nada del grafo.

## 5. Tu base local

El esquema lo crea Hibernate con `ddl-auto=update`. Si en tu base ya hay personas con la fecha vacía, Hibernate no puede volver obligatoria la columna y el back **no arranca**. Antes de levantar el back con el cambio, completar las fechas que falten, por ejemplo:

```sql
UPDATE persona_mayor SET fecha_nacimiento = '1950-01-01' WHERE fecha_nacimiento IS NULL;
```

El nombre de la tabla es el que Hibernate le pone por defecto (`persona_mayor`).

Con los datos de prueba (`ana` y `carlos` tienen fecha) no hace falta hacer nada.

**No usar `docker compose down -v` para esto:** borra los volúmenes de todos los servicios, incluidos los de PostgreSQL y Neo4j y el inicio de sesión de Ollama, y `CLAUDE.md` lo prohíbe. Recrear la base es una decisión de la persona, a mano.

## 6. Pruebas y criterio de "listo"

**Automáticas:** todas las pruebas del back que ya existen pasan (con los comandos de `CLAUDE.md`). No se agregan pruebas nuevas.

**Manuales:** con el back y el front corriendo, `ana` entra a Perfil y ve su fecha de nacimiento (12/05/1950); no queda ningún texto del tipo "sin cargar" ni en el código ni en la pantalla. En el front, `pnpm lint` y `pnpm build` pasan.

**Listo cuando:** el back arranca sobre una base con todas las fechas cargadas, todas las pruebas pasan, el perfil de `ana` y de `carlos` muestra la fecha, y `docs/contrato-login.md` ya no dice que puede ser `null`.

## 7. Historial de cambios

- **v1 (08/11/2026):** primera propuesta, contra el commit `d35f1a9` de `main`.
- **v2 (08/11/2026):** el cambio se hace en `feature/grafo`, en un commit propio, y no en una rama aparte; se borra también el helper `crearPersona` de `PerfilIntegrationTests` (código muerto al quitar la prueba); se quita la recomendación de `docker compose down -v`; se saca la sección "Cómo usar con Claude Code".