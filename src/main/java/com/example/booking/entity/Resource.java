package com.example.booking.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resources")
@Getter
@Setter
@NoArgsConstructor
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /** Free-form category, e.g. ROOM, VEHICLE, EQUIPMENT. */
    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private boolean available = true;

    public Resource(String name, String description, String type, boolean available) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.available = available;
    }
}
