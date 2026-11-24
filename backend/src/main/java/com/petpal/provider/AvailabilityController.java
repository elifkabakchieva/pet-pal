package com.petpal.provider;

import com.petpal.identity.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider/availability")
@Tag(name = "Provider workspace")
class AvailabilityController {

    private final AvailabilityService availabilityService;

    AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    List<AvailabilityWindow> get(AuthenticatedUser user) {
        return availabilityService.getOwn(user.id());
    }

    @PutMapping
    List<AvailabilityWindow> replace(AuthenticatedUser user, @Valid @RequestBody WeeklyHoursRequest request) {
        return availabilityService.replaceOwn(user.id(), request);
    }
}
