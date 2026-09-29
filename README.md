# Sherry Cards Shop API

API REST en Java 21 y Spring Boot 3, organizada como monolito modular. La conexión MySQL se configura mediante variables de entorno y el esquema queda bajo control exclusivo de Flyway.

## Requisitos

- JDK 21
- Maven 3.9+
- MySQL 8

## Ejecutar en local

1. Crea una base de datos vacía en MySQL. No es necesario crear tablas.
2. Copia `.env.example` a `.env` y completa la URL, usuario y contraseña de tu MySQL local. `.env` está excluido de Git.
3. Exporta las variables en tu terminal y arranca la API:

```sh
set -a
source .env
set +a
mvn spring-boot:run
```

Spring Boot no carga archivos `.env` automáticamente; los comandos anteriores exportan sus valores al proceso. También puedes configurar las mismas variables directamente en la terminal o en tu IDE.

Para ejecutar las pruebas, que usan H2 en memoria sin Flyway:

```sh
mvn test
```

## Variables de entorno

Obligatorias para los perfiles `dev` y `prod`:

| Variable | Descripción |
| --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC URL, por ejemplo `jdbc:mysql://host:3306/sherry_cards_shop` |
| `SPRING_DATASOURCE_USERNAME` | Usuario MySQL |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña MySQL |

Opcionales:

| Variable | Uso | Valor por defecto |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | Perfil Spring (`dev`, `test` o `prod`) | `dev` |
| `SERVER_PORT` | Puerto HTTP local; Railway usa su variable `PORT` automáticamente | `8080` |
| `CORS_ALLOWED_ORIGINS` | Orígenes Angular separados por comas | `http://localhost:4200` |

`spring.jpa.hibernate.ddl-auto` permanece en `validate`; Hibernate no crea ni modifica tablas. Flyway está activo para MySQL, valida las migraciones y tiene deshabilitada la operación `clean`. La migración `V1__create_categories.sql` crea la tabla `categories` e inserta las seis categorías iniciales. En una base vacía Flyway crea su tabla interna `flyway_schema_history` y aplica esa migración.

## Comprobar el arranque y la conexión

Con MySQL accesible y las tres variables obligatorias definidas, ejecuta `mvn spring-boot:run` y espera el mensaje `Started SherryCardsShopApiApplication`. Después consulta:

```sh
curl -i http://localhost:8080/actuator/health
curl -i http://localhost:8080/api/health
```

Actuator debe responder HTTP 200 con estado `UP` cuando la aplicación y la conexión de base de datos estén disponibles. `/api/health` es el endpoint de estado de la aplicación; para comprobar específicamente el acceso a MySQL, usa Actuator. Los detalles internos del health check no se exponen.

## Despliegue posterior en Railway

1. Añade un servicio MySQL al proyecto Railway.
2. En el servicio de la API, establece `SPRING_PROFILES_ACTIVE=prod`. Railway asigna `PORT` automáticamente y la aplicación lo prioriza sobre `SERVER_PORT`.
3. Configura `SPRING_DATASOURCE_URL` con la JDBC URL accesible desde la API, y asigna `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` a las credenciales del servicio MySQL. Usa variables de referencia de Railway para leer los valores del servicio de base de datos; no copies credenciales al repositorio.
4. Despliega la API y revisa los logs de inicio. Confirma `UP` consultando `/actuator/health` en el dominio público asignado por Railway.

Variables que debe tener el servicio de la API en Railway:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQLUSER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQLPASSWORD}}
```

En estas referencias, `MySQL` debe coincidir exactamente con el nombre del servicio de base de datos en Railway. Si Railway genera otro nombre, actualiza ese prefijo. El host y puerto deben ser los accesibles desde el servicio de API; no uses una URL `mysql://` sin el prefijo JDBC. Mantén usuario y contraseña como referencias al servicio MySQL, sin copiarlos al repositorio.

## Endpoints disponibles

- `GET /api/health`: estado de la aplicación.
- `GET /api/categories`: categorías activas ordenadas por prioridad.
- `GET /actuator/health`: salud de la aplicación y sus componentes.
- `GET /v3/api-docs` y `/swagger-ui.html`: documentación OpenAPI.

La consulta de categorías es de solo lectura y responde con DTOs dentro del sobre JSON `ApiResponse`; no hay CRUD ni paginación.
# Sherry Cards Shop API

API REST en Java 21 y Spring Boot 3, organizada como monolito modular para consumo JSON desde Angular.

## Requisitos

- JDK 21
- Maven 3.9+
- MySQL 8 para los perfiles `dev` y `prod`

## Configuración local

El perfil `dev` se activa por defecto. Define `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en el entorno antes de arrancar. Flyway aplica las migraciones ubicadas en `src/main/resources/db/migration`; Hibernate valida el esquema y nunca crea tablas.

```sh
export DB_URL='jdbc:mysql://localhost:3306/sherry_cards_shop'
export DB_USERNAME='<usuario>'
export DB_PASSWORD='<contraseña>'
mvn spring-boot:run
```

Las pruebas usan el perfil `test` y una base H2 en modo de compatibilidad MySQL:

```sh
mvn test
```

## Endpoints actuales

- `GET /api/health`: estado de la aplicación.
- `GET /v3/api-docs`: especificación OpenAPI en JSON.
- `/swagger-ui.html`: documentación interactiva.
- `/actuator/health`: health check de Spring Boot Actuator.

Los módulos de ecommerce solo están reservados como estructura; no se han implementado productos ni procesos de negocio. Spring Security está preparado, pero permite las solicitudes sin autenticación mientras no se defina el mecanismo de acceso.

## Perfiles

- `dev`: MySQL y logging de desarrollo.
- `test`: H2 aislado para las pruebas.
- `prod`: MySQL y logging de producción.
- `CORS_ALLOWED_ORIGINS`: orígenes Angular permitidos separados por comas (por defecto `http://localhost:4200`).
- `SERVER_PORT`: puerto HTTP (por defecto `8080`).

## Docker

```sh
docker build -t sherry-cards-shop-api .
docker run --rm -p 8080:8080 \
  -e DB_URL='jdbc:mysql://host.docker.internal:3306/sherry_cards_shop' \
  -e DB_USERNAME='<usuario>' \
  -e DB_PASSWORD='<contraseña>' \
  sherry-cards-shop-api
```

No se incluyen credenciales. La base de datos debe existir antes de iniciar; Flyway administra sus tablas mediante migraciones.