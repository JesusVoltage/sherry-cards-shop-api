CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE INDEX idx_categories_active_display_order
    ON categories (active, display_order);

INSERT INTO categories (name, slug, active, display_order)
VALUES
    ('Pokémon', 'pokemon', TRUE, 1),
    ('One Piece', 'one-piece', TRUE, 2),
    ('Magic', 'magic', TRUE, 3),
    ('Lorcana', 'lorcana', TRUE, 4),
    ('Yu-Gi-Oh!', 'yu-gi-oh', TRUE, 5),
    ('Accesorios', 'accesorios', TRUE, 6);