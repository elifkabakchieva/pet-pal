package com.petpal.discovery;

import com.petpal.catalog.CategoryCode;
import java.math.BigDecimal;
import java.util.List;

public record ProviderSummaryResponse(
        Long id,
        String displayName,
        String city,
        String bioExcerpt,
        BigDecimal ratingAverage,
        int reviewCount,
        int yearsExperience,
        List<CategoryCode> categories,
        BigDecimal startingPrice) {
}
