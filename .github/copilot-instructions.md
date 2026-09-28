# Instrucciones del proyecto

- Mantener la arquitectura de monolito modular por funcionalidades bajo `com.sherrycardsshop.api`.
- Mantener cada módulo enfocado y evitar dependencias circulares entre features.
- Gestionar el esquema exclusivamente con migraciones Flyway y conservar `ddl-auto: validate`.
- Devolver JSON desde la API y manejar errores mediante `GlobalExceptionHandler`.
- No añadir vistas HTML, Thymeleaf, JSP, login, JWT ni funciones de ecommerce sin solicitud explícita.
- Mantener la configuración en `application.yml` y obtener secretos mediante variables de entorno.