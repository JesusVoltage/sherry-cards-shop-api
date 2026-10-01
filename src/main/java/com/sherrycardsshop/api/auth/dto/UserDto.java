package com.sherrycardsshop.api.auth.dto;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String email,
        String nombre,
        String apellidos,
        String role,
        String status,
        LocalDateTime emailVerifiedAt,
        LocalDateTime lastAccessAt) {
}
