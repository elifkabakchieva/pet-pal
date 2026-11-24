package com.petpal.booking;

import org.springframework.data.domain.Sort;

public enum BookingListSort {

    UPCOMING(Sort.by(Sort.Order.asc("startAt"), Sort.Order.asc("id"))),
    RECENT(Sort.by(Sort.Order.desc("startAt"), Sort.Order.desc("id")));

    private final Sort sort;

    BookingListSort(Sort sort) {
        this.sort = sort;
    }

    Sort toSort() {
        return sort;
    }
}
