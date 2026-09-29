package com.example.booking.repository;

import com.example.booking.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {

    /** Fetch resource + user in the same query to avoid N+1 when mapping to DTOs. */
    @Override
    @EntityGraph(attributePaths = {"resource", "user"})
    Page<Reservation> findAll(Specification<Reservation> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"resource", "user"})
    Optional<Reservation> findWithDetailsById(Long id);

    boolean existsByResourceId(Long resourceId);

    /** Counts non-cancelled reservations overlapping [start, end). Use excludeId = -1 for "none". */
    @Query("""
            select count(r) from Reservation r
            where r.resource.id = :resourceId
              and r.status <> com.example.booking.entity.ReservationStatus.CANCELLED
              and r.startTime < :end and r.endTime > :start
              and r.id <> :excludeId
            """)
    long countOverlapping(@Param("resourceId") Long resourceId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("excludeId") Long excludeId);
}
