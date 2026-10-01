package com.sherrycardsshop.api.media.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Almacenamiento de imágenes en Cloudflare R2 (API compatible con S3). Sin endpoint, claves,
 * bucket o URL pública la subida queda desactivada y la API responde 503.
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String accessKeyId,
        String secretAccessKey,
        String bucket,
        String publicUrl,
        DataSize maxImageSize,
        DataSize maxTotalSize) {

    public boolean enabled() {
        return hasText(endpoint) && hasText(accessKeyId) && hasText(secretAccessKey) && hasText(bucket) && hasText(publicUrl);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
