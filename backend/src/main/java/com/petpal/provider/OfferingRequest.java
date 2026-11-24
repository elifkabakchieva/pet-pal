package com.petpal.provider;

import com.petpal.catalog.CategoryCode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record OfferingRequest(
        @NotNull CategoryCode categoryCode,
        @NotBlank @Size(min = 3, max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("1.00") @DecimalMax("10000.00") @Digits(integer = 5, fraction = 2) BigDecimal price,
        @NotNull @Min(15) @Max(480) Integer durationMinutes) {

    @AssertTrue(message = "must be a multiple of 15 minutes")
    public boolean isDurationOnQuarterHour() {
        return durationMinutes == null || durationMinutes % 15 == 0;
    }
}
