package com.petpal.review;

import java.time.Instant;

public record ReviewResponse(Long id, int rating, String comment, String authorName, Instant createdAt) {

    static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getRating(), review.getComment(),
                review.getAuthor().shortName(), review.getCreatedAt());
    }
}
