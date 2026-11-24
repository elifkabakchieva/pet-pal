package com.petpal.review;

public record ReviewSubmittedEvent(Long bookingId, Long providerId, Long providerUserId, String authorName,
        int rating) {
}
