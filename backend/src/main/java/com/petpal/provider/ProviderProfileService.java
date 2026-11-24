package com.petpal.provider;

import com.petpal.common.error.ResourceNotFoundException;
import com.petpal.identity.UserRegisteredEvent;
import com.petpal.identity.UserRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderProfileService {

    private final ProviderProfileRepository profiles;
    private final ServiceOfferingRepository offerings;
    private final AvailabilityRuleRepository availabilityRules;
    private final UserRepository users;
    private final Clock clock;

    ProviderProfileService(ProviderProfileRepository profiles, ServiceOfferingRepository offerings,
            AvailabilityRuleRepository availabilityRules, UserRepository users, Clock clock) {
        this.profiles = profiles;
        this.offerings = offerings;
        this.availabilityRules = availabilityRules;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProviderProfileResponse getOwn(Long userId) {
        return ProviderProfileResponse.from(requireByUserId(userId));
    }

    @Transactional
    public ProviderProfileResponse updateOwn(Long userId, ProviderProfileRequest request) {
        ProviderProfile profile = requireByUserId(userId);
        profile.updateDetails(request.displayName().trim(), trimToNull(request.bio()), request.city().trim(),
                trimToNull(request.address()), request.yearsExperience());
        return ProviderProfileResponse.from(profile);
    }

    @Transactional
    public ProviderProfileResponse submitOwn(Long userId) {
        ProviderProfile profile = requireByUserId(userId);
        profile.submitForVerification(
                offerings.countByProviderIdAndActiveTrue(profile.getId()),
                availabilityRules.countByProviderId(profile.getId()),
                clock.instant());
        return ProviderProfileResponse.from(profile);
    }

    @Transactional
    public void createDraftFor(UserRegisteredEvent event) {
        profiles.save(ProviderProfile.createDraft(users.getReferenceById(event.userId()), event.fullName(),
                event.city()));
    }

    @Transactional(readOnly = true)
    public ProviderProfile requireByUserId(Long userId) {
        return profiles.findByUserId(userId).orElseThrow(() ->
                new ResourceNotFoundException("PROVIDER_NOT_FOUND", "No provider profile exists for this account."));
    }

    @Transactional(readOnly = true)
    public ProviderProfile requireVerified(Long profileId) {
        return profiles.findByIdAndVerificationStatus(profileId, VerificationStatus.VERIFIED).orElseThrow(() ->
                new ResourceNotFoundException("PROVIDER_NOT_FOUND", "This provider does not exist."));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
