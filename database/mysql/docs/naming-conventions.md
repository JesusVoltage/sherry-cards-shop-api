# Convenciones de nombres

Estas convenciones se aplican a los objetos MySQL nuevos. Los cambios de esquema se implementan mediante migraciones Flyway.

## Formato general

- Usar inglés, `snake_case` y nombres descriptivos en minúsculas.
- Evitar espacios, guiones, abreviaturas ambiguas y palabras reservadas de MySQL.
- Mantener nombres estables; renombrar objetos requiere una migración explícita.

## Objetos

- Tablas: nombres descriptivos en plural, por ejemplo `categories`.
- Columnas: nombres descriptivos, por ejemplo `display_order` e `image_url`.
- Claves primarias: `id`.
- Claves foráneas: `<referenced_table_singular>_id`, por ejemplo `category_id`.
- Restricciones únicas: `uk_<table>_<columnas>`, por ejemplo `uk_categories_slug`.
- Restricciones de clave foránea: `fk_<table>_<referenced_table>`, usando un sufijo si hay más de una relación entre las mismas tablas.
- Índices: `idx_<table>_<columnas>`, por ejemplo `idx_categories_active_display_order`.
- Migraciones Flyway: `V<version>__<short_description>.sql`, con doble guion bajo entre versión y descripción.

Los identificadores de restricciones e índices deben ser únicos dentro del esquema y suficientemente cortos para los límites de MySQL.