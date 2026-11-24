package com.petpal.booking;

import com.petpal.common.error.BusinessRuleException;
import com.petpal.common.error.ConflictException;
import com.petpal.common.error.ForbiddenOperationException;
import com.petpal.common.persistence.BaseEntity;
import com.petpal.identity.User;
import com.petpal.pet.Pet;
import com.petpal.provider.ProviderProfile;
import com.petpal.provider.ServiceOffering;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    static final String EXPIRY_NOTE = "Request expired without an answer";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false, updatable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false, updatable = false)
    private ProviderProfile provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false, updatable = false)
    private ServiceOffering offering;

    @Column(name = "start_at", nullable = false, updatable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false, updatable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal price;

    @Column(name = "owner_notes", length = 500, updatable = false)
    private String ownerNotes;

    @Column(nullable = false)
    private boolean reviewed;

    @Version
    private long version;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC, id ASC")
    private List<BookingEvent> events = new ArrayList<>();

    protected Booking() {
    }

    public static Booking request(User owner, Pet pet, ServiceOffering offering, Instant startAt, String ownerNotes,
            Instant now) {
        Booking booking = new Booking();
        booking.owner = owner;
        booking.pet = pet;
        booking.provider = offering.getProvider();
        booking.offering = offering;
        booking.startAt = startAt;
        booking.endAt = startAt.plus(offering.duration());
        booking.price = offering.getPrice();
        booking.ownerNotes = ownerNotes;
        booking.status = BookingStatus.REQUESTED;
        booking.events.add(BookingEvent.of(booking, null, BookingStatus.REQUESTED, owner.getId(), ownerNotes, now));
        return booking;
    }

    public Optional<BookingParty> partyOf(Long userId) {
        if (Objects.equals(owner.getId(), userId)) {
            return Optional.of(BookingParty.OWNER);
        }
        if (Objects.equals(provider.getUser().getId(), userId)) {
            return Optional.of(BookingParty.PROVIDER);
        }
        return Optional.empty();
    }

    public void confirm(BookingParty party, Long actorUserId, Instant now) {
        requireParty(party, BookingParty.PROVIDER);
        requireTransition(BookingStatus.CONFIRMED, "confirmed");
        requireNotStarted(now);
        transition(BookingStatus.CONFIRMED, actorUserId, null, now);
    }

    public void decline(BookingParty party, Long actorUserId, String reason, Instant now) {
        requireParty(party, BookingParty.PROVIDER);
        requireTransition(BookingStatus.DECLINED, "declined");
        transition(BookingStatus.DECLINED, actorUserId, reason, now);
    }

    public void cancel(BookingParty party, Long actorUserId, String reason, Instant now, Duration ownerCutoff) {
        requireTransition(BookingStatus.CANCELLED, "cancelled");
        requireNotStarted(now);
        if (party == BookingParty.OWNER && status == BookingStatus.CONFIRMED
                && !now.isBefore(startAt.minus(ownerCutoff))) {
            throw new BusinessRuleException("CANCELLATION_WINDOW_CLOSED",
                    "Confirmed bookings can be cancelled up to %d hours before the start. Please contact the provider."
                            .formatted(ownerCutoff.toHours()));
        }
        transition(BookingStatus.CANCELLED, actorUserId, reason, now);
    }

    public void complete(BookingParty party, Long actorUserId, Instant now) {
        requireParty(party, BookingParty.PROVIDER);
        requireTransition(BookingStatus.COMPLETED, "completed");
        if (now.isBefore(startAt)) {
            throw new BusinessRuleException("BOOKING_NOT_STARTED",
                    "You can mark the booking as completed once it has started.");
        }
        transition(BookingStatus.COMPLETED, actorUserId, null, now);
    }

    public void expire(Instant now) {
        requireTransition(BookingStatus.CANCELLED, "expired");
        if (status != BookingStatus.REQUESTED || now.isBefore(startAt)) {
            throw new IllegalStateException("Only requests whose start time has passed can expire");
        }
        transition(BookingStatus.CANCELLED, null, EXPIRY_NOTE, now);
    }

    public void markReviewed() {
        if (status != BookingStatus.COMPLETED) {
            throw new BusinessRuleException("BOOKING_NOT_COMPLETED",
                    "You can leave a review once the booking is completed.");
        }
        if (reviewed) {
            throw new ConflictException("ALREADY_REVIEWED", "You have already reviewed this booking.");
        }
        reviewed = true;
    }

    public List<BookingAction> availableActions(BookingParty party, Instant now, Duration ownerCutoff) {
        boolean started = !now.isBefore(startAt);
        return switch (party) {
            case OWNER -> switch (status) {
                case REQUESTED -> started ? List.of() : List.of(BookingAction.CANCEL);
                case CONFIRMED -> now.isBefore(startAt.minus(ownerCutoff)) ? List.of(BookingAction.CANCEL) : List.of();
                case COMPLETED -> reviewed ? List.of() : List.of(BookingAction.REVIEW);
                case DECLINED, CANCELLED -> List.of();
            };
            case PROVIDER -> switch (status) {
                case REQUESTED -> started
                        ? List.of(BookingAction.DECLINE)
                        : List.of(BookingAction.CONFIRM, BookingAction.DECLINE);
                case CONFIRMED -> started ? List.of(BookingAction.COMPLETE) : List.of(BookingAction.CANCEL);
                case DECLINED, CANCELLED, COMPLETED -> List.of();
            };
        };
    }

    private void requireParty(BookingParty actual, BookingParty required) {
        if (actual != required) {
            throw new ForbiddenOperationException("NOT_ALLOWED_FOR_PARTY",
                    "Only the %s can do this.".formatted(required.name().toLowerCase(Locale.ROOT)));
        }
    }

    private void requireTransition(BookingStatus target, String verb) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "This booking is %s and cannot be %s.".formatted(status.name().toLowerCase(Locale.ROOT), verb));
        }
    }

    private void requireNotStarted(Instant now) {
        if (!now.isBefore(startAt)) {
            throw new BusinessRuleException("BOOKING_ALREADY_STARTED", "This booking has already started.");
        }
    }

    private void transition(BookingStatus target, Long actorUserId, String note, Instant now) {
        events.add(BookingEvent.of(this, status, target, actorUserId, note, now));
        status = target;
    }

    public User getOwner() {
        return owner;
    }

    public Pet getPet() {
        return pet;
    }

    public ProviderProfile getProvider() {
        return provider;
    }

    public ServiceOffering getOffering() {
        return offering;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getOwnerNotes() {
        return ownerNotes;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public List<BookingEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }
}
