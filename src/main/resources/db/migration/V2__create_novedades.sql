CREATE TABLE IF NOT EXISTS novedades (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    category_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_novedades PRIMARY KEY (id),
    CONSTRAINT uk_novedades_slug UNIQUE (slug),
    CONSTRAINT fk_novedades_categories FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE INDEX idx_novedades_active_display_order
    ON novedades (active, display_order);

INSERT IGNORE INTO novedades (title, slug, description, category_id, active, display_order)
SELECT 'EB-05 de One Piece',
       'eb-05-one-piece',
       'Novedad del set EB-05 de One Piece.',
       categories.id,
       TRUE,
       1
FROM categories
WHERE categories.slug = 'one-piece';

INSERT IGNORE INTO novedades (title, slug, description, category_id, active, display_order)
SELECT '30 aniversario de Pokémon',
       '30-aniversario-pokemon',
       'Novedades por el 30 aniversario de Pokémon.',
       categories.id,
       TRUE,
       2
FROM categories
WHERE categories.slug = 'pokemon';
