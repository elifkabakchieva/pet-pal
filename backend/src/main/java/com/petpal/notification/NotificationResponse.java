package com.petpal.notification;

import java.time.Instant;

public record NotificationResponse(
        Long id, NotificationType type, String message, String link, boolean read, Instant createdAt) {

    static NotificationResponse from(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getMessage(),
                notification.getLink(), notification.isRead(), notification.getCreatedAt());
    }
}
