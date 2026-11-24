package com.petpal.discovery;

import org.springframework.data.domain.Sort;

public enum ProviderSort {

    RATING(Sort.by(Sort.Order.desc("ratingAverage"), Sort.Order.desc("reviewCount"), Sort.Order.asc("id"))),
    REVIEWS(Sort.by(Sort.Order.desc("reviewCount"), Sort.Order.desc("ratingAverage"), Sort.Order.asc("id"))),
    EXPERIENCE(Sort.by(Sort.Order.desc("yearsExperience"), Sort.Order.desc("ratingAverage"), Sort.Order.asc("id")));

    private final Sort sort;

    ProviderSort(Sort sort) {
        this.sort = sort;
    }

    Sort toSort() {
        return sort;
    }
}
