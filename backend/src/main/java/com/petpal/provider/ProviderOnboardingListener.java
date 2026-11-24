package com.petpal.provider;

import com.petpal.identity.Role;
import com.petpal.identity.UserRegisteredEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class ProviderOnboardingListener {

    private final ProviderProfileService profileService;

    ProviderOnboardingListener(ProviderProfileService profileService) {
        this.profileService = profileService;
    }

    @EventListener
    void on(UserRegisteredEvent event) {
        if (event.role() == Role.PROVIDER) {
            profileService.createDraftFor(event);
        }
    }
}
