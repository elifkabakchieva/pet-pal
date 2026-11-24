package com.petpal.discovery;

import com.petpal.catalog.CategoryCode;
import com.petpal.common.web.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers")
@Tag(name = "Discovery")
class DiscoveryController {

    private final DiscoveryService discoveryService;

    DiscoveryController(DiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    @GetMapping
    PageResponse<ProviderSummaryResponse> search(
            @RequestParam(required = false) CategoryCode category,
            @RequestParam(required = false) @Size(max = 80) String city,
            @RequestParam(required = false) @DecimalMin("0.01") BigDecimal maxPrice,
            @RequestParam(required = false) @DecimalMin("0") @DecimalMax("5") BigDecimal minRating,
            @RequestParam(name = "q", required = false) @Size(max = 100) String text,
            @RequestParam(defaultValue = "RATING") ProviderSort sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        var criteria = new ProviderSearchCriteria(category, city, maxPrice, minRating, text);
        return discoveryService.search(criteria, sort, page, size);
    }

    @GetMapping("/{id}")
    ProviderDetailResponse provider(@PathVariable Long id) {
        return discoveryService.provider(id);
    }
}
