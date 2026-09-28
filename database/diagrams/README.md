# Modelos de datos

Los archivos de esta carpeta son representaciones visuales de referencia. El esquema desplegado se determina por las migraciones Flyway en `src/main/resources/db/migration`, no por los diagramas.

## Mantener el modelo actualizado

1. Cuando una migración Flyway cambie el esquema, actualiza el modelo lógico y físico correspondiente en `.drawio` y/o `.mwb`.
2. Refleja nombres, tipos, nulabilidad, claves, índices y relaciones tal como quedan después de aplicar todas las migraciones.
3. Conserva nombres y convenciones descritos en `../mysql/docs/`.
4. Revisa el diagrama junto con la migración en el mismo cambio de código y valida que no documente objetos que no existan en el esquema final.
5. No uses la exportación del diagrama para modificar directamente bases de datos compartidas. Implementa esos cambios en una nueva migración Flyway.

`sherry-cards-shop.drawio` está reservado para diagrams.net / draw.io y `sherry-cards-shop.mwb` para MySQL Workbench. Los archivos están vacíos como plantillas iniciales.