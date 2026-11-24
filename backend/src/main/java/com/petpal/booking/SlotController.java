package com.petpal.booking;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/{providerId}/slots")
@Tag(name = "Discovery")
class SlotController {

    private final BookingSlotService slotService;

    SlotController(BookingSlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    List<SlotResponse> slots(@PathVariable Long providerId, @RequestParam Long offeringId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.freeSlots(providerId, offeringId, date);
    }
}
