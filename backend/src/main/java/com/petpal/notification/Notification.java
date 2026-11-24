package com.petpal.notification;

import com.petpal.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    static final int MAX_MESSAGE_LENGTH = 300;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private NotificationType type;

    @Column(nullable = false, length = MAX_MESSAGE_LENGTH, updatable = false)
    private String message;

    @Column(length = 200, updatable = false)
    private String link;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
    }

    static Notification create(Long userId, NotificationType type, String message, String link) {
        Notification notification = new Notification();
        notification.userId = userId;
        notification.type = type;
        notification.message = message.length() > MAX_MESSAGE_LENGTH
                ? message.substring(0, MAX_MESSAGE_LENGTH - 1) + "…"
                : message;
        notification.link = link;
        return notification;
    }

    void markRead(Instant now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getLink() {
        return link;
    }

    public boolean isRead() {
        return readAt != null;
    }
}
