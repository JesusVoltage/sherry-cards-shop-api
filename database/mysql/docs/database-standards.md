# Estándares de base de datos

## Propiedad del esquema

- Flyway es la única fuente oficial de cambios de esquema de la aplicación. Sus migraciones están en `src/main/resources/db/migration`.
- No aplicar DDL de aplicación manualmente en entornos compartidos ni duplicar migraciones oficiales en `database/mysql/`.
- Mantener Hibernate en `ddl-auto: validate`; no depender de generación automática de tablas.
- Probar cada migración en una base limpia y, cuando corresponda, en una base con la versión anterior aplicada.

## Tipos de datos

- Usar `BIGINT` para identificadores de entidades cuando corresponda y `AUTO_INCREMENT` solo para claves generadas localmente por MySQL.
- Dimensionar `VARCHAR` de acuerdo con el dominio; usar `TEXT` solo cuando el contenido no tenga un límite razonable.
- Usar `DECIMAL(precision, scale)` para importes y cantidades exactas; no usar `FLOAT` ni `DOUBLE` para dinero.
- Usar `BOOLEAN` para valores binarios y definir explícitamente nulabilidad y valor por defecto.
- Usar tipos temporales con precisión apropiada. Guardar y procesar fechas de forma consistente en UTC; documentar cualquier excepción.
- Elegir tipos compatibles con el uso, índices y serialización de la API, y reflejar su nulabilidad en JPA.

## Claves primarias y restricciones

- Toda tabla de entidad debe tener una clave primaria estable, normalmente `id BIGINT AUTO_INCREMENT`.
- Declarar `NOT NULL`, `UNIQUE` y `CHECK` cuando formen parte de las reglas de integridad de datos.
- Añadir claves foráneas cuando la relación deba garantizarse en la base de datos. Elegir explícitamente las acciones `ON DELETE` y `ON UPDATE`; evitar cascadas no justificadas.
- Indexar columnas usadas en filtros, joins y ordenamientos frecuentes. Evitar índices duplicados y revisar el coste de escritura.

## Auditoría

- Las tablas mutables deben considerar `created_at` y `updated_at`, con tipo y precisión coherentes.
- `created_at` se establece al crear el registro y no se modifica después.
- `updated_at` se actualiza en cada modificación persistida.
- Usar UTC de forma consistente. Añadir columnas de actor o borrado lógico solo cuando exista un requisito funcional aprobado.

## Migraciones Flyway

- Crear migraciones versionadas nuevas; no editar migraciones que ya se hayan aplicado en un entorno compartido.
- Usar nombres `V<version>__<description>.sql`, orden secuencial y una descripción breve en `snake_case`.
- Mantener cada migración enfocada y revisable. Incluir datos iniciales o de referencia solo cuando sean parte del estado requerido de la aplicación.
- No incluir credenciales, datos personales reales ni operaciones destructivas sin una estrategia de recuperación aprobada.
- Los scripts de `database/mysql/scripts/` son auxiliares/manuales y nunca reemplazan una migración oficial.