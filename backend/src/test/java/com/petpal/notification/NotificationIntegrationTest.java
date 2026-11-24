package com.petpal.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.jayway.jsonpath.JsonPath;
import com.petpal.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class NotificationIntegrationTest extends IntegrationTest {

    @Test
    void bookingChangesNotifyTheOtherParty() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "GROOMING", "45.00", 90, 9);
        TestUser owner = registerOwner();
        long bookingId = requestBooking(owner, createPet(owner, "Max"), provider.offeringId(), at(inDays(3), "10:00"));

        String providerInbox = inbox(provider.user());
        assertThat(JsonPath.<List<String>>read(providerInbox, "$.content[*].type"))
                .containsExactly("BOOKING_REQUESTED", "PROVIDER_VERIFIED");
        assertThat(JsonPath.<String>read(providerInbox, "$.content[0].link")).isEqualTo("/bookings/" + bookingId);
        assertThat(JsonPath.<String>read(providerInbox, "$.content[0].message"))
                .startsWith("New request from Test O.: GROOMING service for Max on ");
        assertThat(unreadCount(owner)).isZero();

        bookingAction(provider.user(), bookingId, "confirm");

        assertThat(JsonPath.<List<String>>read(inbox(owner), "$.content[*].type")).containsExactly("BOOKING_CONFIRMED");
        assertThat(unreadCount(owner)).isEqualTo(1);
    }

    @Test
    void notificationsCanBeMarkedReadOneByOneOrAllAtOnce() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        requestBooking(owner, createPet(owner, "Rex"), provider.offeringId(), at(inDays(3), "10:00"));
        long firstId = longAt(inbox(provider.user()), "$.content[0].id");

        assertThat(mvc.post().uri("/api/notifications/{id}/read", firstId).header(AUTHORIZATION, bearer(owner)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.post().uri("/api/notifications/{id}/read", firstId)
                .header(AUTHORIZATION, bearer(provider.user())))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(unreadCount(provider.user())).isEqualTo(1);

        assertThat(mvc.post().uri("/api/notifications/read-all").header(AUTHORIZATION, bearer(provider.user())))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(unreadCount(provider.user())).isZero();
    }

    @Test
    void rejectionNoteIsPartOfTheNotification() {
        TestUser provider = registerProvider();
        completeProfile(provider, "Rejected Provider", "Sofia", 1);
        addService(provider, "WALKING", "15.00", 60);
        openEveryDay(provider, "09:00", "17:00");
        submitForVerification(provider);

        mvc.post().uri("/api/admin/providers/{id}/reject", providerProfileId(provider))
                .header(AUTHORIZATION, bearer(loginAdmin()))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"note\": \"Please describe your qualifications.\"}")
                .exchange();

        assertThat(JsonPath.<String>read(inbox(provider), "$.content[0].message"))
                .isEqualTo("Your profile was not verified: Please describe your qualifications.");
    }

    private String inbox(TestUser user) {
        MvcTestResult result = mvc.get().uri("/api/notifications").header(AUTHORIZATION, bearer(user)).exchange();
        assertThat(result).hasStatus(HttpStatus.OK);
        return body(result);
    }

    private long unreadCount(TestUser user) {
        MvcTestResult result = mvc.get().uri("/api/notifications/unread-count").header(AUTHORIZATION, bearer(user))
                .exchange();
        return longAt(body(result), "$.count");
    }
}
