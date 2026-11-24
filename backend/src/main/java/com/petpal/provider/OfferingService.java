package com.petpal.provider;

import com.petpal.catalog.CategoryService;
import com.petpal.catalog.ServiceCategory;
import com.petpal.common.error.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferingService {

    private final ServiceOfferingRepository offerings;
    private final ProviderProfileService profiles;
    private final CategoryService categories;

    OfferingService(ServiceOfferingRepository offerings, ProviderProfileService profiles, CategoryService categories) {
        this.offerings = offerings;
        this.profiles = profiles;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> listOwn(Long userId) {
        Long providerId = profiles.requireByUserId(userId).getId();
        return offerings.findByProviderIdOrderByActiveDescTitleAsc(providerId).stream()
                .map(OfferingResponse::from)
                .toList();
    }

    @Transactional
    public OfferingResponse create(Long userId, OfferingRequest request) {
        ProviderProfile provider = profiles.requireByUserId(userId);
        ServiceCategory category = categories.require(request.categoryCode());
        ServiceOffering offering = ServiceOffering.create(provider, category, request.title().trim(),
                trimToNull(request.description()), request.price(), request.durationMinutes());
        return OfferingResponse.from(offerings.save(offering));
    }

    @Transactional
    public OfferingResponse update(Long userId, Long offeringId, OfferingRequest request) {
        ServiceOffering offering = requireOwn(userId, offeringId);
        offering.update(categories.require(request.categoryCode()), request.title().trim(),
                trimToNull(request.description()), request.price(), request.durationMinutes());
        return OfferingResponse.from(offering);
    }

    @Transactional
    public void deactivate(Long userId, Long offeringId) {
        requireOwn(userId, offeringId).deactivate();
    }

    @Transactional(readOnly = true)
    public ServiceOffering requireActiveWithProvider(Long offeringId) {
        return offerings.findWithProviderById(offeringId)
                .filter(ServiceOffering::isActive)
                .orElseThrow(OfferingService::notFound);
    }

    private ServiceOffering requireOwn(Long userId, Long offeringId) {
        Long providerId = profiles.requireByUserId(userId).getId();
        return offerings.findByIdAndProviderId(offeringId, providerId).orElseThrow(OfferingService::notFound);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("OFFERING_NOT_FOUND", "This service does not exist.");
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
