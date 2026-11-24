package com.petpal.provider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    @EntityGraph(attributePaths = "category")
    List<ServiceOffering> findByProviderIdOrderByActiveDescTitleAsc(Long providerId);

    @EntityGraph(attributePaths = "category")
    Optional<ServiceOffering> findByIdAndProviderId(Long id, Long providerId);

    @EntityGraph(attributePaths = {"category", "provider", "provider.user"})
    Optional<ServiceOffering> findWithProviderById(Long id);

    long countByProviderIdAndActiveTrue(Long providerId);

    @Query("""
            select o from ServiceOffering o join fetch o.category c
            where o.provider.id in :providerIds and o.active = true
            order by c.sortOrder asc, o.price asc, o.id asc
            """)
    List<ServiceOffering> findActiveWithCategoryByProviderIds(@Param("providerIds") Collection<Long> providerIds);
}
