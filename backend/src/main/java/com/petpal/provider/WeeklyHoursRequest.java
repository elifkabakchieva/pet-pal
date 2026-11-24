package com.petpal.provider;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WeeklyHoursRequest(@NotNull @Size(max = 21) List<@Valid @NotNull AvailabilityWindow> windows) {
}
