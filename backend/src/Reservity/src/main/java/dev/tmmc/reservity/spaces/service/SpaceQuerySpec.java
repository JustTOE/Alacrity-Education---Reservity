package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceStatus;
import dev.tmmc.reservity.spaces.entity.SpaceType;
import dev.tmmc.reservity.spaces.entity.SpaceVibe;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Composable {@link Specification}s used by {@link SpaceService} to translate
 * filter params into Criteria queries.
 *
 * <p>The {@link #published()} predicate is always combined with whatever
 * filters the caller chooses, so unpublished or soft-deleted spaces never
 * leak into a public response.</p>
 */
public final class SpaceQuerySpec {

    private SpaceQuerySpec() {}

    public static Specification<Space> published() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), SpaceStatus.PUBLISHED),
                cb.isNull(root.get("deletedAt"))
        );
    }

    public static Specification<Space> byTextLike(String q) {
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("blurb")), like)
        );
    }

    public static Specification<Space> byType(String typeRaw) {
        if (typeRaw == null || typeRaw.isBlank()) return null;
        SpaceType type = SpaceType.fromValue(typeRaw);
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Space> byMinSeats(Short minSeats) {
        if (minSeats == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("seats"), minSeats);
    }

    public static Specification<Space> byMaxPrice(BigDecimal maxPrice) {
        if (maxPrice == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("pricePerHour"), maxPrice);
    }

    public static Specification<Space> freeOnly(Boolean isFree) {
        if (isFree == null || !isFree) return null;
        return (root, query, cb) -> cb.equal(root.get("pricePerHour"), BigDecimal.ZERO);
    }

    public static Specification<Space> byVibes(List<String> vibeIds) {
        if (vibeIds == null || vibeIds.isEmpty()) return null;
        return (root, query, cb) -> {
            // EXISTS (SELECT 1 FROM SpaceVibe sv WHERE sv.space = root AND sv.vibe.id IN ?)
            Subquery<UUID> sub = query.subquery(UUID.class);
            var sv = sub.from(SpaceVibe.class);
            sub.select(cb.literal(UUID.randomUUID()))
                    .where(cb.and(
                            cb.equal(sv.get("space"), root),
                            sv.get("vibe").get("id").in(vibeIds)
                    ));
            return cb.exists(sub);
        };
    }

    public static Specification<Space> byBuilding(UUID buildingId) {
        if (buildingId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("building").get("id"), buildingId);
    }

    /**
     * Combine several specs (skipping nulls) into one. Convenience for the
     * service layer so it can declare a list of candidate predicates and
     * reduce them in one place.
     */
    @SafeVarargs
    public static Specification<Space> all(Specification<Space>... specs) {
        Specification<Space> combined = null;
        for (Specification<Space> s : specs) {
            if (s == null) continue;
            combined = (combined == null) ? s : combined.and(s);
        }
        return combined;
    }
}
