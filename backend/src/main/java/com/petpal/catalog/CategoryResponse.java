package com.petpal.catalog;

public record CategoryResponse(CategoryCode code, String name, String description) {

    public static CategoryResponse from(ServiceCategory category) {
        return new CategoryResponse(category.getCode(), category.getName(), category.getDescription());
    }
}
