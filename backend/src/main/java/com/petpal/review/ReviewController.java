package com.petpal.review;

import com.petpal.common.web.PageResponse;
import com.petpal.identity.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Reviews")
class ReviewController {

    private final ReviewService reviewService;

    ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/api/bookings/{bookingId}/review")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    ReviewResponse create(AuthenticatedUser user, @PathVariable Long bookingId,
            @Valid @RequestBody ReviewRequest request) {
        return reviewService.create(user.id(), bookingId, request);
    }

    @GetMapping("/api/providers/{providerId}/reviews")
    PageResponse<ReviewResponse> forProvider(@PathVariable Long providerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size) {
        return reviewService.forProvider(providerId, page, size);
    }
}
