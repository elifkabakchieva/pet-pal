package com.petpal.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findAllByOrderBySortOrderAsc();

    Optional<ServiceCategory> findByCode(CategoryCode code);
}
