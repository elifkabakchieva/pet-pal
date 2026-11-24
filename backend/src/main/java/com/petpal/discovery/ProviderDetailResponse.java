package com.petpal.discovery;

import com.petpal.provider.AvailabilityWindow;
import com.petpal.provider.OfferingResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProviderDetailResponse(
        Long id,
        String displayName,
        String bio,
        String city,
        String address,
        int yearsExperience,
        BigDecimal ratingAverage,
        int reviewCount,
        Instant memberSince,
        String currency,
        List<OfferingResponse> services,
        List<AvailabilityWindow> weeklyHours) {
}
