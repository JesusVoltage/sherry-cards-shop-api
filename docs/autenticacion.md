# Autenticación

Login clásico (email y contraseña) y login con Google. La sesión vive en dos cookies
HttpOnly; el frontend nunca ve ni almacena tokens.

| Cookie | Contenido | Path | Duración |
| --- | --- | --- | --- |
| `scs_access` | JWT HS256 (`sub` = id de usuario, `role`) | `/` | 15 min |
| `scs_refresh` | Token opaco aleatorio; en BD solo su SHA-256 | `/api/auth` | 30 días |

Las cookies son `HttpOnly`, `Secure` (salvo en `dev`) y `SameSite` según
`APP_AUTH_COOKIE_SAME_SITE`. Todas las respuestas usan el sobre `ApiResponse`.

## Endpoints

| Método y ruta | Cuerpo | Respuesta |
| --- | --- | --- |
| `POST /api/auth/register` | `username`, `email`, `nombre`, `apellidos?`, `password` | 201 `UserDto`. No inicia sesión |
| `POST /api/auth/login` | `email`, `password` | 200 `UserDto` + cookies |
| `POST /api/auth/google` | `credential` (ID token de Google Identity Services) | 200 `UserDto` + cookies |
| `POST /api/auth/refresh` | — (cookie `scs_refresh`) | 200 `UserDto` + cookies nuevas |
| `POST /api/auth/logout` | — | 200 y cookies borradas. Idempotente |
| `GET /api/auth/me` | — (cookie `scs_access`) | 200 `UserDto` |

`UserDto`: `id`, `username`, `email`, `nombre`, `apellidos`, `role` (`CLIENTE`/`ADMIN`),
`status` (`ACTIVO`/`PENDIENTE`/`BLOQUEADO`), `emailVerifiedAt`, `lastAccessAt`.

Errores: 400 validación (`data` con los campos), 401 credenciales o sesión no válidas,
403 cuenta bloqueada/pendiente u origen no permitido, 409 `"El username ya existe"` /
`"El email ya está registrado"`, 429 tras 5 intentos fallidos en 15 min por email,
503 si Google no está configurado.

## Reglas

- Registro: username 3–30 `[A-Za-z0-9_-]` único sin distinguir mayúsculas; email
  normalizado a minúsculas; contraseña 8–72 caracteres y como máximo 72 bytes (BCrypt).
  Los usuarios nuevos son `CLIENTE` y `ACTIVO`.
- Refresh rotatorio: cada `/refresh` revoca el token usado. Si llega un token ya rotado
  hace más de 30 s se asume robo y se revocan todas las sesiones del usuario.
- Google: el backend valida firma, emisor, audiencia (`GOOGLE_CLIENT_ID`) y caducidad del
  ID token y exige `email_verified`. Busca por `google_sub`; si no existe, vincula la cuenta
  con el mismo email o crea una nueva (username derivado del email). Al vincular una cuenta
  cuyo email no estaba verificado se elimina su contraseña y se revocan sus sesiones, para
  evitar que quien la registró con un email ajeno conserve el acceso.
- CSRF: las peticiones `POST/PUT/PATCH/DELETE` con un `Origin` que no sea la propia API ni
  esté en `CORS_ALLOWED_ORIGINS` reciben 403.
- Autorización: todo es privado por defecto; las rutas públicas están en
  `SecurityConfig.PUBLIC_REQUESTS` y `/api/admin/**` exige rol `ADMIN`.
- El límite de intentos se guarda en memoria: se reinicia con cada despliegue y no se
  comparte entre réplicas.

## Datos de la cuenta y contraseña

| Método y ruta | Cuerpo | Respuesta |
| --- | --- | --- |
| `PUT /api/account/profile` | `username`, `nombre`, `apellidos?` | 200 `UserDto`; 409 con `data.username` si está en uso |
| `PUT /api/account/password` | `currentPassword` (salvo cuentas solo de Google), `newPassword` | 200 `UserDto` y cookies nuevas |

- El correo no se puede cambiar: requerirá verificar la nueva dirección por email.
- Cambiar la contraseña exige la actual (400 con `data.currentPassword` si es incorrecta;
  429 tras 5 fallos en 15 min), revoca todos los refresh tokens del usuario y emite una sesión
  nueva para el dispositivo actual. Los access tokens de otros dispositivos caducan en 15 min.
- `UserDto` incluye `hasPassword` y `googleLinked`.

### Política de contraseñas (registro y cambio)

Según NIST SP 800-63B: mínimo 8 caracteres y máximo 72 bytes (BCrypt), sin reglas de
composición, sin el username ni la parte local del correo (si tienen 4 o más caracteres) y
sin contraseñas filtradas. Esto último consulta la API de rangos de Pwned Passwords enviando
solo los 5 primeros caracteres del SHA-1; si no responde en 2-3 s, la comprobación se omite.
Los errores de política responden 400 con el campo en `data`.

## Promocionar un administrador

```sql
UPDATE usuarios SET rol_id = (SELECT id FROM roles WHERE code = 'ADMIN') WHERE email = 'tu@email.com';
```

El nuevo rol se aplica en el siguiente login o refresh (como máximo 15 minutos).
