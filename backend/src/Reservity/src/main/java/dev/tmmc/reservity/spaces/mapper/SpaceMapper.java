package dev.tmmc.reservity.spaces.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import dev.tmmc.reservity.spaces.dto.SpaceImageResponse;
import dev.tmmc.reservity.spaces.dto.SpaceResponse;
import dev.tmmc.reservity.spaces.dto.SpaceSummary;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceImage;
import dev.tmmc.reservity.spaces.entity.SpaceVibe;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SpaceMapper {

    private final SpaceImageMapper imageMapper;

    public SpaceResponse toResponse(Space s) {
        if (s == null) return null;
        List<SpaceImageResponse> imgList = images(s);
        return new SpaceResponse(
                s.getSlug(),
                s.getId(),
                s.getName(),
                s.getType().value(),
                s.getBuilding() != null ? s.getBuilding().getName() : null,
                s.getBuilding() != null ? s.getBuilding().getId() : null,
                s.getFloor(),
                s.getRoom(),
                s.getSeats(),
                s.getAreaSqm(),
                s.getPricePerHour(),
                displayCurrency(s),
                vibeIds(s),
                List.of(),     // booked — populated in M4
                List.of(),     // events — populated in M8
                0,             // viewing — populated in M11
                0,             // todayBookings — populated in M5
                null,          // fullyBooked — populated in M5
                s.getBlurb(),
                s.getDescription(),
                jsonStringList(s.getAmenities()),
                jsonStringList(s.getRules()),
                pin(s),
                s.isSurprise() ? Boolean.TRUE : null,
                s.isDropIn(),
                s.isInstantBook(),
                imgList,
                primaryUrlFromList(imgList)
        );
    }

    public SpaceSummary toSummary(Space s) {
        return toSummary(s, null);
    }

    public SpaceSummary toSummary(Space s, String primaryImageUrl) {
        if (s == null) return null;
        return new SpaceSummary(
                s.getSlug(),
                s.getId(),
                s.getName(),
                s.getType().value(),
                s.getBuilding() != null ? s.getBuilding().getName() : null,
                s.getSeats(),
                s.getAreaSqm(),
                s.getPricePerHour(),
                displayCurrency(s),
                vibeIds(s),
                s.getBlurb(),
                pin(s),
                s.isSurprise() ? Boolean.TRUE : null,
                s.isDropIn(),
                s.isInstantBook(),
                primaryImageUrl
        );
    }

    /**
     * Frontend renders price as "free" when zero, "$/hr" otherwise. Stored
     * currency stays ISO ("USD") for any future internationalization.
     */
    private static String displayCurrency(Space s) {
        if (s.isFree()) return "free";
        return "$/hr";
    }

    private static List<String> vibeIds(Space s) {
        if (s.getSpaceVibes() == null || s.getSpaceVibes().isEmpty()) return List.of();
        List<String> ids = new ArrayList<>(s.getSpaceVibes().size());
        for (SpaceVibe sv : s.getSpaceVibes()) {
            if (sv.getVibe() != null) ids.add(sv.getVibe().getId());
        }
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    private List<SpaceImageResponse> images(Space s) {
        if (s.getImages() == null || s.getImages().isEmpty()) return List.of();
        List<SpaceImage> sorted = new ArrayList<>(s.getImages());
        sorted.sort(Comparator.comparingInt(SpaceImage::getDisplayOrder));
        List<SpaceImageResponse> out = new ArrayList<>(sorted.size());
        for (SpaceImage img : sorted) out.add(imageMapper.toResponse(img));
        return List.copyOf(out);
    }

    private static String primaryUrlFromList(List<SpaceImageResponse> imgs) {
        if (imgs == null || imgs.isEmpty()) return null;
        for (SpaceImageResponse img : imgs) {
            if (img.isPrimary()) return img.url();
        }
        return imgs.get(0).url();
    }

    private static SpaceResponse.Pin pin(Space s) {
        if (s.getPinX() == null || s.getPinY() == null) return null;
        return new SpaceResponse.Pin(s.getPinX(), s.getPinY());
    }

    /**
     * JSONB columns are Jackson {@link JsonNode}s. Convert array-of-string
     * shapes into {@code List<String>}; anything else returns empty.
     */
    private static List<String> jsonStringList(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> out = new ArrayList<>(node.size());
        node.forEach(n -> {
            if (n.isTextual()) out.add(n.asText());
        });
        return List.copyOf(out);
    }

    @SuppressWarnings("unused")
    private static BigDecimal nonNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
