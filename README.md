# Sherry Cards Shop API

API REST en Java 21 y Spring Boot 3. La configuración de base de datos está separada por perfil; Flyway administra el esquema y Hibernate solo lo valida.

## Requisitos

- JDK 21
- Maven 3.9+
- MySQL 8 para los perfiles `dev` y `prod`

## Desarrollo local

El perfil predeterminado es `dev`. Crea una base MySQL local y configura credenciales en el entorno o en un `.env` local (Spring Boot no carga `.env` por sí solo):

```sh
export SPRING_PROFILES_ACTIVE=dev
export SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/sherry_cards_shop'
export SPRING_DATASOURCE_USERNAME='<usuario-local>'
export SPRING_DATASOURCE_PASSWORD='<contraseña-local>'
mvn spring-boot:run
```

También se aceptan `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` como alternativas locales. La URL debe comenzar por `jdbc:mysql://`.

## Railway

Configura `SPRING_PROFILES_ACTIVE=prod` en el servicio de la API. Para compartir la conexión del servicio MySQL, crea estas variables de referencia en Railway, reemplazando `MySQL` por el nombre exacto del servicio:

```text
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
```

El perfil `prod` también puede consumir directamente `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER` y `MYSQLPASSWORD` si Railway las expone al servicio API; en ese caso construye `jdbc:mysql://...` a partir del host, puerto y base. Las variables `SPRING_DATASOURCE_*` tienen prioridad cuando existen. No uses `MYSQL_URL` como datasource URL: normalmente empieza por `mysql://`, que no es una JDBC URL.

Railway asigna `PORT` automáticamente y la aplicación lo prioriza sobre `SERVER_PORT`.

## Perfiles y esquema

- `dev`: MySQL local; datasource por `SPRING_DATASOURCE_*` o, alternativamente, `DB_*`. URL local predeterminada: `jdbc:mysql://localhost:3306/sherry_cards_shop`.
- `prod`: datasource exclusivamente desde variables de entorno (`SPRING_DATASOURCE_*` o variables MySQL de Railway). No hay usuario ni contraseña por defecto.
- `test`: H2 en memoria en modo MySQL; Flyway aplica la misma migración para que Hibernate pueda validar el esquema.

Flyway en `dev` y `prod` usa automáticamente el mismo datasource configurado por Spring Boot. `spring.jpa.hibernate.ddl-auto` es `validate`; Hibernate no crea ni modifica tablas. Flyway tiene `clean` deshabilitado.

## Ejecución y comprobación

```sh
mvn spring-boot:run
mvn test
```

Endpoints de salud: `GET /api/health` y `GET /actuator/health`.
