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