package com.sherrycardsshop.api.auth.service;

import com.sherrycardsshop.api.auth.dto.UserDto;

public record AuthSession(UserDto user, String accessToken, String refreshToken) {
}
