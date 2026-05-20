package dev.tmmc.reservity.spaces.mapper;

import dev.tmmc.reservity.spaces.dto.VibeResponse;
import dev.tmmc.reservity.spaces.entity.Vibe;
import org.springframework.stereotype.Component;

@Component
public class VibeMapper {

    public VibeResponse toResponse(Vibe v) {
        if (v == null) return null;
        return new VibeResponse(v.getId(), v.getLabel(), v.getIcon(), v.getDescription());
    }
}
