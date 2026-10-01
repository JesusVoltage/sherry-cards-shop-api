# Catálogo, carrito y pedidos

Esquema creado en `V4__create_catalog_cart_and_orders.sql`. Importes en EUR con IVA incluido
(venta B2C en España) y fechas en UTC.

## Modelo

```text
categories ─< products ─< product_variants ─< product_variant_attributes
                  │              │
                  └─< product_images
usuarios ── carts ─< cart_items >── product_variants
usuarios ─< orders ─< order_items        (copia: sku, nombre, precio, IVA)
               ├─< order_addresses       (copia: SHIPPING / BILLING)
               └─< order_status_history  (estado, nota, quién)
product_types, product_statuses, order_statuses: tablas de referencia
```

- **Producto y variantes.** `products` es la ficha (categoría, tipo, estado, slug, fecha de
  lanzamiento para preventas). Lo que se vende es la variante: SKU, precio, precio anterior
  (`compare_at_price` > `price`), IVA, stock, peso. Sus características son libres
  (`product_variant_attributes`: "Idioma: Español", "Edición: Promo", "Color: Azul").
- **Estados de producto:** `DRAFT`, `ACTIVE`, `ARCHIVED`. Un producto con pedidos se archiva,
  no se borra.
- **Carrito:** uno por usuario, sin precios guardados; cantidad entre 1 y 99 por variante.
- **Pedido:** `order_number` legible, totales (`subtotal`, `shipping_total`, `discount_total`,
  `tax_total`, `total`), fechas de cada hito y `stock_reserved_until`. Las líneas y direcciones
  son copias para que el pedido no cambie si se edita el catálogo o la libreta de direcciones.
- **Estados de pedido:** `PENDING_PAYMENT`, `PAID`, `PROCESSING`, `SHIPPED`, `DELIVERED`,
  `CANCELLED`, `REFUNDED` (los tres últimos son finales). Cada cambio se registra con
  `CustomerOrder.changeStatus`.

## Categorías anidadas

`categories.parent_id` (V5) permite subcategorías de cualquier profundidad, por ejemplo
Pokémon › Expansiones › Escarlata y Púrpura. Las raíces tienen `parent_id` nulo y no se puede
borrar una categoría con hijas. Un producto puede colgar de cualquier nivel.

- `GET /api/categories`: categorías raíz activas (lo que muestra la home).
- `GET /api/categories/tree`: árbol de categorías activas con `children`, para menús.
  Una rama cuyo padre está inactivo se oculta entera.
- `CategoryService.getSelfAndDescendantIds(id)`: la categoría y todas sus subcategorías
  visibles, para listar productos de una categoría incluyendo los de sus hijas.

MySQL no admite `CHECK` sobre columnas `AUTO_INCREMENT`, así que evitar que una categoría sea
su propio padre o forme ciclos corresponde a la aplicación (la futura administración).

## Sobreventa

Un pedido solo llega al pago si su stock se ha reservado:

1. `InventoryService.reserve` descuenta todas las líneas con
   `UPDATE … SET stock_quantity = stock_quantity - n WHERE id = ? AND stock_quantity >= n`.
   Si alguna no afecta filas, responde 409 y la transacción devuelve lo ya descontado.
   Las líneas se procesan por id para que pedidos simultáneos no se interbloqueen.
2. `ck_product_variants_stock` (`stock_quantity >= 0`) impide en MySQL cualquier stock
   negativo, venga de donde venga.
3. Un pedido sin pagar conserva la reserva hasta `stock_reserved_until`; al vencer se cancela
   y `InventoryService.release` devuelve las unidades (pendiente de implementar con el checkout).

Se prefiere esta solución a un trigger porque la regla queda en el código, es testeable con H2
y la restricción `CHECK` da la misma garantía en la base de datos.

## Pendiente

`payments` (Stripe o Redsys/Bizum), `shipping_methods` y `stock_movements` se añadirán con
el checkout.
