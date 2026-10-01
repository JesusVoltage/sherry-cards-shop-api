# Instrucciones del proyecto

- Mantener la arquitectura de monolito modular por funcionalidades bajo `com.sherrycardsshop.api`.
- Mantener cada módulo enfocado y evitar dependencias circulares entre features.
- Gestionar el esquema exclusivamente con migraciones Flyway y conservar `ddl-auto: validate`.
- Devolver JSON desde la API y manejar errores mediante `GlobalExceptionHandler`.
- No añadir vistas HTML, Thymeleaf, JSP ni funciones de ecommerce sin solicitud explícita.
- La autenticación vive en el módulo `auth` y en `security`: cookies HttpOnly con access JWT y refresh token rotatorio. Las rutas son privadas por defecto; las públicas se declaran en `SecurityConfig.PUBLIC_REQUESTS`.
- Mantener la configuración en `application.yml` y obtener secretos mediante variables de entorno.