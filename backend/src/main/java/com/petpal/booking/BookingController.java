package com.petpal.booking;

import com.petpal.common.web.PageResponse;
import com.petpal.identity.AuthenticatedUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings")
@PreAuthorize("hasAnyRole('OWNER', 'PROVIDER')")
class BookingController {

    private final BookingService bookingService;

    BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    BookingResponse request(AuthenticatedUser user, @Valid @RequestBody CreateBookingRequest request) {
        return bookingService.request(user, request);
    }

    @GetMapping
    PageResponse<BookingResponse> list(AuthenticatedUser user,
            @RequestParam(name = "status", required = false) Set<BookingStatus> statuses,
            @RequestParam(defaultValue = "RECENT") BookingListSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return bookingService.list(user, statuses, sort, page, size);
    }

    @GetMapping("/{id}")
    BookingDetailResponse detail(AuthenticatedUser user, @PathVariable Long id) {
        return bookingService.detail(user, id);
    }

    @PostMapping("/{id}/confirm")
    BookingResponse confirm(AuthenticatedUser user, @PathVariable Long id) {
        return bookingService.confirm(user, id);
    }

    @PostMapping("/{id}/decline")
    BookingResponse decline(AuthenticatedUser user, @PathVariable Long id,
            @Valid @RequestBody(required = false) BookingReasonRequest body) {
        return bookingService.decline(user, id, body == null ? null : body.reason());
    }

    @PostMapping("/{id}/cancel")
    BookingResponse cancel(AuthenticatedUser user, @PathVariable Long id,
            @Valid @RequestBody(required = false) BookingReasonRequest body) {
        return bookingService.cancel(user, id, body == null ? null : body.reason());
    }

    @PostMapping("/{id}/complete")
    BookingResponse complete(AuthenticatedUser user, @PathVariable Long id) {
        return bookingService.complete(user, id);
    }
}
