package com.petpal.discovery;

import com.petpal.catalog.CategoryCode;
import com.petpal.provider.ProviderProfile;
import com.petpal.provider.ServiceOffering;
import com.petpal.provider.VerificationStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class ProviderSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ProviderSpecifications() {
    }

    static Specification<ProviderProfile> isVerified() {
        return (root, query, cb) -> cb.equal(root.get("verificationStatus"), VerificationStatus.VERIFIED);
    }

    static Specification<ProviderProfile> inCity(String city) {
        String normalised = city.toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.<String>get("city")), normalised);
    }

    static Specification<ProviderProfile> hasMinRating(BigDecimal minRating) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.<BigDecimal>get("ratingAverage"), minRating);
    }

    static Specification<ProviderProfile> matchesText(String text) {
        String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.<String>get("displayName")), pattern, LIKE_ESCAPE),
                cb.like(cb.lower(root.<String>get("bio")), pattern, LIKE_ESCAPE));
    }

    static Specification<ProviderProfile> offers(CategoryCode category, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            Subquery<Integer> services = query.subquery(Integer.class);
            Root<ServiceOffering> offering = services.from(ServiceOffering.class);
            List<Predicate> conditions = new ArrayList<>();
            conditions.add(cb.equal(offering.get("provider"), root));
            conditions.add(cb.isTrue(offering.<Boolean>get("active")));
            if (category != null) {
                conditions.add(cb.equal(offering.get("category").get("code"), category));
            }
            if (maxPrice != null) {
                conditions.add(cb.lessThanOrEqualTo(offering.<BigDecimal>get("price"), maxPrice));
            }
            services.select(cb.literal(1)).where(conditions.toArray(Predicate[]::new));
            return cb.exists(services);
        };
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
