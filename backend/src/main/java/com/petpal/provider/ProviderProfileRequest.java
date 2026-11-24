package com.petpal.provider;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProviderProfileRequest(
        @NotBlank @Size(min = 2, max = 120) String displayName,
        @Size(max = 2000) String bio,
        @NotBlank @Size(min = 2, max = 80) String city,
        @Size(max = 200) String address,
        @NotNull @Min(0) @Max(60) Integer yearsExperience) {
}
