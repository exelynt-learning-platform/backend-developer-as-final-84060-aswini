package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class ReservationSpecifications {
    private ReservationSpecifications() {}

    public static Specification<Reservation> filter(Long ownerId, ReservationStatus status,
                                                    BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ownerId != null) predicates.add(cb.equal(root.get("user").get("id"), ownerId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (minPrice != null) predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            if (maxPrice != null) predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
