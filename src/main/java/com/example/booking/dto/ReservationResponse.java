package com.example.booking.dto;

import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record ReservationResponse(Long id, Long resourceId, String resourceName, Long userId, String username,
                                  LocalDateTime startTime, LocalDateTime endTime, BigDecimal price,
                                  ReservationStatus status, Instant createdAt) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getResource().getId(), r.getResource().getName(),
                r.getUser().getId(), r.getUser().getUsername(), r.getStartTime(), r.getEndTime(),
                r.getPrice(), r.getStatus(), r.getCreatedAt());
    }
}
