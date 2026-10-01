package com.sherrycardsshop.api.config.site;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Con la tienda cerrada al público solo los administradores pueden iniciar sesión y usar la API;
 * el resto de visitantes ve la página de "próximamente" del front.
 */
@ConfigurationProperties(prefix = "app.site")
public record SiteProperties(boolean closed) {
}
