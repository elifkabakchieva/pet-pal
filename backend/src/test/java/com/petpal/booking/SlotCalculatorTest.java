package com.petpal.booking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlotCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Sofia");
    private static final LocalDate TUESDAY = LocalDate.of(2026, 12, 8);
    private static final Instant LONG_AGO = Instant.parse("2020-01-01T00:00:00Z");

    private final SlotCalculator calculator = new SlotCalculator(Duration.ofMinutes(30));

    @Test
    void slotsStartAtTheWindowStartAndRepeatEveryStep() {
        List<TimeRange> slots = calculator.freeSlots(TUESDAY, ZONE, List.of(window("09:00", "11:00")),
                Duration.ofMinutes(60), List.of(), LONG_AGO);

        assertThat(starts(slots)).containsExactly("09:00", "09:30", "10:00");
    }

    @Test
    void exampleFromTheAnalysisIsReproduced() {
        List<DailyWindow> windows = List.of(window("09:00", "12:00"), window("14:00", "16:00"));
        List<TimeRange> busy = List.of(range("10:00", "11:00"));

        List<TimeRange> slots = calculator.freeSlots(TUESDAY, ZONE, windows, Duration.ofMinutes(90), busy, LONG_AGO);

        assertThat(starts(slots)).containsExactly("14:00", "14:30");
    }

    @Test
    void slotsTouchingABookingAreFree() {
        List<TimeRange> slots = calculator.freeSlots(TUESDAY, ZONE, List.of(window("09:00", "12:00")),
                Duration.ofMinutes(60), List.of(range("10:00", "11:00")), LONG_AGO);

        assertThat(starts(slots)).containsExactly("09:00", "11:00");
    }

    @Test
    void slotsBeforeTheLeadTimeAreSkipped() {
        Instant notBefore = TUESDAY.atTime(LocalTime.of(10, 10)).atZone(ZONE).toInstant();

        List<TimeRange> slots = calculator.freeSlots(TUESDAY, ZONE, List.of(window("09:00", "12:00")),
                Duration.ofMinutes(30), List.of(), notBefore);

        assertThat(starts(slots)).containsExactly("10:30", "11:00", "11:30");
    }

    @Test
    void serviceLongerThanTheWindowHasNoSlots() {
        List<TimeRange> slots = calculator.freeSlots(TUESDAY, ZONE, List.of(window("09:00", "10:00")),
                Duration.ofMinutes(90), List.of(), LONG_AGO);

        assertThat(slots).isEmpty();
    }

    @Test
    void slotsOfSeveralWindowsAreSorted() {
        List<TimeRange> slots = calculator.freeSlots(TUESDAY, ZONE,
                List.of(window("15:00", "16:00"), window("08:00", "09:00")), Duration.ofMinutes(60), List.of(),
                LONG_AGO);

        assertThat(starts(slots)).containsExactly("08:00", "15:00");
    }

    private static DailyWindow window(String start, String end) {
        return new DailyWindow(LocalTime.parse(start), LocalTime.parse(end));
    }

    private static TimeRange range(String start, String end) {
        return new TimeRange(TUESDAY.atTime(LocalTime.parse(start)).atZone(ZONE).toInstant(),
                TUESDAY.atTime(LocalTime.parse(end)).atZone(ZONE).toInstant());
    }

    private static List<String> starts(List<TimeRange> slots) {
        return slots.stream().map(slot -> slot.start().atZone(ZONE).toLocalTime().toString()).toList();
    }
}
