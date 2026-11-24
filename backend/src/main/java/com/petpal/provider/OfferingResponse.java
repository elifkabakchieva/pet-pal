package com.petpal.provider;

import com.petpal.catalog.CategoryCode;
import java.math.BigDecimal;

public record OfferingResponse(
        Long id,
        CategoryCode categoryCode,
        String categoryName,
        String title,
        String description,
        BigDecimal price,
        int durationMinutes,
        boolean active) {

    public static OfferingResponse from(ServiceOffering offering) {
        return new OfferingResponse(offering.getId(), offering.getCategory().getCode(),
                offering.getCategory().getName(), offering.getTitle(), offering.getDescription(),
                offering.getPrice(), offering.getDurationMinutes(), offering.isActive());
    }
}
