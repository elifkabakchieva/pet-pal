package com.petpal.booking;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"owner", "pet", "provider", "provider.user", "offering", "offering.category"})
    Page<Booking> findByOwnerIdAndStatusIn(Long ownerId, Collection<BookingStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "pet", "provider", "provider.user", "offering", "offering.category"})
    Page<Booking> findByProviderIdAndStatusIn(Long providerId, Collection<BookingStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "pet", "provider", "provider.user", "offering", "offering.category"})
    Optional<Booking> findDetailedById(Long id);

    @Query("""
            select count(b) > 0 from Booking b
            where b.pet.id = :petId and b.status in :statuses
              and b.startAt < :endAt and b.endAt > :startAt
            """)
    boolean existsOverlapForPet(@Param("petId") Long petId, @Param("statuses") Collection<BookingStatus> statuses,
            @Param("startAt") Instant startAt, @Param("endAt") Instant endAt);

    List<Booking> findTop100ByStatusAndStartAtLessThanEqualOrderByStartAtAsc(BookingStatus status, Instant now);
}
