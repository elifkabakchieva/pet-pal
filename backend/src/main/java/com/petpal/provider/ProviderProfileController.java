package com.petpal.provider;

import com.petpal.identity.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider/profile")
@Tag(name = "Provider workspace")
class ProviderProfileController {

    private final ProviderProfileService profileService;

    ProviderProfileController(ProviderProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    ProviderProfileResponse get(AuthenticatedUser user) {
        return profileService.getOwn(user.id());
    }

    @PutMapping
    ProviderProfileResponse update(AuthenticatedUser user, @Valid @RequestBody ProviderProfileRequest request) {
        return profileService.updateOwn(user.id(), request);
    }

    @PostMapping("/submit")
    ProviderProfileResponse submit(AuthenticatedUser user) {
        return profileService.submitOwn(user.id());
    }
}
