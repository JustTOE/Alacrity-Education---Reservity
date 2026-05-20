package dev.tmmc.reservity.spaces.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hibernate.type.SqlTypes.JSON;

@Entity
@Table(name = "spaces")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 12)
    private SpaceOwnerType ownerType;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 16)
    private SpaceType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false)
    private short floor;

    @Column(nullable = false, length = 20)
    private String room;

    @Column(nullable = false)
    private short seats;

    @Column(name = "area_sqm", nullable = false, precision = 7, scale = 2)
    private BigDecimal areaSqm;

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false, length = 8)
    private String currency;

    /**
     * Generated column — Postgres computes it as {@code price_per_hour = 0}.
     * Marked {@code insertable=false, updatable=false} so JPA never tries to
     * write it; Hibernate reads it back after each insert via the
     * {@code @Generated} annotation.
     */
    @Column(name = "is_free", insertable = false, updatable = false)
    private Boolean isFree;

    @Column(nullable = false, length = 140)
    private String blurb;

    @Column(length = 2000)
    private String description;

    @JdbcTypeCode(JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode amenities;

    @JdbcTypeCode(JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode rules;

    @JdbcTypeCode(JSON)
    @Column(name = "operating_hours", nullable = false, columnDefinition = "jsonb")
    private JsonNode operatingHours;

    @Column(name = "pin_x", precision = 5, scale = 4)
    private BigDecimal pinX;

    @Column(name = "pin_y", precision = 5, scale = 4)
    private BigDecimal pinY;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SpaceStatus status;

    @Column(nullable = false)
    private boolean surprise;

    @Column(name = "drop_in", nullable = false)
    private boolean dropIn;

    @Column(name = "instant_book", nullable = false)
    private boolean instantBook;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "rating_avg", precision = 3, scale = 2)
    private BigDecimal ratingAvg;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Vibes owned via the join entity. Read-only here — favorites and waitlist
     * have their own services for writes; vibes are wired during space creation
     * (M9 owner UI). Until then the seed populates space_vibes directly.
     */
    @OneToMany(mappedBy = "space", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SpaceVibe> spaceVibes = new HashSet<>();

    /**
     * Image rows for this space. Read-only on this side — writes go through
     * {@code SpaceImageService} + {@code SpaceImageRepository}. We deliberately
     * avoid cascade=ALL so that fetching a Space (with its images via
     * @EntityGraph) and then deleting an image directly via the repo doesn't
     * trigger Hibernate to re-cascade-persist the deleted row from the parent's
     * Set on flush. DB-level FK ON DELETE CASCADE handles space removal.
     */
    @OneToMany(mappedBy = "space", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<SpaceImage> images = new HashSet<>();

    @PrePersist
    void onCreate() {
        if (status == null) status = SpaceStatus.PUBLISHED;
        if (currency == null) currency = "USD";
    }

    public boolean isInstantBook() {
        return instantBook;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isFree() {
        return Boolean.TRUE.equals(isFree)
                || (pricePerHour != null && pricePerHour.compareTo(BigDecimal.ZERO) == 0);
    }
}
