package com.example.booking.controller;

import com.example.booking.dto.PageResponse;
import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.entity.ReservationStatus;
import com.example.booking.security.AppUserDetails;
import com.example.booking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reservations")
@Tag(name = "Reservations")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Search reservations (ADMIN: all, USER: only own) with filters, pagination and sorting")
    public PageResponse<ReservationResponse> list(
            @Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "id | price | startTime | endTime | status | createdAt")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "asc | desc") @RequestParam(defaultValue = "asc") String direction) {
        return service.search(principal, status, minPrice, maxPrice, page, size, sortBy, direction);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a reservation (ADMIN: any, USER: own only)")
    public ReservationResponse get(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                   @PathVariable Long id) {
        return service.get(principal, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a reservation. Owner is taken from the JWT; USER reservations start as PENDING")
    public ReservationResponse create(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                      @Valid @RequestBody ReservationRequest request) {
        return service.create(principal, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reservation (ADMIN)")
    public ReservationResponse update(@PathVariable Long id, @Valid @RequestBody ReservationRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a reservation (ADMIN: any, USER: own only)")
    public ReservationResponse cancel(@Parameter(hidden = true) @AuthenticationPrincipal AppUserDetails principal,
                                      @PathVariable Long id) {
        return service.cancel(principal, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a reservation (ADMIN)")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
