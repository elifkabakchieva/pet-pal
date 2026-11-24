package com.petpal.booking;

import java.time.Instant;

public record BookingStatusChangedEvent(
        Long bookingId,
        BookingStatus status,
        Actor actor,
        Long ownerUserId,
        Long providerUserId,
        String ownerName,
        String providerName,
        String petName,
        String serviceTitle,
        Instant startAt) {

    public enum Actor {
        OWNER,
        PROVIDER,
        SYSTEM
    }

    static BookingStatusChangedEvent of(Booking booking, Actor actor) {
        return new BookingStatusChangedEvent(
                booking.getId(),
                booking.getStatus(),
                actor,
                booking.getOwner().getId(),
                booking.getProvider().getUser().getId(),
                booking.getOwner().shortName(),
                booking.getProvider().getDisplayName(),
                booking.getPet().getName(),
                booking.getOffering().getTitle(),
                booking.getStartAt());
    }
}
