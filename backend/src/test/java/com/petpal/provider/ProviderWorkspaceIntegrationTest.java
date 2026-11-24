package com.petpal.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.jayway.jsonpath.JsonPath;
import com.petpal.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class ProviderWorkspaceIntegrationTest extends IntegrationTest {

    @Test
    void registeringAsProviderCreatesADraftProfile() {
        TestUser provider = registerProvider();

        MvcTestResult result = mvc.get().uri("/api/provider/profile").header(AUTHORIZATION, bearer(provider))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        String json = body(result);
        assertThat(JsonPath.<String>read(json, "$.verificationStatus")).isEqualTo("DRAFT");
        assertThat(JsonPath.<String>read(json, "$.displayName")).isEqualTo("Test Provider");
        assertThat(JsonPath.<String>read(json, "$.city")).isEqualTo("Sofia");
    }

    @Test
    void submittingAnIncompleteProfileExplainsWhatIsMissing() {
        TestUser provider = registerProvider();

        assertThat(mvc.post().uri("/api/provider/profile/submit").header(AUTHORIZATION, bearer(provider)))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("PROFILE_INCOMPLETE");
    }

    @Test
    void completeProfileCanBeSubmittedOnce() {
        TestUser provider = registerProvider();
        completeProfile(provider, "Georgi's Grooming", "Sofia", 9);
        addService(provider, "GROOMING", "45.00", 90);
        openEveryDay(provider, "09:00", "17:00");

        assertThat(mvc.post().uri("/api/provider/profile/submit").header(AUTHORIZATION, bearer(provider)))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.verificationStatus").isEqualTo("PENDING");
        assertThat(mvc.post().uri("/api/provider/profile/submit").header(AUTHORIZATION, bearer(provider)))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_VERIFICATION_TRANSITION");
    }

    @Test
    void servicesCanBeCreatedUpdatedAndDeactivated() {
        TestUser provider = registerProvider();
        long walkId = addService(provider, "WALKING", "15.00", 60);
        long groomId = addService(provider, "GROOMING", "40.00", 90);

        assertThat(mvc.put().uri("/api/provider/services/{id}", groomId)
                .header(AUTHORIZATION, bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryCode": "GROOMING", "title": "Full groom", "price": 45.00, "durationMinutes": 120}
                        """))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.durationMinutes").isEqualTo(120);
        assertThat(mvc.delete().uri("/api/provider/services/{id}", walkId).header(AUTHORIZATION, bearer(provider)))
                .hasStatus(HttpStatus.NO_CONTENT);

        MvcTestResult list = mvc.get().uri("/api/provider/services").header(AUTHORIZATION, bearer(provider))
                .exchange();
        String json = body(list);
        assertThat(JsonPath.<List<String>>read(json, "$[*].title")).containsExactly("Full groom", "WALKING service");
        assertThat(JsonPath.<List<Boolean>>read(json, "$[*].active")).containsExactly(true, false);
    }

    @Test
    void serviceDurationMustBeAQuarterHourMultiple() {
        TestUser provider = registerProvider();

        MvcTestResult result = mvc.post().uri("/api/provider/services")
                .header(AUTHORIZATION, bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryCode": "TRAINING", "title": "Odd session", "price": 20.00, "durationMinutes": 50}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(JsonPath.<List<String>>read(body(result), "$.errors[*].field")).contains("durationOnQuarterHour");
    }

    @Test
    void providerCannotChangeAnotherProvidersService() {
        TestUser alice = registerProvider();
        TestUser bob = registerProvider();
        long alicesService = addService(alice, "DAYCARE", "30.00", 240);

        assertThat(mvc.delete().uri("/api/provider/services/{id}", alicesService).header(AUTHORIZATION, bearer(bob)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("OFFERING_NOT_FOUND");
    }

    @Test
    void weeklyHoursAreReplacedAsAWholeAndTouchingWindowsAreAllowed() {
        TestUser provider = registerProvider();
        openEveryDay(provider, "08:00", "18:00");

        MvcTestResult result = mvc.put().uri("/api/provider/availability")
                .header(AUTHORIZATION, bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"windows": [
                          {"dayOfWeek": "TUESDAY", "startTime": "14:00", "endTime": "18:00"},
                          {"dayOfWeek": "TUESDAY", "startTime": "09:00", "endTime": "14:00"}
                        ]}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        MvcTestResult stored = mvc.get().uri("/api/provider/availability").header(AUTHORIZATION, bearer(provider))
                .exchange();
        assertThat(JsonPath.<List<String>>read(body(stored), "$[*].startTime")).containsExactly("09:00:00", "14:00:00");
    }

    @Test
    void overlappingOrOddWindowsAreRejected() {
        TestUser provider = registerProvider();

        assertThat(mvc.put().uri("/api/provider/availability")
                .header(AUTHORIZATION, bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"windows": [
                          {"dayOfWeek": "MONDAY", "startTime": "09:00", "endTime": "13:00"},
                          {"dayOfWeek": "MONDAY", "startTime": "12:00", "endTime": "16:00"}
                        ]}
                        """))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("OVERLAPPING_AVAILABILITY");
        assertThat(mvc.put().uri("/api/provider/availability")
                .header(AUTHORIZATION, bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"windows": [{"dayOfWeek": "MONDAY", "startTime": "09:10", "endTime": "13:00"}]}
                        """))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_AVAILABILITY_WINDOW");
    }

    @Test
    void categoriesArePublicAndOrdered() {
        MvcTestResult result = mvc.get().uri("/api/categories").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(JsonPath.<List<String>>read(body(result), "$[*].code"))
                .containsExactly("DAYCARE", "GROOMING", "VETERINARY", "WALKING", "TRAINING");
    }
}
