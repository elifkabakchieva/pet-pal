package com.petpal.provider;

import com.petpal.common.error.BusinessRuleException;
import com.petpal.common.persistence.BaseEntity;
import com.petpal.identity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "provider_profiles")
public class ProviderProfile extends BaseEntity {

    static final int MIN_BIO_LENGTH_FOR_SUBMISSION = 50;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(length = 2000)
    private String bio;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(length = 200)
    private String address;

    @Column(name = "years_experience", nullable = false)
    private int yearsExperience;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus;

    @Column(name = "verification_note", length = 500)
    private String verificationNote;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "rating_average", nullable = false, precision = 3, scale = 2)
    private BigDecimal ratingAverage;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Version
    private long version;

    protected ProviderProfile() {
    }

    public static ProviderProfile createDraft(User user, String displayName, String city) {
        ProviderProfile profile = new ProviderProfile();
        profile.user = user;
        profile.displayName = displayName;
        profile.city = city;
        profile.verificationStatus = VerificationStatus.DRAFT;
        profile.ratingAverage = BigDecimal.ZERO.setScale(2);
        profile.reviewCount = 0;
        return profile;
    }

    public void updateDetails(String displayName, String bio, String city, String address, int yearsExperience) {
        this.displayName = displayName;
        this.bio = bio;
        this.city = city;
        this.address = address;
        this.yearsExperience = yearsExperience;
    }

    public void submitForVerification(long activeServiceCount, long availabilityWindowCount, Instant now) {
        if (verificationStatus != VerificationStatus.DRAFT && verificationStatus != VerificationStatus.REJECTED) {
            throw invalidTransition("sent for verification");
        }
        List<String> missing = new ArrayList<>();
        if (bio == null || bio.strip().length() < MIN_BIO_LENGTH_FOR_SUBMISSION) {
            missing.add("write a bio of at least " + MIN_BIO_LENGTH_FOR_SUBMISSION + " characters");
        }
        if (activeServiceCount == 0) {
            missing.add("add at least one service");
        }
        if (availabilityWindowCount == 0) {
            missing.add("set your weekly hours");
        }
        if (!missing.isEmpty()) {
            throw new BusinessRuleException("PROFILE_INCOMPLETE",
                    "Before sending your profile for verification: " + String.join("; ", missing) + ".");
        }
        verificationStatus = VerificationStatus.PENDING;
        submittedAt = now;
    }

    public void approve(Instant now) {
        requirePending("approved");
        verificationStatus = VerificationStatus.VERIFIED;
        verifiedAt = now;
        verificationNote = null;
    }

    public void reject(String note) {
        requirePending("rejected");
        verificationStatus = VerificationStatus.REJECTED;
        verificationNote = note;
    }

    public void applyRating(BigDecimal average, int count) {
        this.ratingAverage = average;
        this.reviewCount = count;
    }

    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    private void requirePending(String action) {
        if (verificationStatus != VerificationStatus.PENDING) {
            throw invalidTransition(action);
        }
    }

    private BusinessRuleException invalidTransition(String action) {
        return new BusinessRuleException("INVALID_VERIFICATION_TRANSITION",
                "A profile that is %s cannot be %s.".formatted(
                        verificationStatus.name().toLowerCase(Locale.ROOT), action));
    }

    public User getUser() {
        return user;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBio() {
        return bio;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public int getYearsExperience() {
        return yearsExperience;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public String getVerificationNote() {
        return verificationNote;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public int getReviewCount() {
        return reviewCount;
    }
}
