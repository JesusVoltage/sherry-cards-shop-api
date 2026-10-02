package com.sherrycardsshop.api.admin.dto;

import java.time.LocalDateTime;

public record AdminUserDto(
        Long id,
        String username,
        String email,
        String nombre,
        String apellidos,
        String role,
        String status,
        LocalDateTime emailVerifiedAt,
        LocalDateTime lastAccessAt,
        LocalDateTime createdAt,
        boolean hasPassword,
        boolean googleLinked) {
}
