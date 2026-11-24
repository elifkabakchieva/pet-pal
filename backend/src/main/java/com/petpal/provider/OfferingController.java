package com.petpal.provider;

import com.petpal.identity.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/provider/services")
@Tag(name = "Provider workspace")
class OfferingController {

    private final OfferingService offeringService;

    OfferingController(OfferingService offeringService) {
        this.offeringService = offeringService;
    }

    @GetMapping
    List<OfferingResponse> list(AuthenticatedUser user) {
        return offeringService.listOwn(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OfferingResponse create(AuthenticatedUser user, @Valid @RequestBody OfferingRequest request) {
        return offeringService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    OfferingResponse update(AuthenticatedUser user, @PathVariable Long id,
            @Valid @RequestBody OfferingRequest request) {
        return offeringService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deactivate(AuthenticatedUser user, @PathVariable Long id) {
        offeringService.deactivate(user.id(), id);
    }
}
