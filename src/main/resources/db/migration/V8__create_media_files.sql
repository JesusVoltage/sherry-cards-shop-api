-- Imágenes subidas a Cloudflare R2 desde el panel. Su suma limita el almacenamiento total.
CREATE TABLE media_files (
    id BIGINT NOT NULL AUTO_INCREMENT,
    object_key VARCHAR(300) NOT NULL,
    url VARCHAR(500) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_by BIGINT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_media_files PRIMARY KEY (id),
    CONSTRAINT uk_media_files_object_key UNIQUE (object_key),
    CONSTRAINT uk_media_files_url UNIQUE (url),
    CONSTRAINT fk_media_files_usuarios FOREIGN KEY (uploaded_by) REFERENCES usuarios (id) ON DELETE SET NULL,
    CONSTRAINT ck_media_files_size CHECK (size_bytes > 0)
);

CREATE INDEX idx_product_images_url ON product_images (url);
