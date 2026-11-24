package com.petpal.booking;

import com.petpal.booking.BookingStatusChangedEvent.Actor;
import com.petpal.common.config.PetPalProperties;
import com.petpal.common.error.BusinessRuleException;
import com.petpal.common.error.ConflictException;
import com.petpal.common.error.ResourceNotFoundException;
import com.petpal.common.web.PageResponse;
import com.petpal.identity.AuthenticatedUser;
import com.petpal.identity.Role;
import com.petpal.identity.UserRepository;
import com.petpal.pet.Pet;
import com.petpal.pet.PetService;
import com.petpal.provider.OfferingService;
import com.petpal.provider.ProviderProfileService;
import com.petpal.provider.ServiceOffering;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final String NO_OVERLAP_CONSTRAINT = "ex_bookings_provider_no_overlap";

    private final BookingRepository bookings;
    private final PetService pets;
    private final OfferingService offerings;
    private final ProviderProfileService providerProfiles;
    private final BookingSlotService slots;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final String currency;
    private final Duration ownerCancellationCutoff;

    BookingService(BookingRepository bookings, PetService pets, OfferingService offerings,
            ProviderProfileService providerProfiles, BookingSlotService slots, UserRepository users,
            ApplicationEventPublisher events, TransactionTemplate transactionTemplate, Clock clock,
            PetPalProperties properties) {
        this.bookings = bookings;
        this.pets = pets;
        this.offerings = offerings;
        this.providerProfiles = providerProfiles;
        this.slots = slots;
        this.users = users;
        this.events = events;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.currency = properties.currency();
        this.ownerCancellationCutoff = properties.booking().ownerCancellationCutoff();
    }

    @Transactional
    public BookingResponse request(AuthenticatedUser user, CreateBookingRequest request) {
        Pet pet = pets.requireActivePet(user.id(), request.petId());
        ServiceOffering offering = offerings.requireActiveWithProvider(request.offeringId());
        if (!offering.getProvider().isVerified()) {
            throw new BusinessRuleException("PROVIDER_NOT_BOOKABLE", "This provider cannot be booked at the moment.");
        }
        Instant startAt = request.startAt();
        if (!slots.isFree(offering, startAt)) {
            throw new BusinessRuleException("SLOT_UNAVAILABLE",
                    "This time is no longer available. Please pick another slot.");
        }
        if (bookings.existsOverlapForPet(pet.getId(), BookingStatus.ACTIVE, startAt, startAt.plus(offering.duration()))) {
            throw new BusinessRuleException("PET_ALREADY_BOOKED",
                    "%s already has a booking at this time.".formatted(pet.getName()));
        }
        Booking booking = Booking.request(users.getReferenceById(user.id()), pet, offering, startAt,
                trimToNull(request.notes()), clock.instant());
        try {
            booking = bookings.saveAndFlush(booking);
        } catch (DataIntegrityViolationException exception) {
            if (isOverlapViolation(exception)) {
                throw new ConflictException("SLOT_TAKEN",
                        "Someone booked this time a moment ago. Please pick another slot.");
            }
            throw exception;
        }
        events.publishEvent(BookingStatusChangedEvent.of(booking, Actor.OWNER));
        return responseFor(booking, BookingParty.OWNER);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> list(AuthenticatedUser user, Set<BookingStatus> statuses, BookingListSort sort,
            int page, int size) {
        Collection<BookingStatus> filter =
                statuses == null || statuses.isEmpty() ? EnumSet.allOf(BookingStatus.class) : statuses;
        PageRequest pageable = PageRequest.of(page, size, sort.toSort());
        BookingParty party = partyForRole(user);
        Page<Booking> result = party == BookingParty.OWNER
                ? bookings.findByOwnerIdAndStatusIn(user.id(), filter, pageable)
                : bookings.findByProviderIdAndStatusIn(providerProfiles.requireByUserId(user.id()).getId(), filter,
                        pageable);
        return PageResponse.from(result, booking -> responseFor(booking, party));
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse detail(AuthenticatedUser user, Long bookingId) {
        Participation participation = participation(user, bookingId);
        return new BookingDetailResponse(responseFor(participation.booking(), participation.party()),
                BookingDetailResponse.timelineOf(participation.booking()));
    }

    @Transactional
    public BookingResponse confirm(AuthenticatedUser user, Long bookingId) {
        Participation participation = participation(user, bookingId);
        participation.booking().confirm(participation.party(), user.id(), clock.instant());
        return afterTransition(participation);
    }

    @Transactional
    public BookingResponse decline(AuthenticatedUser user, Long bookingId, String reason) {
        Participation participation = participation(user, bookingId);
        participation.booking().decline(participation.party(), user.id(), trimToNull(reason), clock.instant());
        return afterTransition(participation);
    }

    @Transactional
    public BookingResponse cancel(AuthenticatedUser user, Long bookingId, String reason) {
        Participation participation = participation(user, bookingId);
        participation.booking().cancel(participation.party(), user.id(), trimToNull(reason), clock.instant(),
                ownerCancellationCutoff);
        return afterTransition(participation);
    }

    @Transactional
    public BookingResponse complete(AuthenticatedUser user, Long bookingId) {
        Participation participation = participation(user, bookingId);
        participation.booking().complete(participation.party(), user.id(), clock.instant());
        return afterTransition(participation);
    }

    public int expireOverdueRequests() {
        List<Long> overdue = transactionTemplate.execute(status ->
                bookings.findTop100ByStatusAndStartAtLessThanEqualOrderByStartAtAsc(BookingStatus.REQUESTED,
                                clock.instant())
                        .stream()
                        .map(Booking::getId)
                        .toList());
        int expired = 0;
        for (Long bookingId : overdue == null ? List.<Long>of() : overdue) {
            try {
                Boolean done = transactionTemplate.execute(status -> expireOne(bookingId));
                if (Boolean.TRUE.equals(done)) {
                    expired++;
                }
            } catch (RuntimeException exception) {
                log.warn("Could not expire booking {}: {}", bookingId, exception.getMessage());
            }
        }
        return expired;
    }

    private boolean expireOne(Long bookingId) {
        Booking booking = bookings.findDetailedById(bookingId).orElse(null);
        if (booking == null || booking.getStatus() != BookingStatus.REQUESTED) {
            return false;
        }
        booking.expire(clock.instant());
        events.publishEvent(BookingStatusChangedEvent.of(booking, Actor.SYSTEM));
        return true;
    }

    @Transactional(readOnly = true)
    public Booking requireOwnBooking(Long ownerId, Long bookingId) {
        return bookings.findDetailedById(bookingId)
                .filter(booking -> booking.getOwner().getId().equals(ownerId))
                .orElseThrow(BookingService::notFound);
    }

    private record Participation(Booking booking, BookingParty party) {
    }

    private Participation participation(AuthenticatedUser user, Long bookingId) {
        Booking booking = bookings.findDetailedById(bookingId).orElseThrow(BookingService::notFound);
        BookingParty party = booking.partyOf(user.id()).orElseThrow(BookingService::notFound);
        return new Participation(booking, party);
    }

    private BookingResponse afterTransition(Participation participation) {
        Actor actor = participation.party() == BookingParty.OWNER ? Actor.OWNER : Actor.PROVIDER;
        events.publishEvent(BookingStatusChangedEvent.of(participation.booking(), actor));
        return responseFor(participation.booking(), participation.party());
    }

    private BookingResponse responseFor(Booking booking, BookingParty party) {
        List<BookingAction> actions = booking.availableActions(party, clock.instant(), ownerCancellationCutoff);
        return BookingResponse.of(booking, party, actions, currency);
    }

    private static BookingParty partyForRole(AuthenticatedUser user) {
        return user.is(Role.OWNER) ? BookingParty.OWNER : BookingParty.PROVIDER;
    }

    private static boolean isOverlapViolation(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        return message != null && message.contains(NO_OVERLAP_CONSTRAINT);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("BOOKING_NOT_FOUND", "This booking does not exist.");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
