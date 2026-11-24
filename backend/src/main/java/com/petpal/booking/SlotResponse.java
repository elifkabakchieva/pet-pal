package com.petpal.booking;

import java.time.Instant;

public record SlotResponse(Instant startAt, Instant endAt) {

    static SlotResponse from(TimeRange range) {
        return new SlotResponse(range.start(), range.end());
    }
}
