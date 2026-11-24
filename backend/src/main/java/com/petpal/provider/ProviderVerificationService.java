package com.petpal.provider;

import com.petpal.common.error.ResourceNotFoundException;
import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderVerificationService {

    private final ProviderProfileRepository profiles;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ProviderVerificationService(ProviderProfileRepository profiles, ApplicationEventPublisher events, Clock clock) {
        this.profiles = profiles;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AdminProviderResponse> list(VerificationStatus status) {
        return profiles.findByVerificationStatusOrderBySubmittedAtAscIdAsc(status).stream()
                .map(AdminProviderResponse::from)
                .toList();
    }

    @Transactional
    public AdminProviderResponse approve(Long profileId) {
        ProviderProfile profile = require(profileId);
        profile.approve(clock.instant());
        events.publishEvent(new ProviderVerificationDecidedEvent(profile.getId(), profile.getUser().getId(), true, null));
        return AdminProviderResponse.from(profile);
    }

    @Transactional
    public AdminProviderResponse reject(Long profileId, String note) {
        ProviderProfile profile = require(profileId);
        String trimmedNote = note.trim();
        profile.reject(trimmedNote);
        events.publishEvent(
                new ProviderVerificationDecidedEvent(profile.getId(), profile.getUser().getId(), false, trimmedNote));
        return AdminProviderResponse.from(profile);
    }

    private ProviderProfile require(Long profileId) {
        return profiles.findWithUserById(profileId).orElseThrow(() ->
                new ResourceNotFoundException("PROVIDER_NOT_FOUND", "This provider does not exist."));
    }
}
