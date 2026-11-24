package com.petpal.provider;

import java.math.BigDecimal;
import java.time.Instant;

public record ProviderProfileResponse(
        Long id,
        String displayName,
        String bio,
        String city,
        String address,
        int yearsExperience,
        VerificationStatus verificationStatus,
        String verificationNote,
        Instant submittedAt,
        Instant verifiedAt,
        BigDecimal ratingAverage,
        int reviewCount) {

    public static ProviderProfileResponse from(ProviderProfile profile) {
        return new ProviderProfileResponse(profile.getId(), profile.getDisplayName(), profile.getBio(),
                profile.getCity(), profile.getAddress(), profile.getYearsExperience(),
                profile.getVerificationStatus(), profile.getVerificationNote(), profile.getSubmittedAt(),
                profile.getVerifiedAt(), profile.getRatingAverage(), profile.getReviewCount());
    }
}
