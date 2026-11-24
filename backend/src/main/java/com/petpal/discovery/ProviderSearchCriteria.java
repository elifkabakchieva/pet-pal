package com.petpal.discovery;

import com.petpal.catalog.CategoryCode;
import java.math.BigDecimal;

public record ProviderSearchCriteria(
        CategoryCode category, String city, BigDecimal maxPrice, BigDecimal minRating, String text) {

    public ProviderSearchCriteria {
        city = blankToNull(city);
        text = blankToNull(text);
    }

    boolean filtersOnServices() {
        return category != null || maxPrice != null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
