package com.petpal.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.petpal.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class DiscoveryIntegrationTest extends IntegrationTest {

    @Test
    void searchShowsOnlyVerifiedProvidersOfTheCity() {
        String city = uniqueCity();
        VerifiedProvider groomer = verifiedProvider(city, "GROOMING", "45.00", 90, 9);
        VerifiedProvider walker = verifiedProvider(city, "WALKING", "15.00", 60, 3);
        TestUser unverified = registerProvider();
        completeProfile(unverified, "Not yet verified", city, 5);

        List<Integer> ids = idsOf(search("city=" + city.toLowerCase()));

        assertThat(ids).containsExactlyInAnyOrder((int) groomer.profileId(), (int) walker.profileId());
    }

    @Test
    void categoryAndPriceFiltersLookAtActiveServices() {
        String city = uniqueCity();
        VerifiedProvider groomer = verifiedProvider(city, "GROOMING", "45.00", 90, 9);
        VerifiedProvider walker = verifiedProvider(city, "WALKING", "15.00", 60, 3);

        assertThat(idsOf(search("city=" + city + "&category=GROOMING"))).containsExactly((int) groomer.profileId());
        assertThat(idsOf(search("city=" + city + "&maxPrice=20"))).containsExactly((int) walker.profileId());
        assertThat(idsOf(search("city=" + city + "&category=GROOMING&maxPrice=20"))).isEmpty();
    }

    @Test
    void textSearchMatchesNameAndBioAndEscapesWildcards() {
        String city = uniqueCity();
        VerifiedProvider provider = verifiedProvider(city, "TRAINING", "30.00", 60, 4);

        assertThat(idsOf(search("city=" + city + "&q=PUNCTUAL"))).containsExactly((int) provider.profileId());
        assertThat(idsOf(search("city=" + city + "&q=%25"))).isEmpty();
    }

    @Test
    void resultsCanBeSortedByExperienceAndArePaged() {
        String city = uniqueCity();
        VerifiedProvider junior = verifiedProvider(city, "DAYCARE", "25.00", 240, 2);
        VerifiedProvider senior = verifiedProvider(city, "DAYCARE", "35.00", 240, 12);

        String json = body(search("city=" + city + "&sort=EXPERIENCE&size=1"));

        assertThat(JsonPath.<List<Integer>>read(json, "$.content[*].id")).containsExactly((int) senior.profileId());
        assertThat(JsonPath.<Integer>read(json, "$.totalElements")).isEqualTo(2);
        assertThat(JsonPath.<Integer>read(json, "$.totalPages")).isEqualTo(2);
        assertThat(idsOf(search("city=" + city + "&sort=EXPERIENCE&size=1&page=1")))
                .containsExactly((int) junior.profileId());
    }

    @Test
    void summariesContainCategoriesAndStartingPrice() {
        String city = uniqueCity();
        VerifiedProvider provider = verifiedProvider(city, "WALKING", "15.00", 60, 3);
        addService(provider.user(), "DAYCARE", "28.50", 240);

        String json = body(search("city=" + city));

        assertThat(JsonPath.<List<String>>read(json, "$.content[0].categories")).containsExactly("DAYCARE", "WALKING");
        assertThat(JsonPath.<Double>read(json, "$.content[0].startingPrice")).isEqualTo(15.0);
    }

    @Test
    void invalidParametersAreRejected() {
        assertThat(mvc.get().uri("/api/providers?size=500"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("VALIDATION_FAILED");
        assertThat(mvc.get().uri("/api/providers?sort=PRICE"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_PARAMETER");
    }

    @Test
    void providerPageShowsServicesAndHoursButOnlyForVerifiedProviders() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "GROOMING", "45.00", 90, 9);
        TestUser draft = registerProvider();

        MvcTestResult page = mvc.get().uri("/api/providers/{id}", provider.profileId()).exchange();

        assertThat(page).hasStatus(HttpStatus.OK);
        String json = body(page);
        assertThat(JsonPath.<List<Integer>>read(json, "$.services[*].id")).containsExactly((int) provider.offeringId());
        assertThat(JsonPath.<List<Object>>read(json, "$.weeklyHours")).hasSize(7);
        assertThat(JsonPath.<String>read(json, "$.currency")).isEqualTo("EUR");
        assertThat(json).doesNotContain(provider.user().email());
        assertThat(mvc.get().uri("/api/providers/{id}", providerProfileId(draft)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("PROVIDER_NOT_FOUND");
    }

    @Test
    void slotsFollowTheWeeklyHoursAndTheLeadTime() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        LocalDate date = inDays(3);

        List<String> allDay = slotStarts(provider, date);
        clock.setInstant(at(date, "09:10"));
        List<String> afterLeadTime = slotStarts(provider, date);

        assertThat(allDay).hasSize(19).startsWith(at(date, "08:00").toString()).endsWith(at(date, "17:00").toString());
        assertThat(afterLeadTime.getFirst()).isEqualTo(at(date, "11:30").toString());
    }

    @Test
    void slotsOutsideTheHorizonOrForAnotherProvidersServiceAreNotOffered() {
        VerifiedProvider provider = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);
        VerifiedProvider other = verifiedProvider(uniqueCity(), "WALKING", "15.00", 60, 3);

        assertThat(slotStarts(provider, inDays(-1))).isEmpty();
        assertThat(slotStarts(provider, inDays(61))).isEmpty();
        assertThat(mvc.get().uri("/api/providers/{id}/slots?offeringId={offering}&date={date}",
                provider.profileId(), other.offeringId(), inDays(2)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("OFFERING_NOT_FOUND");
    }

    private MvcTestResult search(String query) {
        MvcTestResult result = mvc.get().uri("/api/providers?" + query).exchange();
        assertThat(result).hasStatus(HttpStatus.OK);
        return result;
    }

    private static List<Integer> idsOf(MvcTestResult result) {
        return JsonPath.read(body(result), "$.content[*].id");
    }

    private List<String> slotStarts(VerifiedProvider provider, LocalDate date) {
        MvcTestResult result = mvc.get().uri("/api/providers/{id}/slots?offeringId={offering}&date={date}",
                provider.profileId(), provider.offeringId(), date).exchange();
        assertThat(result).hasStatus(HttpStatus.OK);
        return JsonPath.read(body(result), "$[*].startAt");
    }
}
