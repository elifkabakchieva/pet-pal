package com.petpal.provider;

import java.time.DayOfWeek;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {

    List<AvailabilityRule> findByProviderIdOrderByDayOfWeekAscStartTimeAsc(Long providerId);

    List<AvailabilityRule> findByProviderIdAndDayOfWeekOrderByStartTimeAsc(Long providerId, DayOfWeek dayOfWeek);

    long countByProviderId(Long providerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AvailabilityRule r where r.providerId = :providerId")
    void deleteAllByProviderId(@Param("providerId") Long providerId);
}
