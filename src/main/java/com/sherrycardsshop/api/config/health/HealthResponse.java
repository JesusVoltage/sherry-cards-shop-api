package com.sherrycardsshop.api.config.health;

public record HealthResponse(String status, String application, String version) {
}