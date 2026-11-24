package com.petpal.booking;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record BookingDetailResponse(BookingResponse booking, List<TimelineEntry> timeline) {

    public record TimelineEntry(
            BookingStatus fromStatus, BookingStatus toStatus, String actorName, String note, Instant occurredAt) {
    }

    static List<TimelineEntry> timelineOf(Booking booking) {
        Long ownerId = booking.getOwner().getId();
        Long providerUserId = booking.getProvider().getUser().getId();
        return booking.getEvents().stream()
                .map(event -> new TimelineEntry(event.getFromStatus(), event.getToStatus(),
                        actorName(event.getActorUserId(), ownerId, providerUserId, booking), event.getNote(),
                        event.getOccurredAt()))
                .toList();
    }

    private static String actorName(Long actorId, Long ownerId, Long providerUserId, Booking booking) {
        if (actorId == null) {
            return "PetPal";
        }
        if (Objects.equals(actorId, ownerId)) {
            return booking.getOwner().shortName();
        }
        if (Objects.equals(actorId, providerUserId)) {
            return booking.getProvider().getDisplayName();
        }
        return "PetPal";
    }
}
