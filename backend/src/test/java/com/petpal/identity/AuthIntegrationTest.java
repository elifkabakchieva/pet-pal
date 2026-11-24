package com.petpal.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.jayway.jsonpath.JsonPath;
import com.petpal.support.IntegrationTest;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

class AuthIntegrationTest extends IntegrationTest {

    @Test
    void registeringAnOwnerReturnsTokenAndUser() {
        String email = "Maria." + UUID.randomUUID() + "@Example.com";

        MvcTestResult result = mvc.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "correct-horse", "firstName": " Maria ", "lastName": "Kirova",
                         "phone": "", "city": "Sofia", "accountType": "OWNER"}
                        """.formatted(email))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        String json = body(result);
        assertThat(JsonPath.<String>read(json, "$.accessToken")).isNotBlank();
        assertThat(JsonPath.<String>read(json, "$.expiresAt")).isNotBlank();
        assertThat(JsonPath.<String>read(json, "$.user.email")).isEqualTo(email.toLowerCase(Locale.ROOT));
        assertThat(JsonPath.<String>read(json, "$.user.firstName")).isEqualTo("Maria");
        assertThat(JsonPath.<Object>read(json, "$.user.phone")).isNull();
        assertThat(JsonPath.<String>read(json, "$.user.role")).isEqualTo("OWNER");
    }

    @Test
    void registeringAnExistingEmailIgnoringCaseIsAConflict() {
        TestUser existing = registerOwner();

        assertThat(mvc.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "%s", "password": "correct-horse", "firstName": "A", "lastName": "B",
                 "city": "Sofia", "accountType": "PROVIDER"}
                """.formatted(existing.email().toUpperCase())))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void invalidRegistrationListsEveryInvalidField() {
        MvcTestResult result = mvc.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "not-an-email", "password": "short", "firstName": "", "lastName": "B",
                         "city": "Sofia", "accountType": "OWNER"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        String json = body(result);
        assertThat(JsonPath.<String>read(json, "$.code")).isEqualTo("VALIDATION_FAILED");
        assertThat(JsonPath.<List<String>>read(json, "$.errors[*].field"))
                .contains("email", "password", "firstName");
    }

    @Test
    void adminCannotBeSelfRegistered() {
        assertThat(mvc.post().uri("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email": "sneaky@test.petpal", "password": "correct-horse", "firstName": "A", "lastName": "B",
                 "city": "Sofia", "accountType": "ADMIN"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    void loginWithCorrectCredentialsReturnsToken() {
        TestUser owner = registerOwner();

        MvcTestResult result = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(owner.email(), PASSWORD))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(longAt(body(result), "$.user.id")).isEqualTo(owner.id());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() {
        TestUser owner = registerOwner();

        MvcTestResult wrongPassword = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "wrong-password"}
                        """.formatted(owner.email()))
                .exchange();
        MvcTestResult unknownEmail = mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "nobody-%s@test.petpal", "password": "wrong-password"}
                        """.formatted(UUID.randomUUID()))
                .exchange();

        assertThat(wrongPassword).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail).hasStatus(HttpStatus.UNAUTHORIZED);
        for (String path : List.of("$.code", "$.detail", "$.status")) {
            Object expected = JsonPath.read(body(wrongPassword), path);
            assertThat(JsonPath.<Object>read(body(unknownEmail), path)).isEqualTo(expected);
        }
        assertThat(JsonPath.<String>read(body(wrongPassword), "$.code")).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void currentUserIsReturnedForAValidToken() {
        TestUser provider = registerProvider();

        assertThat(mvc.get().uri("/api/users/me").header(AUTHORIZATION, bearer(provider)))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.role").isEqualTo("PROVIDER");
    }

    @Test
    void currentUserRequiresAToken() {
        assertThat(mvc.get().uri("/api/users/me")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/users/me").header(AUTHORIZATION, "Bearer not-a-token"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminAccountExistsAfterStartup() {
        TestUser admin = loginAdmin();

        assertThat(mvc.get().uri("/api/users/me").header(AUTHORIZATION, bearer(admin)))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.role").isEqualTo("ADMIN");
    }
}
