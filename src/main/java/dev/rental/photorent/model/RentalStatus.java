package dev.rental.photorent.model;

import java.util.List;

public enum RentalStatus {
    BOOKED,
    ACTIVE,
    RETURNED,
    OVERDUE;

    // аренды, которые занимают технику (те же статусы указаны в ограничении EXCLUDE в schema.sql)
    public static final List<RentalStatus> OPEN_STATUSES = List.of(BOOKED, ACTIVE, OVERDUE);
}

