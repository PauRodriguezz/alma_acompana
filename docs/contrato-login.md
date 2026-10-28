# Contrato de autenticación (login) · Alma Acompaña

**Estado:** propuesta para acordar entre los dos. Una vez aprobada, es la referencia única para back y front. Si algo cambia, se cambia primero acá y se avisa al otro.

**Fuente de verdad:** casos de uso CU-12 (login del administrador) y CU-14 (login y logout de la persona mayor), y el diagrama de clases (clase `Cuenta`). Si este contrato contradice esos documentos, ganan los documentos y se corrige el contrato.

**Dónde va:** `docs/contrato-login.md`. El `CLAUDE.md` debe enlazarlo y tiene que estar en la **raíz** del repo, porque Claude Code lo lee desde la carpeta donde se lo abre.

**Última revisión contra el repo:** commit `b47fe79` ("configuracion Swagger", 23/10/2026).

---

## 1. Alcance

**Incluye:** login, consulta de la cuenta actual, token, roles, protección de rutas, formato de errores, usuarios de prueba y preferencias de accesibilidad (provisional).

**No incluye (la documentación no lo contempla o queda para después):** registro de cuentas, recuperación de contraseña, refresh token, cierre de sesión en el servidor, bloqueo por intentos fallidos, verificación en dos pasos.

---

## 2. Decisiones

| Tema | Decisión | Motivo |
|---|---|---|
| Credenciales | **Nombre de usuario + contraseña** | CU-12, CU-14 y el DC (`Cuenta.nombre_usuario`, `Cuenta.contrasenia`) |
| Token | JWT firmado con HS256, clave `JWT_SECRET` (mínimo 32 caracteres) | Ya está previsto en `application.properties`. Es el mismo tipo de token (JWT) que usaría un proveedor externo (nota de OSEP en la arquitectura), así que el emisor se podría cambiar por configuración |
| Sesión | Sin estado (stateless). No hay "logout" en el servidor: cerrar sesión es que el front descarte el token | Más simple; el token vence solo |
| Sesión persistente | La sesión se mantiene por defecto (casilla "Mantener sesión iniciada", tildada de entrada). Con la casilla tildada el front guarda el token en `localStorage`; destildada, en `sessionStorage` | CU-14 |
| Envío del token | Encabezado `Authorization: Bearer <token>` | Estándar |
| Expiración | 1440 min (1 día) por defecto; 43200 min (30 días) si el login pide `mantenerSesion: true`. Propiedades `alma.security.jwt.expiration-minutes` y `alma.security.jwt.remember-expiration-minutes` | Una persona mayor no debería tener que reingresar seguido |
| Contraseñas | Siempre encriptadas con BCrypt (`PasswordEncoder`). Nunca en texto plano, ni en logs | Seguridad básica |
| Roles | `PERSONA_MAYOR` y `ADMINISTRADOR`. **Resuelto:** el operador es el `ADMINISTRADOR`; hay una sola tabla de cuentas y un solo mecanismo de login | DC y CU-12 |
| CSRF | Desactivado | No se usan cookies de sesión; el token va en un encabezado |
| CORS | No se configura | En desarrollo el proxy de Vite reenvía `/api` al 8080; en producción front y back salen del mismo origen |
| Autoridad | El back decide siempre. El front oculta pantallas solo por comodidad | Un usuario puede saltarse el front |
| Identidad | El back toma el usuario **del token**. Nunca del `id` en la URL o en el cuerpo | Evita que alguien lea datos de otra persona |
| Errores | Formato Problem Details (RFC 9457) | Estándar, Spring lo soporta |
| Cierre de sesión | El front pide confirmación antes de cerrar sesión y luego descarta el token | CU-14 |

---

## 3. Convenciones de la API

- Todo cuelga de `/api`.
- JSON en UTF-8, propiedades en `camelCase`.
- Enums en `MAYUSCULAS_CON_GUION_BAJO`.
- Fechas en ISO 8601. Jackson usa la zona `America/Argentina/Buenos_Aires` (`spring.jackson.time-zone`); esa propiedad afecta solo a cómo se escriben las fechas en el JSON.
- Los ids son numéricos (`Long`).
- Los textos de error que llegan a la persona están en español, con frases cortas y sin jerga.

---

## 4. Endpoints

### 4.1 `POST /api/auth/login` (público)

**Request**
```json
{ "nombreUsuario": "ana", "contrasenia": "Alma2026!", "mantenerSesion": true }
```

**Validaciones:** `nombreUsuario` y `contrasenia` obligatorios. `mantenerSesion` es opcional (por defecto `true`). El back compara el nombre de usuario sin espacios al inicio y al final y sin distinguir mayúsculas.

**200 OK**
```json
{
  "token": "eyJhbGciOi...",
  "tipoToken": "Bearer",
  "expiraEnSegundos": 86400,
  "usuario": {
    "id": 1,
    "nombreUsuario": "ana",
    "nombre": "Ana",
    "apellido": "Pérez",
    "rol": "PERSONA_MAYOR",
    "preferenciasAccesibilidad": { "tamanioLetra": "NORMAL", "altoContraste": false }
  }
}
```

`preferenciasAccesibilidad` solo viene para `PERSONA_MAYOR`; para `ADMINISTRADOR` viene `null`. Así el front aplica la letra y el contraste apenas la persona entra, sin otra llamada (CU-13).

**Errores**
- `400`: faltan campos.
- `401`: usuario inexistente o contraseña incorrecta. Siempre el mismo mensaje, para no revelar si el usuario existe.

### 4.2 `GET /api/auth/me` (autenticado)

Devuelve la cuenta del token. El front lo usa al cargar la app para recuperar la sesión.

**200 OK**
```json
{
  "id": 1,
  "nombreUsuario": "ana",
  "nombre": "Ana",
  "apellido": "Pérez",
  "rol": "PERSONA_MAYOR",
  "preferenciasAccesibilidad": { "tamanioLetra": "NORMAL", "altoContraste": false }
}
```

**Errores:** `401` si falta el token, es inválido o venció.

> `nombre` y `apellido` no están en `Cuenta`: salen de `PersonaMayor` o de `Administrador`, según el rol (cada una tiene una relación 1 a 1 con `Cuenta` en el DC). `id` es el de la cuenta.

### 4.3 `GET /api/accesibilidad` y `PUT /api/accesibilidad` (solo `PERSONA_MAYOR`)

Preferencias de la persona del token (CU-13). Según el DC, `PreferenciaAccesibilidad` pertenece a `PersonaMayor` (relación 1 a 1) y tiene solo dos campos:

```json
{ "tamanioLetra": "NORMAL", "altoContraste": false }
```

- `tamanioLetra`: `NORMAL`, `GRANDE` o `MUY_GRANDE` (enum `TamanioLetra`).
- `altoContraste`: `true` o `false`.
- Al crear una persona mayor, se crea su preferencia con `NORMAL` y `false`.
- `PUT` recibe el objeto completo y devuelve el objeto guardado.
- Un `ADMINISTRADOR` recibe `403` en estas rutas.

---

## 5. Token

**Claims**

| Claim | Valor |
|---|---|
| `iss` | `alma` |
| `sub` | id de la cuenta, como texto |
| `rol` | `PERSONA_MAYOR` o `ADMINISTRADOR` |
| `iat` / `exp` | emisión y vencimiento |

No se incluyen nombre, apellido ni datos sensibles: para eso está `/api/auth/me`.

**Detalle importante para el back:** Spring Security lee por defecto el claim `scope` para los permisos. Hay que configurar un conversor que lea el claim `rol` y le agregue el prefijo `ROLE_`, para poder usar `hasRole("ADMINISTRADOR")`.

---

## 6. Reglas de acceso

| Ruta | Quién puede |
|---|---|
| `POST /api/auth/login` | Público |
| `GET /actuator/health` | Público (lo usa el healthcheck de Docker) |
| `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` | Público en el MVP (cerrar en una entrega real) |
| `/api/admin/**` | Solo `ADMINISTRADOR` |
| Resto de `/api/**` | Cualquier cuenta autenticada. Cada módulo verifica que el dato pertenezca a quien lo pide |
| Rutas que no empiezan con `/api` (archivos del front) | Públicas. La regla para devolver `index.html` en las rutas de react-router queda para la Etapa 3 |

---

## 7. Errores

Formato Problem Details, con `Content-Type: application/problem+json`. El campo `type` no se envía cuando vale `about:blank` (así lo hace Spring 7); según RFC 9457, si falta, vale `about:blank`. El front no debe depender de `type`:

```json
{
  "title": "No autorizado",
  "status": 401,
  "detail": "Usuario o contraseña incorrectos. Intentá nuevamente.",
  "instance": "/api/auth/login"
}
```

Para errores de validación (`400`) se agrega una lista:

```json
{
  "title": "Datos inválidos",
  "status": 400,
  "detail": "Revisá los datos ingresados.",
  "instance": "/api/auth/login",
  "errores": [
    { "campo": "nombreUsuario", "mensaje": "Ingresá tu nombre de usuario." }
  ]
}
```

| Status | Cuándo | `detail` |
|---|---|---|
| 400 | Validación | "Revisá los datos ingresados." |
| 401 | Login inválido | "Usuario o contraseña incorrectos. Intentá nuevamente." (texto exacto de CU-12 y CU-14) |
| 401 | Token faltante, inválido o vencido | "Tu sesión terminó. Ingresá de nuevo." |
| 403 | Rol sin permiso | "No tenés permiso para ver esta pantalla." |
| 404 | No existe | "No encontramos lo que buscás." |
| 500 | Error interno | "Algo salió mal. Probá de nuevo en un rato." |

**Importante:** los `401` y `403` los generan los filtros de Spring Security, no los controladores. Hay que implementar un `AuthenticationEntryPoint` y un `AccessDeniedHandler` que devuelvan este mismo formato. Si no, el front recibe respuestas sin cuerpo. Los `500` nunca muestran el detalle técnico ni el stack trace.

---

## 8. Back: qué implementar

Paquete base: `ar.edu.utn.frm.gelab.alma`

- `auth/`: `domain/Cuenta`, `domain/Rol`, `repository/CuentaRepository`, `service/AuthService`, `service/JwtService`, `controller/AuthController`, `dto/` (`LoginRequest`, `LoginResponse`, `UsuarioResponse`).
- `config/`: ya existen `SecurityConfig` y `OpenApiConfig`. Falta `JwtConfig` (beans `JwtEncoder` y `JwtDecoder`).
- `common/`: manejo global de errores (`@RestControllerAdvice`), entry point y access denied handler.
- `persona/`: `PersonaMayor`, `PreferenciaAccesibilidad`, `TamanioLetra` y el controlador de accesibilidad.
- `administrador/`: `Administrador`.

**Entidades** (según el DC; no se agregan campos que el DC no tiene, y si se necesitan, primero se actualiza el DC):
- `Cuenta`: `id`, `nombreUsuario` (único), `contrasenia` (guardada como hash BCrypt), `rol`, `fechaAlta`.
- `PersonaMayor`: `id`, `nombre`, `apellido`, `fechaNacimiento`; 1 a 1 con `Cuenta`; contiene una `PreferenciaAccesibilidad` (1 a 1).
- `Administrador`: `id`, `nombre`, `apellido`; 1 a 1 con `Cuenta`.
- `PreferenciaAccesibilidad`: `tamanioLetra` (`TamanioLetra`), `altoContraste` (`Boolean`).

**Checklist**

1. **Hecho:** `springdoc-openapi-starter-webmvc-ui` 3.1.1 ya está en el `pom.xml` y la app arranca con Swagger.
2. Activar `spring.mvc.problemdetails.enabled=true` (todavía no está en `application.properties`).
3. `SecurityConfig`: ya tiene sesión stateless, CSRF desactivado, y Swagger y `/actuator/health` públicos. **Falta:** desactivar `httpBasic` y `formLogin`, activar `oauth2ResourceServer` con JWT, permitir `POST /api/auth/login`, exigir `ADMINISTRADOR` en `/api/admin/**`, y dejar públicas las rutas que no empiezan con `/api`. Hoy `anyRequest().authenticated()` bloquearía el front cuando lo sirva Spring.
4. `JwtEncoder` y `JwtDecoder` con la clave HMAC de `alma.security.jwt.secret`. Si la clave tiene menos de 32 bytes, la app debe **fallar al arrancar** con un mensaje claro.
5. Carga de usuarios de prueba (sección 10), **idempotente**: solo crea la cuenta si el nombre de usuario no existe. Para cada persona mayor crea también su `PersonaMayor` y su `PreferenciaAccesibilidad`; para el administrador, su `Administrador`. Controlada por `alma.seed.enabled`.
6. **Hecho:** `application.properties` ya no menciona Flyway; el esquema lo crea Hibernate con `ddl-auto=update`.
7. **Hecho a medias:** `OpenApiConfig` ya declara el esquema `bearerAuth` y lo aplica a todos los endpoints. Falta marcar `POST /api/auth/login` como endpoint sin seguridad (`@SecurityRequirements` vacío); si no, Swagger muestra el candado y pide un token para poder loguearse.
8. Pruebas de la sección 11.

---

## 9. Front: qué implementar

Estructura sugerida:

```
src/
  lib/api.ts                 cliente fetch
  features/auth/
    api.ts                   login(), me()
    schemas.ts               esquema zod del formulario
    AuthProvider.tsx         estado de sesión
    useAuth.ts
    LoginPage.tsx
    ProtectedRoute.tsx
```

- **`lib/api.ts`:** base `/api`. Agrega `Authorization: Bearer` si hay token. Convierte las respuestas de error en un `ApiError` con `status`, `detail` y `errores`. Ante un `401` (salvo en `/auth/login`), borra el token y manda a `/login` con el aviso "Tu sesión terminó. Ingresá de nuevo."
- **`AuthProvider`:** guarda el token con la clave `alma.token`, en `localStorage` si el usuario dejó tildado "Mantener sesión iniciada" y en `sessionStorage` si no. Al iniciar la app busca el token en ambos lugares; si hay, llama a `/api/auth/me` y, si falla, lo borra. Expone `usuario`, `cargando`, `login()` y `logout()`.
- **Cerrar sesión:** el botón pide confirmación ("¿Querés cerrar sesión?") antes de llamar a `logout()`.
- **`LoginPage`:** `react-hook-form` + `zod`. Campos: usuario, contraseña y casilla "Mantener sesión iniciada" (tildada de entrada). Después de ingresar redirige según el rol: `PERSONA_MAYOR` a `/`, `ADMINISTRADOR` a `/admin`. En error 401 muestra el mensaje genérico, sin indicar cuál campo falló.
- **`ProtectedRoute`:** sin sesión manda a `/login`; con rol no permitido muestra una pantalla "No tenés permiso para ver esta pantalla."
- **Preferencias de accesibilidad:** al recibir `preferenciasAccesibilidad` (en el login o en `/me`), aplicarlas con clases o atributos en `<html>`: tamaño de letra (`NORMAL`, `GRANDE`, `MUY_GRANDE`) y alto contraste. Cuando la persona las cambia, se envía `PUT /api/accesibilidad`. Se diseñan sobre las variables de color y el tamaño base de letra de Tailwind, para que el cambio alcance a toda la app.

**Accesibilidad del formulario** (el público son personas mayores):
- Etiquetas siempre visibles, no solo placeholders.
- Botón grande y con texto claro ("Ingresar").
- Botón para mostrar u ocultar la contraseña.
- Atributos `autocomplete` correctos (`username` y `current-password`).
- Los errores se muestran junto al campo y en un aviso arriba, en lenguaje simple.
- Todo se puede usar con teclado: Tab para moverse y Enter para enviar.

---

## 10. Usuarios de prueba

Solo para desarrollo. Los crea el back al arrancar si `alma.seed.enabled=true` (activarlo solo en local, nunca en un despliegue real).

| Usuario | Nombre y apellido | Rol | Contraseña |
|---|---|---|---|
| `ana` | Ana Pérez | `PERSONA_MAYOR` | `Alma2026!` |
| `carlos` | Carlos Gómez | `PERSONA_MAYOR` | `Alma2026!` |
| `admin` | Admin Alma | `ADMINISTRADOR` | `Alma2026!` |

Las personas mayores de prueba necesitan además `fechaNacimiento` (por ejemplo `1950-05-12` y `1948-11-03`).

---

## 11. Pruebas y criterio de "listo"

**Back (automáticas o con Swagger/curl)**
- Login correcto: `200` con token y usuario.
- Contraseña incorrecta: `401`.
- Usuario inexistente: `401` con el **mismo** mensaje que la contraseña incorrecta.
- `GET /api/auth/me` sin token: `401`.
- Token de persona mayor contra `/api/admin/**`: `403`.
- `GET /actuator/health` sin token: `200`.
- Token vencido: `401`.
- Con `mantenerSesion: true` el token vence a los 30 días; con `false`, al día.

**Front (manuales)**
- Cada usuario de prueba entra y llega a la pantalla de su rol.
- Con "Mantener sesión iniciada" tildada, al recargar (F5) o cerrar y abrir el navegador la sesión sigue. Destildada, sigue al recargar pero se pierde al cerrar la pestaña.
- Si se borra el token del navegador, la app vuelve a `/login`.
- Una persona mayor que escribe `/admin` en la barra ve "No tenés permiso".
- Cerrar sesión pide confirmación.
- Se puede completar el login solo con el teclado.

**Listo cuando:** con `docker compose up -d`, el back corriendo y `pnpm dev`, los tres usuarios de prueba pueden entrar desde el front y ven la pantalla que les corresponde.

---

## 12. Pendientes para decidir

1. **Token en `localStorage`:** aceptable para el MVP. Si el proyecto crece, evaluar una cookie `HttpOnly`, que exige además proteger contra CSRF.

*(Resueltos: "operador" y "administrador" son el mismo rol, `ADMINISTRADOR`; las preferencias de accesibilidad son `tamanioLetra` y `altoContraste`, según el DC; `nombre` y `apellido` viven en `PersonaMayor` y `Administrador`.)*

---

## 13. Cómo usar este documento con Claude Code

**Back**
> Leé `CLAUDE.md` y `docs/contrato-login.md`. Mostrame primero el plan para implementar la parte del back según la sección 8, y no escribas código hasta que lo apruebe.

**Front**
> Leé `CLAUDE.md` y `docs/contrato-login.md`. Mostrame primero el plan para implementar la parte del front según la sección 9, y no escribas código hasta que lo apruebe.

Si Claude Code propone algo que contradice el contrato, gana el contrato. Si hay que cambiarlo, se edita este archivo.

---

## 14. Historial de cambios

- **v1 (23/10/2026):** primera propuesta.
- **v2 (23/10/2026, contra el commit `b47fe79`):** se marcó lo que ya está hecho (springdoc, `OpenApiConfig`, parte de `SecurityConfig`), se agregó lo que falta en `SecurityConfig` y en Swagger, se aclaró que la zona horaria es la de Jackson, y se avisó que el `CLAUDE.md` debe estar en la raíz.
- **v5 (23/10/2026):** sección 7: el campo `type` no viaja cuando vale `about:blank` (comportamiento de Spring 7, equivalente según RFC 9457); se quitó de los ejemplos.
- **v4 (23/10/2026):** se ajustó al diagrama de clases (DC_PPS_3): `nombre` y `apellido` salen de `PersonaMayor` y `Administrador`; las preferencias de accesibilidad quedan en `tamanioLetra` y `altoContraste` (se quitó `reducirMovimiento`), solo para `PERSONA_MAYOR`, y viajan en el login y en `/me`; se agregaron las entidades y el seed correspondiente.
- **v3 (23/10/2026):** se alineó con la documentación (CU-12, CU-14, DC): credenciales por nombre de usuario y contraseña en lugar de email; mensaje de error exacto de los CU; "Mantener sesión iniciada" (token en `localStorage` o `sessionStorage` y expiración de 30 días); confirmación al cerrar sesión; `Cuenta` sin campos fuera del DC; seed por nombre de usuario; se resolvió que operador y administrador son el mismo rol.
