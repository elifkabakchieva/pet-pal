package com.petpal.provider;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityWindow(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime,
        @NotNull LocalTime endTime) {

    public static AvailabilityWindow from(AvailabilityRule rule) {
        return new AvailabilityWindow(rule.getDayOfWeek(), rule.getStartTime(), rule.getEndTime());
    }
}
