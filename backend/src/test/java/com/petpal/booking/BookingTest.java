package com.petpal.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petpal.identity.Role;
import com.petpal.identity.User;
import com.petpal.pet.Pet;
import com.petpal.pet.Species;
import com.petpal.provider.ProviderProfile;
import com.petpal.provider.ServiceOffering;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BookingTest {

    private static final Instant START = Instant.parse("2026-12-10T08:00:00Z");
    private static final Instant REQUESTED_AT = START.minus(Duration.ofDays(3));
    private static final Duration CUTOFF = Duration.ofHours(12);

    @Test
    void requestCopiesPriceAndDurationAndRecordsTheFirstEvent() {
        Booking booking = newBooking();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.REQUESTED);
        assertThat(booking.getEndAt()).isEqualTo(START.plus(Duration.ofMinutes(90)));
        assertThat(booking.getPrice()).isEqualByComparingTo("45.00");
        assertThat(booking.getEvents()).singleElement().satisfies(event -> {
            assertThat(event.getFromStatus()).isNull();
            assertThat(event.getToStatus()).isEqualTo(BookingStatus.REQUESTED);
            assertThat(event.getNote()).isEqualTo("Quiet dryer please");
        });
    }

    @Test
    void providerConfirmsBeforeTheStart() {
        Booking booking = newBooking();

        booking.confirm(BookingParty.PROVIDER, 2L, START.minus(Duration.ofHours(1)));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getEvents()).hasSize(2);
    }

    @Test
    void ownerCannotConfirm() {
        Booking booking = newBooking();

        assertThatThrownBy(() -> booking.confirm(BookingParty.OWNER, 1L, REQUESTED_AT))
                .hasFieldOrPropertyWithValue("code", "NOT_ALLOWED_FOR_PARTY");
    }

    @Test
    void confirmingAfterTheStartIsTooLate() {
        Booking booking = newBooking();

        assertThatThrownBy(() -> booking.confirm(BookingParty.PROVIDER, 2L, START))
                .hasFieldOrPropertyWithValue("code", "BOOKING_ALREADY_STARTED");
    }

    @Test
    void ownerCancelsAConfirmedBookingOnlyBeforeTheCutoff() {
        Booking early = confirmedBooking();
        Booking late = confirmedBooking();

        early.cancel(BookingParty.OWNER, 1L, "Plans changed", START.minus(Duration.ofHours(13)), CUTOFF);

        assertThat(early.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> late.cancel(BookingParty.OWNER, 1L, null, START.minus(Duration.ofHours(11)), CUTOFF))
                .hasFieldOrPropertyWithValue("code", "CANCELLATION_WINDOW_CLOSED")
                .hasMessageContaining("12 hours");
    }

    @Test
    void providerCancelsAConfirmedBookingUntilTheStart() {
        Booking booking = confirmedBooking();

        booking.cancel(BookingParty.PROVIDER, 2L, "Ill today", START.minus(Duration.ofMinutes(30)), CUTOFF);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getEvents().getLast().getNote()).isEqualTo("Ill today");
    }

    @Test
    void completionNeedsAStartedConfirmedBooking() {
        Booking booking = confirmedBooking();

        assertThatThrownBy(() -> booking.complete(BookingParty.PROVIDER, 2L, START.minusSeconds(1)))
                .hasFieldOrPropertyWithValue("code", "BOOKING_NOT_STARTED");
        booking.complete(BookingParty.PROVIDER, 2L, START.plus(Duration.ofMinutes(5)));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
    }

    @Test
    void terminalBookingsCannotChange() {
        Booking booking = newBooking();
        booking.decline(BookingParty.PROVIDER, 2L, "Fully booked", REQUESTED_AT);

        assertThatThrownBy(() -> booking.confirm(BookingParty.PROVIDER, 2L, REQUESTED_AT))
                .hasFieldOrPropertyWithValue("code", "INVALID_STATUS_TRANSITION")
                .hasMessage("This booking is declined and cannot be confirmed.");
    }

    @Test
    void expiryCancelsWithoutActor() {
        Booking booking = newBooking();

        booking.expire(START.plusSeconds(1));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getEvents().getLast().getActorUserId()).isNull();
        assertThat(booking.getEvents().getLast().getNote()).isEqualTo(Booking.EXPIRY_NOTE);
    }

    @Test
    void availableActionsFollowTheRules() {
        Booking requested = newBooking();
        Booking confirmed = confirmedBooking();
        Instant dayBefore = START.minus(Duration.ofDays(1));

        assertThat(requested.availableActions(BookingParty.OWNER, dayBefore, CUTOFF)).containsExactly(BookingAction.CANCEL);
        assertThat(requested.availableActions(BookingParty.PROVIDER, dayBefore, CUTOFF))
                .containsExactly(BookingAction.CONFIRM, BookingAction.DECLINE);
        assertThat(requested.availableActions(BookingParty.PROVIDER, START, CUTOFF)).containsExactly(BookingAction.DECLINE);
        assertThat(confirmed.availableActions(BookingParty.OWNER, START.minus(Duration.ofHours(2)), CUTOFF)).isEmpty();
        assertThat(confirmed.availableActions(BookingParty.PROVIDER, START.minus(Duration.ofHours(2)), CUTOFF))
                .containsExactly(BookingAction.CANCEL);
        assertThat(confirmed.availableActions(BookingParty.PROVIDER, START, CUTOFF)).containsExactly(BookingAction.COMPLETE);
    }

    @Test
    void reviewIsOfferedOnceAfterCompletion() {
        Booking booking = confirmedBooking();
        booking.complete(BookingParty.PROVIDER, 2L, START);

        assertThat(booking.availableActions(BookingParty.OWNER, START, CUTOFF)).containsExactly(BookingAction.REVIEW);
        booking.markReviewed();
        assertThat(booking.availableActions(BookingParty.OWNER, START, CUTOFF)).isEmpty();
        assertThatThrownBy(booking::markReviewed).hasFieldOrPropertyWithValue("code", "ALREADY_REVIEWED");
    }

    private static Booking newBooking() {
        User owner = User.register("maria@example.com", "{noop}x", "Maria", "Kirova", null, "Sofia", Role.OWNER);
        User providerUser =
                User.register("georgi@example.com", "{noop}x", "Georgi", "Petrov", "+359 1", "Sofia", Role.PROVIDER);
        ProviderProfile provider = ProviderProfile.createDraft(providerUser, "Georgi's Grooming", "Sofia");
        Pet pet = Pet.create(1L, "Max", Species.DOG, null, null, null, null);
        ServiceOffering offering =
                ServiceOffering.create(provider, null, "Full groom", null, new BigDecimal("45.00"), 90);
        return Booking.request(owner, pet, offering, START, "Quiet dryer please", REQUESTED_AT);
    }

    private static Booking confirmedBooking() {
        Booking booking = newBooking();
        booking.confirm(BookingParty.PROVIDER, 2L, REQUESTED_AT);
        return booking;
    }
}
