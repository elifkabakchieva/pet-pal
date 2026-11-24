package com.petpal.provider;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/providers")
@Tag(name = "Admin")
class AdminProviderController {

    private final ProviderVerificationService verificationService;

    AdminProviderController(ProviderVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping
    List<AdminProviderResponse> list(@RequestParam(defaultValue = "PENDING") VerificationStatus status) {
        return verificationService.list(status);
    }

    @PostMapping("/{id}/approve")
    AdminProviderResponse approve(@PathVariable Long id) {
        return verificationService.approve(id);
    }

    @PostMapping("/{id}/reject")
    AdminProviderResponse reject(@PathVariable Long id, @Valid @RequestBody RejectProviderRequest request) {
        return verificationService.reject(id, request.note());
    }
}
