-- Cajón de sastre: todo producto necesita categoría y aquí van los que aún no tienen una o los de
-- una categoría que se borra. Oculta en la tienda; el panel no deja borrarla, moverla ni anidar en ella.
INSERT INTO categories (name, slug, description, active, display_order)
VALUES ('Sin categoría', 'sin-categoria', 'Productos pendientes de clasificar.', FALSE, 999);
