package dev.tmmc.reservity.spaces.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vibes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vibe {

    /**
     * The PK is the slug used by the frontend ('focus', 'lab-only', etc.).
     * Not auto-generated — every row is curated and seeded by R__seed_vibes.
     */
    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, length = 40)
    private String label;

    @Column(nullable = false, length = 8)
    private String icon;

    @Column(length = 200)
    private String description;

    @Column(name = "display_order", nullable = false)
    private short displayOrder;

    @Column(nullable = false)
    private boolean active;
}
