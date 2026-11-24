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

class AdminVerificationIntegrationTest extends IntegrationTest {

    @Test
    void pendingProfilesAreListedWithContactData() {
        TestUser provider = pendingProvider();
        long profileId = providerProfileId(provider);

        MvcTestResult result = mvc.get().uri("/api/admin/providers").header(AUTHORIZATION, bearer(loginAdmin()))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        String json = body(result);
        List<Integer> ids = JsonPath.read(json, "$[*].id");
        assertThat(ids).contains((int) profileId);
        assertThat(JsonPath.<List<String>>read(json, "$[?(@.id == " + profileId + ")].email"))
                .containsExactly(provider.email());
    }

    @Test
    void approvalMakesTheProfileVerified() {
        TestUser provider = pendingProvider();

        approve(providerProfileId(provider));

        assertThat(mvc.get().uri("/api/provider/profile").header(AUTHORIZATION, bearer(provider)))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.verificationStatus").isEqualTo("VERIFIED");
    }

    @Test
    void rejectionNeedsANoteAndShowsItToTheProvider() {
        TestUser provider = pendingProvider();
        long profileId = providerProfileId(provider);
        TestUser admin = loginAdmin();

        assertThat(mvc.post().uri("/api/admin/providers/{id}/reject", profileId)
                .header(AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\": \"\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.post().uri("/api/admin/providers/{id}/reject", profileId)
                .header(AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\": \"Please describe your qualifications.\"}"))
                .hasStatus(HttpStatus.OK);

        assertThat(mvc.get().uri("/api/provider/profile").header(AUTHORIZATION, bearer(provider)))
                .bodyJson().extractingPath("$.verificationNote").isEqualTo("Please describe your qualifications.");
    }

    @Test
    void decisionsAreOnlyPossibleForPendingProfiles() {
        TestUser provider = pendingProvider();
        long profileId = providerProfileId(provider);
        approve(profileId);

        assertThat(mvc.post().uri("/api/admin/providers/{id}/approve", profileId)
                .header(AUTHORIZATION, bearer(loginAdmin())))
                .hasStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_VERIFICATION_TRANSITION");
    }

    @Test
    void unknownProfileIsNotFound() {
        assertThat(mvc.post().uri("/api/admin/providers/{id}/approve", 999_999)
                .header(AUTHORIZATION, bearer(loginAdmin())))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("$.code").isEqualTo("PROVIDER_NOT_FOUND");
    }

    private TestUser pendingProvider() {
        TestUser provider = registerProvider();
        completeProfile(provider, "Pending Provider", "Sofia", 3);
        addService(provider, "WALKING", "15.00", 60);
        openEveryDay(provider, "09:00", "17:00");
        submitForVerification(provider);
        return provider;
    }
}
