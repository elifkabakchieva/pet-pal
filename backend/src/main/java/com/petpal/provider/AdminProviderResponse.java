package com.petpal.provider;

import java.time.Instant;

public record AdminProviderResponse(
        Long id,
        String displayName,
        String bio,
        String city,
        String address,
        int yearsExperience,
        VerificationStatus verificationStatus,
        String verificationNote,
        Instant submittedAt,
        String contactName,
        String email,
        String phone) {

    static AdminProviderResponse from(ProviderProfile profile) {
        return new AdminProviderResponse(profile.getId(), profile.getDisplayName(), profile.getBio(),
                profile.getCity(), profile.getAddress(), profile.getYearsExperience(),
                profile.getVerificationStatus(), profile.getVerificationNote(), profile.getSubmittedAt(),
                profile.getUser().fullName(), profile.getUser().getEmail(), profile.getUser().getPhone());
    }
}
