package com.sherrycardsshop.api.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

import com.sherrycardsshop.api.auth.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Comprueba si una contraseña aparece en filtraciones públicas con la API de rangos de
 * Pwned Passwords (k-anonimato): solo se envían los 5 primeros caracteres del SHA-1, nunca la
 * contraseña ni su hash completo. Si el servicio no responde, la comprobación se omite para no
 * bloquear registros ni cambios de contraseña.
 */
@Component
public class BreachedPasswordChecker {

    private static final Logger logger = LoggerFactory.getLogger(BreachedPasswordChecker.class);
    private static final String BASE_URL = "https://api.pwnedpasswords.com";

    private final RestClient restClient;
    private final boolean enabled;

    @Autowired
    public BreachedPasswordChecker(RestClient.Builder builder, AuthProperties authProperties) {
        this(builder.requestFactory(timeouts()).build(), authProperties.breachedPasswordCheck());
    }

    BreachedPasswordChecker(RestClient restClient, boolean enabled) {
        this.restClient = restClient;
        this.enabled = enabled;
    }

    public boolean isBreached(String password) {
        if (!enabled) {
            return false;
        }
        String hash = sha1Hex(password);
        String prefix = hash.substring(0, 5);
        String suffix = hash.substring(5);
        try {
            String body = restClient.get()
                    .uri(BASE_URL + "/range/{prefix}", prefix)
                    // Respuestas de tamaño uniforme para que el tráfico no revele el prefijo consultado.
                    .header("Add-Padding", "true")
                    .header(HttpHeaders.USER_AGENT, "sherry-cards-shop-api")
                    .retrieve()
                    .body(String.class);
            return body != null && body.lines().anyMatch(line -> isBreachedEntry(line, suffix));
        } catch (RestClientException exception) {
            logger.warn("No se pudo consultar Pwned Passwords; se omite la comprobación: {}", exception.getMessage());
            return false;
        }
    }

    private static boolean isBreachedEntry(String line, String suffix) {
        int separator = line.indexOf(':');
        // Las entradas de relleno tienen recuento 0.
        return separator > 0
                && line.substring(0, separator).equalsIgnoreCase(suffix)
                && !line.substring(separator + 1).trim().equals("0");
    }

    private static String sha1Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1 no disponible", exception);
        }
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        return factory;
    }
}
