# Consulta de novedades

`GET /api/novedades` devuelve las novedades activas ordenadas por `displayOrder`
ascendente. No recibe parámetros ni cuerpo. Sigue la configuración de acceso
público actual de la API.

Respuesta HTTP 200:

```json
{
  "success": true,
  "message": "Novedades activas obtenidas correctamente",
  "data": [
    {
      "id": 1,
      "title": "EB-05 de One Piece",
      "slug": "eb-05-one-piece",
      "description": "Novedad del set EB-05 de One Piece.",
      "imageUrl": null,
      "categoryName": "One Piece",
      "categorySlug": "one-piece",
      "displayOrder": 1
    }
  ],
  "timestamp": "2026-09-29T12:00:00Z"
}
```

Si no hay novedades activas, responde HTTP 200 con `data: []`.
`description` e `imageUrl` pueden ser nulos. Cada novedad está vinculada a una
categoría. El filtro utiliza el estado de la novedad; no filtra por el estado de
la categoría. No se garantiza un orden entre novedades con el mismo
`displayOrder`.

El módulo mantiene las capas `controller`, `service`, `repository`, `entity`,
`dto` y `mapper`. El servicio consulta dentro de una transacción de solo lectura
y el repositorio carga las categorías con las novedades para evitar consultas
adicionales durante el mapeo. Las entidades no se exponen en la respuesta.

La tabla `novedades` y sus datos iniciales se definen en la migración existente
`V2__create_novedades.sql`. Este endpoint no incluye altas, modificaciones ni borrado.
