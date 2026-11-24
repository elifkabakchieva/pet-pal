package com.petpal.booking;

import java.time.Instant;

public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
