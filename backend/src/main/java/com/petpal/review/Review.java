package com.petpal.review;

import com.petpal.booking.Booking;
import com.petpal.common.persistence.BaseEntity;
import com.petpal.identity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review extends BaseEntity {

    @Column(name = "booking_id", nullable = false, updatable = false)
    private Long bookingId;

    @Column(name = "provider_id", nullable = false, updatable = false)
    private Long providerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false, updatable = false)
    private User author;

    @Column(nullable = false, updatable = false)
    private int rating;

    @Column(length = 1000, updatable = false)
    private String comment;

    protected Review() {
    }

    static Review of(Booking booking, int rating, String comment) {
        Review review = new Review();
        review.bookingId = booking.getId();
        review.providerId = booking.getProvider().getId();
        review.author = booking.getOwner();
        review.rating = rating;
        review.comment = comment;
        return review;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public Long getProviderId() {
        return providerId;
    }

    public User getAuthor() {
        return author;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }
}
