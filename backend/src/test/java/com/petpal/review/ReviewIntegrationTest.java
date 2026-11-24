package com.petpal.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.jayway.jsonpath.JsonPath;
import com.petpal.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class ReviewIntegrationTest extends IntegrationTest {

    @Test
    void reviewsUpdateTheRatingAndArePublic() {
        String city = uniqueCity();
        VerifiedProvider provider = verifiedProvider(city, "GROOMING", "45.00", 60, 9);
        TestUser owner = registerOwner();
        long petId = createPet(owner, "Max");
        long first = completedBooking(provider, owner, petId, 3);
        long second = completedBooking(provider, owner, petId, 4);

        assertThat(review(owner, first, 5, "Max came back calm and fluffy.")).hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.authorName").isEqualTo("Test O.");
        assertThat(review(owner, second, 4, "Good, a bit late.")).hasStatus(HttpStatus.CREATED);

        MvcTestResult page = mvc.get().uri("/api/providers/{id}", provider.profileId()).exchange();
        assertThat(JsonPath.<Double>read(body(page), "$.ratingAverage")).isEqualTo(4.5);
        assertThat(JsonPath.<Integer>read(body(page), "$.reviewCount")).isEqualTo(2);

        MvcTestResult reviews = mvc.get().uri("/api/providers/{id}/reviews", provider.profileId()).exchange();
        assertThat(JsonPath.<List<Integer>>read(body(reviews), "$.content[*].rating")).containsExactly(4, 5);
        assertThat(body(reviews)).doesNotContain(owner.email());

        assertThat(JsonPath.<List<Integer>>read(body(mvc.get()
                .uri("/api/providers?city=" + city + "&minRating=4.6").exchange()), "$.content[*].id")).isEmpty();
        assertThat(JsonPath.<List<Integer>>read(body(mvc.get()
                .uri("/api/providers?city=" + city + "&minRating=4.5").exchange()), "$.content[*].id"))
                .containsExactly((int) provider.profileId());
    }

    @Test
    void providerIsNotifiedOfANewReview() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        long bookingId = completedBooking(provider, owner, createPet(owner, "Rex"), 3);

        review(owner, bookingId, 5, "Great walk.");

        MvcTestResult inbox = mvc.get().uri("/api/notifications").header(AUTHORIZATION, bearer(provider.user()))
                .exchange();
        assertThat(JsonPath.<String>read(body(inbox), "$.content[0].message")).isEqualTo("Test O. left a 5-star review.");
        assertThat(JsonPath.<String>read(body(inbox), "$.content[0].type")).isEqualTo("REVIEW_RECEIVED");
    }

    @Test
    void onlyCompletedBookingsCanBeReviewedAndOnlyOnce() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        long petId = createPet(owner, "Rex");
        long open = requestBooking(owner, petId, provider.offeringId(), at(inDays(5), "10:00"));
        long done = completedBooking(provider, owner, petId, 3);

        assertThat(review(owner, open, 5, "Too early"))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("BOOKING_NOT_COMPLETED");
        assertThat(review(owner, done, 5, "Great")).hasStatus(HttpStatus.CREATED);
        assertThat(review(owner, done, 1, "Changed my mind"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("ALREADY_REVIEWED");
    }

    @Test
    void onlyTheOwnerOfTheBookingCanReview() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        TestUser owner = registerOwner();
        TestUser stranger = registerOwner();
        long bookingId = completedBooking(provider, owner, createPet(owner, "Rex"), 3);

        assertThat(review(stranger, bookingId, 5, "Fake"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("BOOKING_NOT_FOUND");
        assertThat(review(provider.user(), bookingId, 5, "Self praise")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(review(owner, bookingId, 6, "Too good")).hasStatus(HttpStatus.BAD_REQUEST);
    }
}
