# Direcciones del usuario

Endpoints privados (requieren la cookie de sesión) para gestionar las direcciones de envío y
facturación del usuario autenticado. Cada usuario solo ve y modifica las suyas: una dirección
ajena responde 404.

| Método y ruta | Cuerpo | Respuesta |
| --- | --- | --- |
| `GET /api/account/addresses` | — | 200 `AddressDto[]`, por id ascendente |
| `POST /api/account/addresses` | `AddressRequest` | 201 `AddressDto` |
| `PUT /api/account/addresses/{id}` | `AddressRequest` | 200 `AddressDto` |
| `DELETE /api/account/addresses/{id}` | — | 200 |

`AddressRequest`: `alias?`, `nombreDestinatario`, `apellidosDestinatario`, `telefono?`
(números, espacios y `+ ( ) - .`), `calle`, `numero`, `complemento?`, `codigoPostal`,
`localidad`, `provincia`, `pais`, `usoEnvio`, `usoFacturacion`, `predeterminadaEnvio`,
`predeterminadaFacturacion`. `AddressDto` añade `id`. Debe marcarse al menos un uso
(error de validación en el campo `uso`).

## Predeterminadas

- Hay como máximo una dirección predeterminada de envío y otra de facturación por usuario
  (lo garantiza también la base de datos con columnas generadas).
- La primera dirección de cada uso pasa a ser la predeterminada aunque no se pida.
- Marcar otra como predeterminada desmarca la anterior.
- Al borrar o desmarcar la predeterminada, pasa a serlo la más antigua de las demás de ese uso.
- Una dirección no puede ser predeterminada de un uso que no tiene marcado.

Límite: 20 direcciones por usuario (409 al superarlo).
