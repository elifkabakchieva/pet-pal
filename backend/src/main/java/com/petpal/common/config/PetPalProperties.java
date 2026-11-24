package com.petpal.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "petpal")
public record PetPalProperties(
        @NotNull ZoneId businessZone,
        @NotBlank String currency,
        @Valid @NotNull Security security,
        @Valid @NotNull Admin admin,
        @Valid @NotNull Booking booking) {

    public record Security(
            @NotBlank @Size(min = 32, message = "must be at least 32 characters") String jwtSecret,
            @NotNull Duration tokenTtl) {
    }

    public record Admin(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password) {
    }

    public record Booking(
            @NotNull Duration slotStep,
            @NotNull Duration minLeadTime,
            @NotNull Period horizon,
            @NotNull Duration ownerCancellationCutoff,
            boolean expiryEnabled,
            @NotNull Duration expiryCheckInterval) {
    }
}
