package dev.tmmc.reservity.spaces.mapper;

import dev.tmmc.reservity.spaces.dto.SpaceImageResponse;
import dev.tmmc.reservity.spaces.entity.SpaceImage;
import org.springframework.stereotype.Component;

@Component
public class SpaceImageMapper {

    public SpaceImageResponse toResponse(SpaceImage img) {
        if (img == null) return null;
        return new SpaceImageResponse(
                img.getId(),
                img.getUrl(),
                img.getAltText(),
                img.getWidth(),
                img.getHeight(),
                img.getDisplayOrder(),
                img.isPrimary()
        );
    }
}
