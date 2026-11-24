package com.petpal.provider;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProviderProfileRepository
        extends JpaRepository<ProviderProfile, Long>, JpaSpecificationExecutor<ProviderProfile> {

    Optional<ProviderProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<ProviderProfile> findWithUserById(Long id);

    @EntityGraph(attributePaths = "user")
    List<ProviderProfile> findByVerificationStatusOrderBySubmittedAtAscIdAsc(VerificationStatus status);

    Optional<ProviderProfile> findByIdAndVerificationStatus(Long id, VerificationStatus status);
}
