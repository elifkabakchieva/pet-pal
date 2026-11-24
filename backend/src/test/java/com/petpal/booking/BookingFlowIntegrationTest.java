package com.petpal.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.jayway.jsonpath.JsonPath;
import com.petpal.support.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class BookingFlowIntegrationTest extends IntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void ownerRequestsAndProviderConfirms() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "GROOMING", "45.00", 90, 9);
        TestUser owner = registerOwner();
        long petId = createPet(owner, "Max");
        Instant start = at(inDays(3), "10:00");

        MvcTestResult created = requestBookingResult(owner, petId, provider.offeringId(), start);

        assertThat(created).hasStatus(HttpStatus.CREATED);
        String json = body(created);
        long bookingId = longAt(json, "$.id");
        assertThat(JsonPath.<String>read(json, "$.status")).isEqualTo("REQUESTED");
        assertThat(JsonPath.<String>read(json, "$.endAt")).isEqualTo(start.plus(Duration.ofMinutes(90)).toString());
        assertThat(JsonPath.<List<String>>read(json, "$.actions")).containsExactly("CANCEL");
        assertThat(JsonPath.<Object>read(json, "$.provider.phone")).isNull();

        MvcTestResult providerView = mvc.get().uri("/api/bookings?status=REQUESTED&sort=UPCOMING")
                .header(AUTHORIZATION, bearer(provider.user())).exchange();
        assertThat(JsonPath.<List<Integer>>read(body(providerView), "$.content[*].id")).containsExactly((int) bookingId);
        assertThat(JsonPath.<List<String>>read(body(providerView), "$.content[0].actions"))
                .containsExactly("CONFIRM", "DECLINE");

        assertThat(bookingAction(provider.user(), bookingId, "confirm"))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.status").isEqualTo("CONFIRMED");

        MvcTestResult detail = mvc.get().uri("/api/bookings/{id}", bookingId).header(AUTHORIZATION, bearer(owner))
                .exchange();
        String detailJson = body(detail);
        assertThat(JsonPath.<List<String>>read(detailJson, "$.timeline[*].toStatus"))
                .containsExactly("REQUESTED", "CONFIRMED");
        assertThat(JsonPath.<List<String>>read(detailJson, "$.timeline[*].actorName")).first().isEqualTo("Test O.");
        assertThat(JsonPath.<String>read(detailJson, "$.booking.provider.phone")).isEqualTo("+359 888 000 000");
    }

    @Test
    void requestedTimeMustBeOneOfTheFreeSlots() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        long petId = createPet(owner, "Rex");

        assertThat(requestBookingResult(owner, petId, provider.offeringId(), at(inDays(3), "10:07")))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("SLOT_UNAVAILABLE");
        assertThat(requestBookingResult(owner, petId, provider.offeringId(), at(inDays(3), "20:00")))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("SLOT_UNAVAILABLE");
    }

    @Test
    void bookedTimeIsNoLongerOfferedToOthers() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser first = registerOwner();
        TestUser second = registerOwner();
        Instant start = at(inDays(4), "10:00");
        requestBooking(first, createPet(first, "Rex"), provider.offeringId(), start);

        MvcTestResult slots = mvc.get().uri("/api/providers/{id}/slots?offeringId={o}&date={d}",
                provider.profileId(), provider.offeringId(), inDays(4)).exchange();
        List<String> starts = JsonPath.read(body(slots), "$[*].startAt");

        assertThat(starts).doesNotContain(start.toString(), start.minus(Duration.ofMinutes(30)).toString())
                .contains(start.plus(Duration.ofMinutes(60)).toString());
        assertThat(requestBookingResult(second, createPet(second, "Bella"), provider.offeringId(), start))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("SLOT_UNAVAILABLE");
    }

    @Test
    void databaseRejectsOverlappingActiveBookingsOfOneProvider() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        long petId = createPet(owner, "Rex");
        Instant start = at(inDays(5), "10:00");
        requestBooking(owner, petId, provider.offeringId(), start);

        assertThatThrownBy(() -> jdbc.sql("""
                        insert into bookings (owner_id, pet_id, provider_id, offering_id, start_at, end_at, status, price)
                        values (:owner, :pet, :provider, :offering, :start, :end, 'REQUESTED', 15.00)
                        """)
                .param("owner", owner.id())
                .param("pet", petId)
                .param("provider", provider.profileId())
                .param("offering", provider.offeringId())
                .param("start", OffsetDateTime.ofInstant(start.plus(Duration.ofMinutes(30)), ZoneOffset.UTC))
                .param("end", OffsetDateTime.ofInstant(start.plus(Duration.ofMinutes(90)), ZoneOffset.UTC))
                .update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ex_bookings_provider_no_overlap");
    }

    @Test
    void aPetCannotBeInTwoPlacesAtOnce() {
        VerifiedProvider groomer = verifiedProvider(uniqueCity(), "GROOMING", "45.00", 60, 9);
        VerifiedProvider walker = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        long petId = createPet(owner, "Max");
        Instant start = at(inDays(3), "11:00");
        requestBooking(owner, petId, groomer.offeringId(), start);

        assertThat(requestBookingResult(owner, petId, walker.offeringId(), start.plus(Duration.ofMinutes(30))))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("PET_ALREADY_BOOKED");
    }

    @Test
    void archivedPetsAndStoppedServicesCannotBeBooked() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        long petId = createPet(owner, "Old Rex");
        mvc.delete().uri("/api/pets/{id}", petId).header(AUTHORIZATION, bearer(owner)).exchange();
        long otherPet = createPet(owner, "Bella");
        mvc.delete().uri("/api/provider/services/{id}", provider.offeringId())
                .header(AUTHORIZATION, bearer(provider.user())).exchange();

        assertThat(requestBookingResult(owner, petId, provider.offeringId(), at(inDays(3), "10:00")))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("PET_NOT_FOUND");
        assertThat(requestBookingResult(owner, otherPet, provider.offeringId(), at(inDays(3), "10:00")))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("OFFERING_NOT_FOUND");
    }

    @Test
    void strangersSeeNothingAndOwnersCannotActForProviders() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        TestUser stranger = registerOwner();
        long bookingId = requestBooking(owner, createPet(owner, "Rex"), provider.offeringId(), at(inDays(3), "10:00"));

        assertThat(mvc.get().uri("/api/bookings/{id}", bookingId).header(AUTHORIZATION, bearer(stranger)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("BOOKING_NOT_FOUND");
        assertThat(bookingAction(owner, bookingId, "confirm"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.code").isEqualTo("NOT_ALLOWED_FOR_PARTY");
        assertThat(mvc.post().uri("/api/bookings").header(AUTHORIZATION, bearer(provider.user()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"offeringId\": 1, \"petId\": 1, \"startAt\": \"2030-01-01T10:00:00Z\"}"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void ownersCannotCancelConfirmedBookingsShortlyBeforeTheStartButProvidersCan() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        Instant start = at(inDays(3), "10:00");
        long bookingId = requestBooking(owner, createPet(owner, "Rex"), provider.offeringId(), start);
        bookingAction(provider.user(), bookingId, "confirm");

        clock.setInstant(start.minus(Duration.ofHours(11)));

        assertThat(bookingAction(owner, bookingId, "cancel"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("CANCELLATION_WINDOW_CLOSED");
        assertThat(mvc.post().uri("/api/bookings/{id}/cancel", bookingId)
                .header(AUTHORIZATION, bearer(provider.user()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"Ill today\"}"))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.status").isEqualTo("CANCELLED");
    }

    @Test
    void providerCompletesOnceTheBookingHasStarted() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        Instant start = at(inDays(3), "10:00");
        long bookingId = requestBooking(owner, createPet(owner, "Rex"), provider.offeringId(), start);
        bookingAction(provider.user(), bookingId, "confirm");

        assertThat(bookingAction(provider.user(), bookingId, "complete"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("BOOKING_NOT_STARTED");

        clock.setInstant(start.plus(Duration.ofMinutes(30)));

        assertThat(bookingAction(provider.user(), bookingId, "complete"))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.status").isEqualTo("COMPLETED");
        assertThat(mvc.get().uri("/api/bookings/{id}", bookingId).header(AUTHORIZATION, bearer(owner)))
                .bodyJson().extractingPath("$.booking.actions[0]").isEqualTo("REVIEW");
    }

    @Test
    void unansweredRequestsExpireAtTheirStartTime() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        Instant start = at(inDays(3), "10:00");
        long bookingId = requestBooking(owner, createPet(owner, "Rex"), provider.offeringId(), start);

        clock.setInstant(start.plusSeconds(60));
        int expired = bookingService.expireOverdueRequests();

        assertThat(expired).isGreaterThanOrEqualTo(1);
        MvcTestResult detail = mvc.get().uri("/api/bookings/{id}", bookingId).header(AUTHORIZATION, bearer(owner))
                .exchange();
        String json = body(detail);
        assertThat(JsonPath.<String>read(json, "$.booking.status")).isEqualTo("CANCELLED");
        assertThat(JsonPath.<List<String>>read(json, "$.timeline[*].actorName")).last().isEqualTo("PetPal");
    }

    @Test
    void listCanBeFilteredByStatus() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        long petId = createPet(owner, "Rex");
        long first = requestBooking(owner, petId, provider.offeringId(), at(inDays(3), "09:00"));
        long second = requestBooking(owner, petId, provider.offeringId(), at(inDays(4), "09:00"));
        bookingAction(provider.user(), second, "decline");

        MvcTestResult requested = mvc.get().uri("/api/bookings?status=REQUESTED")
                .header(AUTHORIZATION, bearer(owner)).exchange();
        MvcTestResult all = mvc.get().uri("/api/bookings?sort=UPCOMING").header(AUTHORIZATION, bearer(owner)).exchange();

        assertThat(JsonPath.<List<Integer>>read(body(requested), "$.content[*].id")).containsExactly((int) first);
        assertThat(JsonPath.<List<Integer>>read(body(all), "$.content[*].id")).containsExactly((int) first, (int) second);
    }
}
