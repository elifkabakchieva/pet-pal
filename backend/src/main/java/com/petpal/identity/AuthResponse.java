package com.petpal.identity;

import java.time.Instant;

public record AuthResponse(String accessToken, Instant expiresAt, UserResponse user) {
}
