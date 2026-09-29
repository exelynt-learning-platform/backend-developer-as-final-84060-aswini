package com.example.booking.dto;

import com.example.booking.entity.ReservationStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request body for creating / updating a reservation.
 * <p>
 * {@code userId} and {@code status} are only honoured for ADMIN callers. For a normal USER the
 * owner always comes from the JWT and a new reservation is always PENDING.
 */
public record ReservationRequest(
        @NotNull(message = "resourceId is required") Long resourceId,
        Long userId,
        @NotNull(message = "startTime is required") LocalDateTime startTime,
        @NotNull(message = "endTime is required") LocalDateTime endTime,
        @NotNull(message = "price is required")
        @DecimalMin(value = "0.00", message = "price must not be negative")
        @Digits(integer = 8, fraction = 2, message = "price must have at most 8 integer digits and 2 decimals")
        BigDecimal price,
        ReservationStatus status) {
}
