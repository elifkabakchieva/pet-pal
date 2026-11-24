package com.petpal.provider;

public record ProviderVerificationDecidedEvent(Long providerId, Long providerUserId, boolean approved, String note) {
}
