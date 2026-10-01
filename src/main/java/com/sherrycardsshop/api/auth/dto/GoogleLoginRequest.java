package com.sherrycardsshop.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code credential} es el ID token que Google Identity Services entrega al frontend.
 */
public record GoogleLoginRequest(@NotBlank @Size(max = 4096) String credential) {
}
