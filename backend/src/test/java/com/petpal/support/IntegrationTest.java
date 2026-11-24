package com.petpal.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestDatabaseConfiguration.class)
public abstract class IntegrationTest {

    protected static final String PASSWORD = "test-password-1";
    protected static final String ADMIN_EMAIL = "admin@test.petpal";
    protected static final String ADMIN_PASSWORD = "admin-test-password";

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.reset();
    }

    protected record TestUser(long id, String email, String token) {
    }

    protected TestUser registerOwner() {
        return register("OWNER");
    }

    protected TestUser registerProvider() {
        return register("PROVIDER");
    }

    protected TestUser register(String accountType) {
        String email = accountType.toLowerCase(Locale.ROOT) + "-" + UUID.randomUUID() + "@test.petpal";
        String lastName = accountType.charAt(0) + accountType.substring(1).toLowerCase(Locale.ROOT);
        MvcTestResult result = mvc.post().uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "firstName": "Test", "lastName": "%s",
                         "phone": "+359 888 000 000", "city": "Sofia", "accountType": "%s"}
                        """.formatted(email, PASSWORD, lastName, accountType))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        String body = body(result);
        return new TestUser(longAt(body, "$.user.id"), email, JsonPath.read(body, "$.accessToken"));
    }

    protected TestUser loginAdmin() {
        MvcTestResult result = mvc.post().uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(ADMIN_EMAIL, ADMIN_PASSWORD))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.OK);
        String body = body(result);
        return new TestUser(longAt(body, "$.user.id"), ADMIN_EMAIL, JsonPath.read(body, "$.accessToken"));
    }

    protected long createPet(TestUser owner, String name) {
        MvcTestResult result = mvc.post().uri("/api/pets")
                .header(AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "species": "DOG", "breed": "Mixed", "birthDate": "2021-04-01",
                         "weightKg": 12.5, "careNotes": "Friendly."}
                        """.formatted(name))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return longAt(body(result), "$.id");
    }

    protected long providerProfileId(TestUser provider) {
        MvcTestResult result = mvc.get().uri("/api/provider/profile").header(AUTHORIZATION, bearer(provider)).exchange();
        assertThat(result).hasStatus(HttpStatus.OK);
        return longAt(body(result), "$.id");
    }

    protected void completeProfile(TestUser provider, String displayName, String city, int yearsExperience) {
        assertThat(mvc.put().uri("/api/provider/profile")
                .header(AUTHORIZATION, bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"displayName": "%s", "city": "%s", "address": "Main street 1", "yearsExperience": %d,
                         "bio": "Experienced and calm with animals of every size. Certified, insured and punctual."}
                        """.formatted(displayName, city, yearsExperience)))
                .hasStatus(HttpStatus.OK);
    }

    protected long addService(TestUser provider, String categoryCode, String price, int durationMinutes) {
        MvcTestResult result = mvc.post().uri("/api/provider/services")
                .header(AUTHORIZATION, bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"categoryCode": "%s", "title": "%s service", "description": "Good care.",
                         "price": %s, "durationMinutes": %d}
                        """.formatted(categoryCode, categoryCode, price, durationMinutes))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return longAt(body(result), "$.id");
    }

    protected void openEveryDay(TestUser provider, String from, String to) {
        String windows = java.util.Arrays.stream(java.time.DayOfWeek.values())
                .map(day -> """
                        {"dayOfWeek": "%s", "startTime": "%s", "endTime": "%s"}""".formatted(day, from, to))
                .collect(java.util.stream.Collectors.joining(", "));
        assertThat(mvc.put().uri("/api/provider/availability")
                .header(AUTHORIZATION, bearer(provider))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"windows\": [" + windows + "]}"))
                .hasStatus(HttpStatus.OK);
    }

    protected void submitForVerification(TestUser provider) {
        assertThat(mvc.post().uri("/api/provider/profile/submit").header(AUTHORIZATION, bearer(provider)))
                .hasStatus(HttpStatus.OK);
    }

    protected void approve(long profileId) {
        assertThat(mvc.post().uri("/api/admin/providers/{id}/approve", profileId)
                .header(AUTHORIZATION, bearer(loginAdmin())))
                .hasStatus(HttpStatus.OK);
    }

    protected record VerifiedProvider(TestUser user, long profileId, long offeringId) {
    }

    protected VerifiedProvider verifiedProvider(String city, String categoryCode, String price, int durationMinutes,
            int yearsExperience) {
        TestUser user = registerProvider();
        completeProfile(user, "Provider " + UUID.randomUUID().toString().substring(0, 8), city, yearsExperience);
        long offeringId = addService(user, categoryCode, price, durationMinutes);
        openEveryDay(user, "08:00", "18:00");
        submitForVerification(user);
        long profileId = providerProfileId(user);
        approve(profileId);
        return new VerifiedProvider(user, profileId, offeringId);
    }

    protected static String uniqueCity() {
        return "City-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Sofia");

    protected LocalDate inDays(int days) {
        return LocalDate.now(clock.withZone(BUSINESS_ZONE)).plusDays(days);
    }

    protected static Instant at(LocalDate date, String localTime) {
        return date.atTime(LocalTime.parse(localTime)).atZone(BUSINESS_ZONE).toInstant();
    }

    protected MvcTestResult requestBookingResult(TestUser owner, long petId, long offeringId, Instant startAt) {
        return mvc.post().uri("/api/bookings")
                .header(AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"offeringId": %d, "petId": %d, "startAt": "%s", "notes": "Please be gentle."}
                        """.formatted(offeringId, petId, startAt))
                .exchange();
    }

    protected long requestBooking(TestUser owner, long petId, long offeringId, Instant startAt) {
        MvcTestResult result = requestBookingResult(owner, petId, offeringId, startAt);
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return longAt(body(result), "$.id");
    }

    protected MvcTestResult bookingAction(TestUser user, long bookingId, String action) {
        return mvc.post().uri("/api/bookings/{id}/{action}", bookingId, action)
                .header(AUTHORIZATION, bearer(user))
                .exchange();
    }

    protected long completedBooking(VerifiedProvider provider, TestUser owner, long petId, int daysAhead) {
        Instant start = at(inDays(daysAhead), "10:00");
        long bookingId = requestBooking(owner, petId, provider.offeringId(), start);
        assertThat(bookingAction(provider.user(), bookingId, "confirm")).hasStatus(HttpStatus.OK);
        clock.setInstant(start.plusSeconds(3600));
        assertThat(bookingAction(provider.user(), bookingId, "complete")).hasStatus(HttpStatus.OK);
        clock.reset();
        return bookingId;
    }

    protected MvcTestResult review(TestUser owner, long bookingId, int rating, String comment) {
        return mvc.post().uri("/api/bookings/{id}/review", bookingId)
                .header(AUTHORIZATION, bearer(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"rating": %d, "comment": "%s"}
                        """.formatted(rating, comment))
                .exchange();
    }

    protected static String bearer(TestUser user) {
        return "Bearer " + user.token();
    }

    protected static String body(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    protected static long longAt(String json, String path) {
        Number value = JsonPath.read(json, path);
        return value.longValue();
    }
}
