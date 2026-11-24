package com.petpal.booking;

import java.util.EnumSet;
import java.util.Set;

public enum BookingStatus {
    REQUESTED,
    CONFIRMED,
    DECLINED,
    CANCELLED,
    COMPLETED;

    public static final Set<BookingStatus> ACTIVE = EnumSet.of(REQUESTED, CONFIRMED);

    public Set<BookingStatus> allowedTransitions() {
        return switch (this) {
            case REQUESTED -> EnumSet.of(CONFIRMED, DECLINED, CANCELLED);
            case CONFIRMED -> EnumSet.of(CANCELLED, COMPLETED);
            case DECLINED, CANCELLED, COMPLETED -> EnumSet.noneOf(BookingStatus.class);
        };
    }

    public boolean canTransitionTo(BookingStatus target) {
        return allowedTransitions().contains(target);
    }
}
