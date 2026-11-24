package com.petpal.provider;

import com.petpal.common.persistence.BaseEntity;
import com.petpal.common.persistence.DayOfWeekConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "availability_rules")
public class AvailabilityRule extends BaseEntity {

    @Column(name = "provider_id", nullable = false, updatable = false)
    private Long providerId;

    @Convert(converter = DayOfWeekConverter.class)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected AvailabilityRule() {
    }

    public static AvailabilityRule of(Long providerId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        AvailabilityRule rule = new AvailabilityRule();
        rule.providerId = providerId;
        rule.dayOfWeek = dayOfWeek;
        rule.startTime = startTime;
        rule.endTime = endTime;
        return rule;
    }

    public Long getProviderId() {
        return providerId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }
}
