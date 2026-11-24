package com.petpal.booking;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class BookedTimeQuery {

    private final JdbcClient jdbc;

    BookedTimeQuery(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    List<TimeRange> busyRanges(Long providerId, Instant from, Instant to) {
        return jdbc.sql("""
                        select start_at, end_at from bookings
                        where provider_id = :providerId
                          and status in ('REQUESTED', 'CONFIRMED')
                          and start_at < :to and end_at > :from
                        order by start_at
                        """)
                .param("providerId", providerId)
                .param("from", OffsetDateTime.ofInstant(from, ZoneOffset.UTC))
                .param("to", OffsetDateTime.ofInstant(to, ZoneOffset.UTC))
                .query((resultSet, rowNumber) -> new TimeRange(
                        resultSet.getObject("start_at", OffsetDateTime.class).toInstant(),
                        resultSet.getObject("end_at", OffsetDateTime.class).toInstant()))
                .list();
    }
}
