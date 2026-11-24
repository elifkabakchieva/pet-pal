package com.petpal.booking;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SlotCalculator {

    private final Duration step;

    SlotCalculator(Duration step) {
        if (step.isZero() || step.isNegative()) {
            throw new IllegalArgumentException("step must be positive");
        }
        this.step = step;
    }

    List<TimeRange> freeSlots(LocalDate date, ZoneId zone, List<DailyWindow> windows, Duration serviceDuration,
            List<TimeRange> busy, Instant notBefore) {
        List<TimeRange> slots = new ArrayList<>();
        for (DailyWindow window : windows) {
            ZonedDateTime cursor = date.atTime(window.start()).atZone(zone);
            ZonedDateTime windowEnd = date.atTime(window.end()).atZone(zone);
            while (!cursor.plus(serviceDuration).isAfter(windowEnd)) {
                TimeRange candidate = new TimeRange(cursor.toInstant(), cursor.plus(serviceDuration).toInstant());
                boolean startsLateEnough = !candidate.start().isBefore(notBefore);
                if (startsLateEnough && busy.stream().noneMatch(candidate::overlaps)) {
                    slots.add(candidate);
                }
                cursor = cursor.plus(step);
            }
        }
        slots.sort(Comparator.comparing(TimeRange::start));
        return List.copyOf(slots);
    }
}
