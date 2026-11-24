package com.petpal.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateBookingRequest(
        @NotNull Long offeringId,
        @NotNull Long petId,
        @NotNull Instant startAt,
        @Size(max = 500) String notes) {
}
