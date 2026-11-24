package com.petpal.review;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record RatingSummary(Double average, Long count) {

    BigDecimal roundedAverage() {
        if (average == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
    }

    int reviewCount() {
        return count == null ? 0 : count.intValue();
    }
}
