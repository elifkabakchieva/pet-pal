package com.petpal.notification;

import com.petpal.common.error.ResourceNotFoundException;
import com.petpal.common.web.PageResponse;
import java.time.Clock;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final Clock clock;

    NotificationService(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, int page, int size) {
        return PageResponse.from(
                notifications.findByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(page, size)),
                NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notifications.countByUserIdAndReadAtIsNull(userId));
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("NOTIFICATION_NOT_FOUND",
                        "This notification does not exist."))
                .markRead(clock.instant());
    }

    @Transactional
    public void markAllRead(Long userId) {
        notifications.markAllRead(userId, clock.instant());
    }

    @Transactional
    void notify(Long userId, NotificationType type, String message, String link) {
        notifications.save(Notification.create(userId, type, message, link));
    }
}
