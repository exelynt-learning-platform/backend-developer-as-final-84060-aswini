package com.example.booking.service;

import com.example.booking.dto.ResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.entity.Resource;
import com.example.booking.exception.ConflictException;
import com.example.booking.exception.ResourceNotFoundException;
import com.example.booking.repository.ReservationRepository;
import com.example.booking.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;

    public ResourceService(ResourceRepository resourceRepository, ReservationRepository reservationRepository) {
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> list(Boolean available) {
        List<Resource> found = available == null ? resourceRepository.findAll() : resourceRepository.findByAvailable(available);
        return found.stream().map(ResourceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponse get(Long id) {
        return ResourceResponse.from(find(id));
    }

    @Transactional
    public ResourceResponse create(ResourceRequest req) {
        Resource r = new Resource();
        apply(r, req);
        return ResourceResponse.from(resourceRepository.save(r));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest req) {
        Resource r = find(id);
        apply(r, req);
        return ResourceResponse.from(resourceRepository.save(r));
    }

    @Transactional
    public void delete(Long id) {
        Resource r = find(id);
        if (reservationRepository.existsByResourceId(id)) {
            throw new ConflictException("Resource has reservations; delete them first");
        }
        resourceRepository.delete(r);
    }

    private void apply(Resource r, ResourceRequest req) {
        r.setName(req.name().trim());
        r.setDescription(req.description());
        r.setType(req.type().trim());
        r.setAvailable(req.available() == null || req.available());
    }

    private Resource find(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource " + id + " not found"));
    }
}
