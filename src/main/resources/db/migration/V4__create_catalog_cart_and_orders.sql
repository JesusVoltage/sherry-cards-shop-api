-- Catálogo con variantes, carrito y pedidos.
-- Importes en EUR con IVA incluido (venta B2C en España). Fechas en UTC.

-- ---------------------------------------------------------------- Catálogo

CREATE TABLE product_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_product_types PRIMARY KEY (id),
    CONSTRAINT uk_product_types_code UNIQUE (code)
);

INSERT INTO product_types (code, name)
VALUES ('SEALED', 'Producto sellado'), ('SINGLE', 'Carta suelta'), ('ACCESSORY', 'Accesorio');

CREATE TABLE product_statuses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_product_statuses PRIMARY KEY (id),
    CONSTRAINT uk_product_statuses_code UNIQUE (code)
);

INSERT INTO product_statuses (code, name)
VALUES ('DRAFT', 'Borrador'), ('ACTIVE', 'Activo'), ('ARCHIVED', 'Archivado');

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    product_type_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    release_date DATE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_slug UNIQUE (slug),
    CONSTRAINT fk_products_categories FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_product_types FOREIGN KEY (product_type_id) REFERENCES product_types (id),
    CONSTRAINT fk_products_product_statuses FOREIGN KEY (status_id) REFERENCES product_statuses (id)
);

CREATE INDEX idx_products_category_status ON products (category_id, status_id);
CREATE INDEX idx_products_status_release ON products (status_id, release_date);

-- Unidad vendible: idioma, edición promo, color… Cada variante tiene su SKU, precio y stock.
CREATE TABLE product_variants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    compare_at_price DECIMAL(10, 2),
    vat_rate DECIMAL(5, 2) NOT NULL DEFAULT 21.00,
    stock_quantity INT NOT NULL DEFAULT 0,
    weight_grams INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_product_variants PRIMARY KEY (id),
    CONSTRAINT uk_product_variants_sku UNIQUE (sku),
    CONSTRAINT fk_product_variants_products FOREIGN KEY (product_id) REFERENCES products (id),
    -- Garantía final contra la sobreventa: ninguna operación puede dejar el stock en negativo.
    CONSTRAINT ck_product_variants_stock CHECK (stock_quantity >= 0),
    CONSTRAINT ck_product_variants_price CHECK (price >= 0),
    CONSTRAINT ck_product_variants_compare_price CHECK (compare_at_price IS NULL OR compare_at_price > price),
    CONSTRAINT ck_product_variants_vat CHECK (vat_rate >= 0 AND vat_rate <= 100),
    CONSTRAINT ck_product_variants_weight CHECK (weight_grams IS NULL OR weight_grams > 0)
);

CREATE INDEX idx_product_variants_product ON product_variants (product_id, display_order);

CREATE TABLE product_variant_attributes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_variant_id BIGINT NOT NULL,
    attribute_name VARCHAR(50) NOT NULL,
    attribute_value VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_variant_attributes PRIMARY KEY (id),
    CONSTRAINT uk_product_variant_attributes_name UNIQUE (product_variant_id, attribute_name),
    CONSTRAINT fk_product_variant_attributes_variants FOREIGN KEY (product_variant_id)
        REFERENCES product_variants (id) ON DELETE CASCADE
);

CREATE TABLE product_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    alt_text VARCHAR(200),
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_product_images PRIMARY KEY (id),
    CONSTRAINT fk_product_images_products FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product ON product_images (product_id, display_order);

-- ---------------------------------------------------------------- Carrito

-- Un carrito por usuario. El precio no se guarda: se muestra el actual y se fija en el pedido.
CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_carts PRIMARY KEY (id),
    CONSTRAINT uk_carts_user UNIQUE (user_id),
    CONSTRAINT fk_carts_usuarios FOREIGN KEY (user_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_cart_items PRIMARY KEY (id),
    CONSTRAINT uk_cart_items_variant UNIQUE (cart_id, product_variant_id),
    CONSTRAINT fk_cart_items_carts FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_product_variants FOREIGN KEY (product_variant_id)
        REFERENCES product_variants (id) ON DELETE CASCADE,
    CONSTRAINT ck_cart_items_quantity CHECK (quantity > 0 AND quantity <= 99)
);

CREATE INDEX idx_cart_items_variant ON cart_items (product_variant_id);

-- ---------------------------------------------------------------- Pedidos

CREATE TABLE order_statuses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(100) NOT NULL,
    final_status BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_order_statuses PRIMARY KEY (id),
    CONSTRAINT uk_order_statuses_code UNIQUE (code)
);

INSERT INTO order_statuses (code, name, final_status, display_order)
VALUES
    ('PENDING_PAYMENT', 'Pendiente de pago', FALSE, 1),
    ('PAID', 'Pagado', FALSE, 2),
    ('PROCESSING', 'En preparación', FALSE, 3),
    ('SHIPPED', 'Enviado', FALSE, 4),
    ('DELIVERED', 'Entregado', TRUE, 5),
    ('CANCELLED', 'Cancelado', TRUE, 6),
    ('REFUNDED', 'Reembolsado', TRUE, 7);

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(30) NOT NULL,
    user_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    email VARCHAR(254) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    shipping_total DECIMAL(10, 2) NOT NULL DEFAULT 0,
    discount_total DECIMAL(10, 2) NOT NULL DEFAULT 0,
    tax_total DECIMAL(10, 2) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    customer_note VARCHAR(500),
    placed_at DATETIME(6) NOT NULL,
    -- Fin de la reserva de stock de un pedido sin pagar; al vencer se cancela y el stock vuelve.
    stock_reserved_until DATETIME(6),
    paid_at DATETIME(6),
    shipped_at DATETIME(6),
    delivered_at DATETIME(6),
    cancelled_at DATETIME(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT uk_orders_order_number UNIQUE (order_number),
    CONSTRAINT fk_orders_usuarios FOREIGN KEY (user_id) REFERENCES usuarios (id),
    CONSTRAINT fk_orders_order_statuses FOREIGN KEY (status_id) REFERENCES order_statuses (id),
    CONSTRAINT ck_orders_amounts CHECK (subtotal >= 0 AND shipping_total >= 0 AND discount_total >= 0
        AND tax_total >= 0 AND total >= 0)
);

CREATE INDEX idx_orders_user_placed ON orders (user_id, placed_at);
CREATE INDEX idx_orders_status_reserved ON orders (status_id, stock_reserved_until);

-- Copia de lo comprado: el pedido no cambia si después se edita o archiva el producto.
CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_variant_id BIGINT,
    sku VARCHAR(64) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    variant_name VARCHAR(150) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    vat_rate DECIMAL(5, 2) NOT NULL,
    quantity INT NOT NULL,
    line_total DECIMAL(10, 2) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_orders FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_product_variants FOREIGN KEY (product_variant_id)
        REFERENCES product_variants (id) ON DELETE SET NULL,
    CONSTRAINT ck_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_items_amounts CHECK (unit_price >= 0 AND line_total >= 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);
CREATE INDEX idx_order_items_variant ON order_items (product_variant_id);

-- Copia de las direcciones: el cliente puede editar o borrar las suyas sin afectar al pedido.
CREATE TABLE order_addresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    address_type VARCHAR(20) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(150) NOT NULL,
    phone VARCHAR(30),
    street VARCHAR(200) NOT NULL,
    street_number VARCHAR(30) NOT NULL,
    address_line2 VARCHAR(150),
    postal_code VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    province VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    CONSTRAINT pk_order_addresses PRIMARY KEY (id),
    CONSTRAINT uk_order_addresses_type UNIQUE (order_id, address_type),
    CONSTRAINT fk_order_addresses_orders FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_addresses_type CHECK (address_type IN ('SHIPPING', 'BILLING'))
);

CREATE TABLE order_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    note VARCHAR(500),
    changed_by_user_id BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_order_status_history PRIMARY KEY (id),
    CONSTRAINT fk_order_status_history_orders FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_history_statuses FOREIGN KEY (status_id) REFERENCES order_statuses (id),
    CONSTRAINT fk_order_status_history_usuarios FOREIGN KEY (changed_by_user_id)
        REFERENCES usuarios (id) ON DELETE SET NULL
);

CREATE INDEX idx_order_status_history_order ON order_status_history (order_id, created_at);
