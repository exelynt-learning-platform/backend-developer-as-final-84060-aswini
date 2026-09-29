package com.example.booking.controller;

import com.example.booking.dto.ResourceRequest;
import com.example.booking.dto.ResourceResponse;
import com.example.booking.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/resources")
@Tag(name = "Resources")
@SecurityRequirement(name = "bearerAuth")
public class ResourceController {
    private final ResourceService service;

    public ResourceController(ResourceService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "List resources (USER, ADMIN). Optional ?available=true|false")
    public List<ResourceResponse> list(@RequestParam(required = false) Boolean available) {
        return service.list(available);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a resource (USER, ADMIN)")
    public ResourceResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a resource (ADMIN)")
    public ResourceResponse create(@Valid @RequestBody ResourceRequest request) { return service.create(request); }

    @PutMapping("/{id}")
    @Operation(summary = "Update a resource (ADMIN)")
    public ResourceResponse update(@PathVariable Long id, @Valid @RequestBody ResourceRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a resource (ADMIN)")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
