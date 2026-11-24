package com.petpal.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableClock extends Clock {

    private final AtomicReference<Instant> fixedInstant = new AtomicReference<>();

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant(), zone);
    }

    @Override
    public Instant instant() {
        Instant fixed = fixedInstant.get();
        return fixed != null ? fixed : Instant.now();
    }

    public void setInstant(Instant instant) {
        fixedInstant.set(instant);
    }

    public void advance(Duration duration) {
        fixedInstant.set(instant().plus(duration));
    }

    public void reset() {
        fixedInstant.set(null);
    }
}
