package dev.tmmc.reservity.spaces.mapper;

import dev.tmmc.reservity.spaces.dto.BuildingResponse;
import dev.tmmc.reservity.spaces.entity.Building;
import org.springframework.stereotype.Component;

@Component
public class BuildingMapper {

    public BuildingResponse toResponse(Building b) {
        if (b == null) return null;
        BuildingResponse.Pin pin = (b.getPinX() != null && b.getPinY() != null)
                ? new BuildingResponse.Pin(b.getPinX(), b.getPinY())
                : null;
        return new BuildingResponse(
                b.getId(),
                b.getName(),
                b.getShortCode(),
                b.getCampusName(),
                b.getAddress(),
                b.getLatitude(),
                b.getLongitude(),
                pin,
                b.getHeroImageUrl(),
                b.getDescription()
        );
    }
}
