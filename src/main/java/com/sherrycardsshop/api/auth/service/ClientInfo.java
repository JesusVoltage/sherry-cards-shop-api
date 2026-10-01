package com.sherrycardsshop.api.auth.service;

/**
 * Datos del cliente HTTP que se guardan junto a cada refresh token.
 */
public record ClientInfo(String userAgent, String ipAddress) {

    public ClientInfo {
        userAgent = truncate(userAgent, 500);
        ipAddress = truncate(ipAddress, 45);
    }

    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
