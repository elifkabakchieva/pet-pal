package com.petpal.provider;

import com.petpal.common.error.BusinessRuleException;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityService {

    private static final Comparator<AvailabilityWindow> BY_DAY_AND_START =
            Comparator.comparing(AvailabilityWindow::dayOfWeek).thenComparing(AvailabilityWindow::startTime);

    private final AvailabilityRuleRepository rules;
    private final ProviderProfileService profiles;

    AvailabilityService(AvailabilityRuleRepository rules, ProviderProfileService profiles) {
        this.rules = rules;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityWindow> getOwn(Long userId) {
        return windowsOf(profiles.requireByUserId(userId).getId());
    }

    @Transactional
    public List<AvailabilityWindow> replaceOwn(Long userId, WeeklyHoursRequest request) {
        Long providerId = profiles.requireByUserId(userId).getId();
        List<AvailabilityWindow> windows = request.windows().stream().sorted(BY_DAY_AND_START).toList();
        validate(windows);
        rules.deleteAllByProviderId(providerId);
        rules.saveAll(windows.stream()
                .map(window -> AvailabilityRule.of(providerId, window.dayOfWeek(), window.startTime(),
                        window.endTime()))
                .toList());
        return windows;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityWindow> windowsOf(Long providerId) {
        return rules.findByProviderIdOrderByDayOfWeekAscStartTimeAsc(providerId).stream()
                .map(AvailabilityWindow::from)
                .toList();
    }

    static void validate(List<AvailabilityWindow> windows) {
        for (AvailabilityWindow window : windows) {
            boolean valid = isOnQuarterHour(window.startTime()) && isOnQuarterHour(window.endTime())
                    && window.endTime().isAfter(window.startTime());
            if (!valid) {
                throw new BusinessRuleException("INVALID_AVAILABILITY_WINDOW",
                        "Each time window must start and end on a quarter hour and end after it starts.");
            }
        }
        for (int i = 1; i < windows.size(); i++) {
            AvailabilityWindow previous = windows.get(i - 1);
            AvailabilityWindow current = windows.get(i);
            if (previous.dayOfWeek() == current.dayOfWeek() && previous.endTime().isAfter(current.startTime())) {
                String day = current.dayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                throw new BusinessRuleException("OVERLAPPING_AVAILABILITY",
                        "The time windows on %s overlap. Make sure they do not cross each other.".formatted(day));
            }
        }
    }

    private static boolean isOnQuarterHour(LocalTime time) {
        return time.getSecond() == 0 && time.getNano() == 0 && time.getMinute() % 15 == 0;
    }
}
