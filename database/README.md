# Database resources

Esta carpeta reúne documentación, modelos y herramientas auxiliares para administrar y mantener la base de datos del proyecto. No es el origen de los cambios de esquema de la aplicación.

## Fuente oficial del esquema

- Las migraciones oficiales se mantienen exclusivamente en `src/main/resources/db/migration` y se ejecutan con Flyway.
- No apliques cambios de esquema de la aplicación desde scripts guardados aquí. Los cambios versionados deben incorporarse como nuevas migraciones Flyway.
- `mysql/scripts` contiene scripts auxiliares de administración, inspección y diagnóstico. Revisa cada script y selecciona explícitamente el entorno antes de ejecutarlo.

## Carpetas

- `diagrams/`: modelos de datos y diagramas conceptuales/lógicos/físicos del proyecto.
- `mysql/schema/`: referencias y exportaciones de esquema para consulta; no sustituyen las migraciones Flyway.
- `mysql/data/`: datos de referencia o muestras para documentación y trabajo local; no sustituye seeds oficiales en migraciones.
- `mysql/views/`: documentación y borradores de vistas MySQL.
- `mysql/procedures/`: documentación y borradores de procedimientos almacenados.
- `mysql/functions/`: documentación y borradores de funciones almacenadas.
- `mysql/triggers/`: documentación y borradores de triggers.
- `mysql/scripts/`: scripts auxiliares para creación/administración, comprobaciones y análisis manuales.
- `mysql/backups/`: copias de seguridad manuales. No guardar aquí secretos ni copias con datos personales sin autorización y protección adecuada.
- `mysql/docs/`: documentación técnica, estándares y convenciones de la base de datos.

Consulta `diagrams/README.md` para actualizar los modelos y `mysql/docs/` para las convenciones del proyecto.