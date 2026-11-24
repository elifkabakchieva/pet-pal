package com.petpal.discovery;

import static java.util.stream.Collectors.groupingBy;

import com.petpal.catalog.CategoryCode;
import com.petpal.common.config.PetPalProperties;
import com.petpal.common.web.PageResponse;
import com.petpal.provider.AvailabilityService;
import com.petpal.provider.OfferingResponse;
import com.petpal.provider.ProviderProfile;
import com.petpal.provider.ProviderProfileRepository;
import com.petpal.provider.ProviderProfileService;
import com.petpal.provider.ServiceOffering;
import com.petpal.provider.ServiceOfferingRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiscoveryService {

    static final int EXCERPT_LENGTH = 160;

    private final ProviderProfileRepository profiles;
    private final ProviderProfileService profileService;
    private final ServiceOfferingRepository offerings;
    private final AvailabilityService availability;
    private final PetPalProperties properties;

    DiscoveryService(ProviderProfileRepository profiles, ProviderProfileService profileService,
            ServiceOfferingRepository offerings, AvailabilityService availability, PetPalProperties properties) {
        this.profiles = profiles;
        this.profileService = profileService;
        this.offerings = offerings;
        this.availability = availability;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProviderSummaryResponse> search(ProviderSearchCriteria criteria, ProviderSort sort, int page,
            int size) {
        Page<ProviderProfile> result =
                profiles.findAll(specificationFor(criteria), PageRequest.of(page, size, sort.toSort()));
        Map<Long, List<ServiceOffering>> servicesByProvider = result.isEmpty()
                ? Map.of()
                : offerings.findActiveWithCategoryByProviderIds(result.map(ProviderProfile::getId).getContent())
                        .stream()
                        .collect(groupingBy(offering -> offering.getProvider().getId()));
        return PageResponse.from(result,
                profile -> summaryOf(profile, servicesByProvider.getOrDefault(profile.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public ProviderDetailResponse provider(Long id) {
        ProviderProfile profile = profileService.requireVerified(id);
        List<OfferingResponse> services = offerings.findActiveWithCategoryByProviderIds(List.of(id)).stream()
                .map(OfferingResponse::from)
                .toList();
        return new ProviderDetailResponse(profile.getId(), profile.getDisplayName(), profile.getBio(),
                profile.getCity(), profile.getAddress(), profile.getYearsExperience(), profile.getRatingAverage(),
                profile.getReviewCount(), profile.getCreatedAt(), properties.currency(), services,
                availability.windowsOf(id));
    }

    private static Specification<ProviderProfile> specificationFor(ProviderSearchCriteria criteria) {
        List<Specification<ProviderProfile>> specifications = new ArrayList<>();
        specifications.add(ProviderSpecifications.isVerified());
        if (criteria.city() != null) {
            specifications.add(ProviderSpecifications.inCity(criteria.city()));
        }
        if (criteria.minRating() != null) {
            specifications.add(ProviderSpecifications.hasMinRating(criteria.minRating()));
        }
        if (criteria.text() != null) {
            specifications.add(ProviderSpecifications.matchesText(criteria.text()));
        }
        if (criteria.filtersOnServices()) {
            specifications.add(ProviderSpecifications.offers(criteria.category(), criteria.maxPrice()));
        }
        return Specification.allOf(specifications);
    }

    private static ProviderSummaryResponse summaryOf(ProviderProfile profile, List<ServiceOffering> services) {
        List<CategoryCode> categories = services.stream()
                .map(offering -> offering.getCategory().getCode())
                .distinct()
                .sorted()
                .toList();
        BigDecimal startingPrice = services.stream()
                .map(ServiceOffering::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new ProviderSummaryResponse(profile.getId(), profile.getDisplayName(), profile.getCity(),
                excerpt(profile.getBio()), profile.getRatingAverage(), profile.getReviewCount(),
                profile.getYearsExperience(), categories, startingPrice);
    }

    static String excerpt(String text) {
        if (text == null || text.length() <= EXCERPT_LENGTH) {
            return text;
        }
        String cut = text.substring(0, EXCERPT_LENGTH);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > EXCERPT_LENGTH / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut.replaceAll("[\\s.,;:!?-]+$", "") + "…";
    }
}
