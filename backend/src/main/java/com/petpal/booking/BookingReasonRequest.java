package com.petpal.booking;

import jakarta.validation.constraints.Size;

public record BookingReasonRequest(@Size(max = 500) String reason) {
}
