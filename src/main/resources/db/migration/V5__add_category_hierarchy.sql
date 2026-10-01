-- Subcategorías anidables: cada categoría puede colgar de otra (Pokémon › Expansiones › …).
-- Las categorías raíz tienen parent_id NULL. No se puede borrar una categoría con hijas.
-- MySQL no admite CHECK sobre columnas AUTO_INCREMENT, así que impedir que una categoría sea
-- su propio padre (o crear ciclos) es responsabilidad de la aplicación.
ALTER TABLE categories ADD COLUMN parent_id BIGINT NULL;

ALTER TABLE categories
    ADD CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id);

CREATE INDEX idx_categories_parent_display_order ON categories (parent_id, display_order);
