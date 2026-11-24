package com.petpal.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "petpal.booking.expiry-enabled", havingValue = "true", matchIfMissing = true)
class BookingExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);

    private final BookingService bookingService;

    BookingExpiryJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${petpal.booking.expiry-check-interval}", initialDelayString = "PT1M")
    void expireOverdueRequests() {
        int expired = bookingService.expireOverdueRequests();
        if (expired > 0) {
            log.info("Expired {} unanswered booking request(s)", expired);
        }
    }
}
