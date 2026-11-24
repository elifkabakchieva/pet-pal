package com.petpal.catalog;

import com.petpal.common.error.ResourceNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final ServiceCategoryRepository categories;

    CategoryService(ServiceCategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categories.findAllByOrderBySortOrderAsc().stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ServiceCategory require(CategoryCode code) {
        return categories.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("CATEGORY_NOT_FOUND", "This category does not exist."));
    }
}
