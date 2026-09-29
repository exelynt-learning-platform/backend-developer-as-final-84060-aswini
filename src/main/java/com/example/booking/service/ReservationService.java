package com.example.booking.service;

import com.example.booking.dto.PageResponse;
import com.example.booking.dto.ReservationRequest;
import com.example.booking.dto.ReservationResponse;
import com.example.booking.entity.Reservation;
import com.example.booking.entity.ReservationStatus;
import com.example.booking.entity.Resource;
import com.example.booking.entity.User;
import com.example.booking.exception.BadRequestException;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ReservationSpecifications;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.AppUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class ReservationService {
    private static final Set<String> SORTABLE = Set.of("id", "price", "startTime", "endTime", "status", "createdAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository, ResourceRepository resourceRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> search(AppUserDetails principal, ReservationStatus status,
                                                    BigDecimal minPrice, BigDecimal maxPrice,
                                                    int page, int size, String sortBy, String direction) {
        if (page < 0) throw new BadRequestException("page must be >= 0");
        if (size < 1 || size > MAX_PAGE_SIZE) throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must not be greater than maxPrice");
        }
        if (!SORTABLE.contains(sortBy)) {
            throw new BadRequestException("sortBy must be one of " + SORTABLE);
        }
        Sort.Direction dir;
        try {
            dir = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("direction must be 'asc' or 'desc'");
        }
        Sort sort = Sort.by(dir, sortBy);
        if (!sortBy.equals("id")) sort = sort.and(Sort.by(Sort.Direction.ASC, "id")); // stable paging

        // Ownership rule: USER is always restricted to their own reservations (id from the JWT).
        Long ownerId = principal.isAdmin() ? null : principal.getId();
        Page<Reservation> result = reservationRepository.findAll(
                ReservationSpecifications.filter(ownerId, status, minPrice, maxPrice), PageRequest.of(page, size, sort));
        return PageResponse.from(result.map(ReservationResponse::from));
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(AppUserDetails principal, Long id) {
        Reservation r = find(id);
        assertCanAccess(principal, r);
        return ReservationResponse.from(r);
    }

    @Transactional
    public ReservationResponse create(AppUserDetails principal, ReservationRequest req) {
        Resource resource = resourceRepository.findById(req.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource " + req.resourceId() + " not found"));
        if (!resource.isAvailable()) throw new ConflictException("Resource is not available for booking");
        validateTimes(req.startTime(), req.endTime());
        if (!req.startTime().isAfter(LocalDateTime.now())) {
            throw new BadRequestException("startTime must be in the future");
        }
        assertNoOverlap(resource.getId(), req.startTime(), req.endTime(), -1L);

        // Identity comes from the JWT. Only ADMIN may book on behalf of someone else / choose the status.
        User owner = (principal.isAdmin() && req.userId() != null)
                ? findUser(req.userId())
                : findUser(principal.getId());
        Reservation r = new Reservation();
        r.setResource(resource);
        r.setUser(owner);
        r.setStartTime(req.startTime());
        r.setEndTime(req.endTime());
        r.setPrice(req.price());
        r.setStatus(principal.isAdmin() && req.status() != null ? req.status() : ReservationStatus.PENDING);
        return ReservationResponse.from(reservationRepository.save(r));
    }

    /** ADMIN only (enforced in SecurityConfig). Full replace of a reservation. */
    @Transactional
    public ReservationResponse update(Long id, ReservationRequest req) {
        Reservation r = find(id);
        Resource resource = resourceRepository.findById(req.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource " + req.resourceId() + " not found"));
        validateTimes(req.startTime(), req.endTime());
        ReservationStatus status = req.status() != null ? req.status() : r.getStatus();
        if (status != ReservationStatus.CANCELLED) {
            assertNoOverlap(resource.getId(), req.startTime(), req.endTime(), id);
        }
        if (req.userId() != null) r.setUser(findUser(req.userId()));
        r.setResource(resource);
        r.setStartTime(req.startTime());
        r.setEndTime(req.endTime());
        r.setPrice(req.price());
        r.setStatus(status);
        return ReservationResponse.from(reservationRepository.save(r));
    }

    /** USER may cancel their own reservation; ADMIN may cancel any. */
    @Transactional
    public ReservationResponse cancel(AppUserDetails principal, Long id) {
        Reservation r = find(id);
        assertCanAccess(principal, r);
        r.setStatus(ReservationStatus.CANCELLED);
        return ReservationResponse.from(reservationRepository.save(r));
    }

    @Transactional
    public void delete(Long id) {
        reservationRepository.delete(find(id));
    }

    // ---- helpers ----

    private Reservation find(Long id) {
        return reservationRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation " + id + " not found"));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    private void assertCanAccess(AppUserDetails principal, Reservation r) {
        if (!principal.isAdmin() && !r.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Not the owner of this reservation");
        }
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) throw new BadRequestException("endTime must be after startTime");
    }

    private void assertNoOverlap(Long resourceId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        if (reservationRepository.countOverlapping(resourceId, start, end, excludeId) > 0) {
            throw new ConflictException("Resource is already booked for the requested time range");
        }
    }
}
