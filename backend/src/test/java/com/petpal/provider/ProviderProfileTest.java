package com.petpal.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petpal.common.error.BusinessRuleException;
import com.petpal.identity.Role;
import com.petpal.identity.User;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProviderProfileTest {

    private static final Instant NOW = Instant.parse("2026-12-01T10:00:00Z");
    private static final String LONG_BIO = "Calm and patient groomer with nine years of experience with dogs and cats.";

    @Test
    void newProfileIsADraftWithoutRating() {
        ProviderProfile profile = draft();

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.DRAFT);
        assertThat(profile.getRatingAverage()).isEqualByComparingTo("0");
        assertThat(profile.getReviewCount()).isZero();
    }

    @Test
    void submissionListsEverythingThatIsMissing() {
        ProviderProfile profile = draft();

        assertThatThrownBy(() -> profile.submitForVerification(0, 0, NOW))
                .isInstanceOfSatisfying(BusinessRuleException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("PROFILE_INCOMPLETE");
                    assertThat(exception.getMessage())
                            .contains("bio of at least 50 characters", "at least one service", "weekly hours");
                });
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.DRAFT);
    }

    @Test
    void completeDraftBecomesPending() {
        ProviderProfile profile = completeDraft();

        profile.submitForVerification(1, 1, NOW);

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(profile.getSubmittedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectionStoresTheNoteAndAllowsResubmission() {
        ProviderProfile profile = completeDraft();
        profile.submitForVerification(1, 1, NOW);

        profile.reject("Please describe your qualifications.");
        profile.submitForVerification(1, 1, NOW.plusSeconds(60));

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(profile.getVerificationNote()).isEqualTo("Please describe your qualifications.");
    }

    @Test
    void approvalClearsTheNoteAndSetsVerifiedAt() {
        ProviderProfile profile = completeDraft();
        profile.submitForVerification(1, 1, NOW);
        profile.reject("Too short");
        profile.submitForVerification(1, 1, NOW);

        profile.approve(NOW.plusSeconds(120));

        assertThat(profile.isVerified()).isTrue();
        assertThat(profile.getVerificationNote()).isNull();
        assertThat(profile.getVerifiedAt()).isEqualTo(NOW.plusSeconds(120));
    }

    @Test
    void decisionsAndSubmissionsAreOnlyAllowedFromTheRightStatus() {
        ProviderProfile profile = completeDraft();

        assertThatThrownBy(() -> profile.approve(NOW))
                .hasFieldOrPropertyWithValue("code", "INVALID_VERIFICATION_TRANSITION");
        profile.submitForVerification(1, 1, NOW);
        assertThatThrownBy(() -> profile.submitForVerification(1, 1, NOW))
                .hasFieldOrPropertyWithValue("code", "INVALID_VERIFICATION_TRANSITION");
        profile.approve(NOW);
        assertThatThrownBy(() -> profile.reject("Changed my mind"))
                .hasFieldOrPropertyWithValue("code", "INVALID_VERIFICATION_TRANSITION");
    }

    private static ProviderProfile draft() {
        User user = User.register("georgi@example.com", "{noop}x", "Georgi", "Petrov", null, "Sofia", Role.PROVIDER);
        return ProviderProfile.createDraft(user, "Georgi Petrov", "Sofia");
    }

    private static ProviderProfile completeDraft() {
        ProviderProfile profile = draft();
        profile.updateDetails("Georgi's Grooming", LONG_BIO, "Sofia", "ul. Shipka 12", 9);
        return profile;
    }
}
