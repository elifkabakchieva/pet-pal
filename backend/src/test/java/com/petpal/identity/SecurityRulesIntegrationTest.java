package com.petpal.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.petpal.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class SecurityRulesIntegrationTest extends IntegrationTest {

    @Test
    void anonymousCallsToProtectedEndpointsAreUnauthorized() {
        assertThat(mvc.get().uri("/api/pets")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/provider/profile")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/admin/providers")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/bookings")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void ownersCannotUseProviderOrAdminEndpoints() {
        TestUser owner = registerOwner();

        assertThat(mvc.get().uri("/api/provider/profile").header(AUTHORIZATION, bearer(owner)))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/admin/providers").header(AUTHORIZATION, bearer(owner)))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void providersCannotUsePetOrAdminEndpoints() {
        TestUser provider = registerProvider();

        assertThat(mvc.get().uri("/api/pets").header(AUTHORIZATION, bearer(provider)))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/admin/providers").header(AUTHORIZATION, bearer(provider)))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminsCannotUseOwnerOrProviderEndpoints() {
        TestUser admin = loginAdmin();

        assertThat(mvc.get().uri("/api/pets").header(AUTHORIZATION, bearer(admin)))
                .hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.get().uri("/api/provider/profile").header(AUTHORIZATION, bearer(admin)))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void healthIsPublic() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatus(HttpStatus.OK);
    }
}
