package com.petpal.notification;

import com.petpal.booking.BookingStatusChangedEvent;
import com.petpal.common.config.PetPalProperties;
import com.petpal.provider.ProviderVerificationDecidedEvent;
import com.petpal.review.ReviewSubmittedEvent;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class NotificationListener {

    private static final String WORKSPACE_PROFILE_LINK = "/workspace/profile";

    private final NotificationService notifications;
    private final DateTimeFormatter startFormat;

    NotificationListener(NotificationService notifications, PetPalProperties properties) {
        this.notifications = notifications;
        this.startFormat = DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.ENGLISH)
                .withZone(properties.businessZone());
    }

    @EventListener
    void on(BookingStatusChangedEvent event) {
        String what = "%s for %s on %s".formatted(event.serviceTitle(), event.petName(),
                startFormat.format(event.startAt()));
        String link = "/bookings/" + event.bookingId();
        switch (event.status()) {
            case REQUESTED -> notifications.notify(event.providerUserId(), NotificationType.BOOKING_REQUESTED,
                    "New request from %s: %s.".formatted(event.ownerName(), what), link);
            case CONFIRMED -> notifications.notify(event.ownerUserId(), NotificationType.BOOKING_CONFIRMED,
                    "%s confirmed %s.".formatted(event.providerName(), what), link);
            case DECLINED -> notifications.notify(event.ownerUserId(), NotificationType.BOOKING_DECLINED,
                    "%s declined %s.".formatted(event.providerName(), what), link);
            case CANCELLED -> notifyCancellation(event, what, link);
            case COMPLETED -> notifications.notify(event.ownerUserId(), NotificationType.BOOKING_COMPLETED,
                    "%s for %s is complete. You can now leave a review.".formatted(event.serviceTitle(),
                            event.petName()), link);
        }
    }

    @EventListener
    void on(ProviderVerificationDecidedEvent event) {
        if (event.approved()) {
            notifications.notify(event.providerUserId(), NotificationType.PROVIDER_VERIFIED,
                    "Your profile is verified. Owners can now find and book you.", WORKSPACE_PROFILE_LINK);
        } else {
            notifications.notify(event.providerUserId(), NotificationType.PROVIDER_REJECTED,
                    "Your profile was not verified: " + event.note(), WORKSPACE_PROFILE_LINK);
        }
    }

    @EventListener
    void on(ReviewSubmittedEvent event) {
        notifications.notify(event.providerUserId(), NotificationType.REVIEW_RECEIVED,
                "%s left a %d-star review.".formatted(event.authorName(), event.rating()),
                "/providers/" + event.providerId());
    }

    private void notifyCancellation(BookingStatusChangedEvent event, String what, String link) {
        switch (event.actor()) {
            case OWNER -> notifications.notify(event.providerUserId(), NotificationType.BOOKING_CANCELLED,
                    "%s cancelled %s.".formatted(event.ownerName(), what), link);
            case PROVIDER -> notifications.notify(event.ownerUserId(), NotificationType.BOOKING_CANCELLED,
                    "%s cancelled %s.".formatted(event.providerName(), what), link);
            case SYSTEM -> {
                String message = "The request for %s expired without an answer.".formatted(what);
                notifications.notify(event.ownerUserId(), NotificationType.BOOKING_EXPIRED, message, link);
                notifications.notify(event.providerUserId(), NotificationType.BOOKING_EXPIRED, message, link);
            }
        }
    }
}
