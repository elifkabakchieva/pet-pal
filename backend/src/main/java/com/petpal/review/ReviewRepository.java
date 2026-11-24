package com.petpal.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "author")
    Page<Review> findByProviderIdOrderByCreatedAtDescIdDesc(Long providerId, Pageable pageable);

    @Query("select new com.petpal.review.RatingSummary(avg(r.rating), count(r)) from Review r where r.providerId = :providerId")
    RatingSummary summarize(@Param("providerId") Long providerId);
}
