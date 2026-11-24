package com.petpal.review;

import com.petpal.booking.Booking;
import com.petpal.booking.BookingService;
import com.petpal.common.error.ConflictException;
import com.petpal.common.web.PageResponse;
import com.petpal.provider.ProviderProfile;
import com.petpal.provider.ProviderProfileService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final BookingService bookings;
    private final ProviderProfileService profiles;
    private final ApplicationEventPublisher events;

    ReviewService(ReviewRepository reviews, BookingService bookings, ProviderProfileService profiles,
            ApplicationEventPublisher events) {
        this.reviews = reviews;
        this.bookings = bookings;
        this.profiles = profiles;
        this.events = events;
    }

    @Transactional
    public ReviewResponse create(Long ownerId, Long bookingId, ReviewRequest request) {
        Booking booking = bookings.requireOwnBooking(ownerId, bookingId);
        booking.markReviewed();
        Review review;
        try {
            review = reviews.saveAndFlush(Review.of(booking, request.rating(), trimToNull(request.comment())));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("ALREADY_REVIEWED", "You have already reviewed this booking.");
        }
        ProviderProfile provider = booking.getProvider();
        RatingSummary summary = reviews.summarize(provider.getId());
        provider.applyRating(summary.roundedAverage(), summary.reviewCount());
        events.publishEvent(new ReviewSubmittedEvent(booking.getId(), provider.getId(), provider.getUser().getId(),
                booking.getOwner().shortName(), review.getRating()));
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> forProvider(Long providerId, int page, int size) {
        profiles.requireVerified(providerId);
        return PageResponse.from(
                reviews.findByProviderIdOrderByCreatedAtDescIdDesc(providerId, PageRequest.of(page, size)),
                ReviewResponse::from);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
