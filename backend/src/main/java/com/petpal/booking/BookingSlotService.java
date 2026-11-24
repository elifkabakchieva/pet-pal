package com.petpal.booking;

import com.petpal.common.config.PetPalProperties;
import com.petpal.common.error.ResourceNotFoundException;
import com.petpal.provider.AvailabilityRuleRepository;
import com.petpal.provider.OfferingService;
import com.petpal.provider.ProviderProfileService;
import com.petpal.provider.ServiceOffering;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingSlotService {

    private final ProviderProfileService profiles;
    private final OfferingService offerings;
    private final AvailabilityRuleRepository availabilityRules;
    private final BookedTimeQuery bookedTime;
    private final PetPalProperties properties;
    private final Clock clock;
    private final SlotCalculator calculator;

    BookingSlotService(ProviderProfileService profiles, OfferingService offerings,
            AvailabilityRuleRepository availabilityRules, BookedTimeQuery bookedTime, PetPalProperties properties,
            Clock clock) {
        this.profiles = profiles;
        this.offerings = offerings;
        this.availabilityRules = availabilityRules;
        this.bookedTime = bookedTime;
        this.properties = properties;
        this.clock = clock;
        this.calculator = new SlotCalculator(properties.booking().slotStep());
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> freeSlots(Long providerId, Long offeringId, LocalDate date) {
        profiles.requireVerified(providerId);
        ServiceOffering offering = offerings.requireActiveWithProvider(offeringId);
        if (!offering.getProvider().getId().equals(providerId)) {
            throw new ResourceNotFoundException("OFFERING_NOT_FOUND", "This service does not exist.");
        }
        return calculate(offering, date).stream().map(SlotResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public boolean isFree(ServiceOffering offering, Instant startAt) {
        LocalDate date = startAt.atZone(properties.businessZone()).toLocalDate();
        return calculate(offering, date).stream().anyMatch(slot -> slot.start().equals(startAt));
    }

    private List<TimeRange> calculate(ServiceOffering offering, LocalDate date) {
        ZoneId zone = properties.businessZone();
        LocalDate today = LocalDate.now(clock.withZone(zone));
        if (date.isBefore(today) || date.isAfter(today.plus(properties.booking().horizon()))) {
            return List.of();
        }
        Long providerId = offering.getProvider().getId();
        List<DailyWindow> windows = availabilityRules
                .findByProviderIdAndDayOfWeekOrderByStartTimeAsc(providerId, date.getDayOfWeek()).stream()
                .map(rule -> new DailyWindow(rule.getStartTime(), rule.getEndTime()))
                .toList();
        if (windows.isEmpty()) {
            return List.of();
        }
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        List<TimeRange> busy = bookedTime.busyRanges(providerId, dayStart, dayEnd);
        Instant notBefore = clock.instant().plus(properties.booking().minLeadTime());
        return calculator.freeSlots(date, zone, windows, offering.duration(), busy, notBefore);
    }
}
