package dev.tmmc.reservity.spaces.controller;

import dev.tmmc.reservity.spaces.dto.BuildingResponse;
import dev.tmmc.reservity.spaces.dto.SpaceSummary;
import dev.tmmc.reservity.spaces.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;

    @GetMapping
    public List<BuildingResponse> list() {
        return buildingService.listAll();
    }

    @GetMapping("/{slugOrId}")
    public BuildingResponse get(@PathVariable String slugOrId) {
        return buildingService.getBySlugOrId(slugOrId);
    }

    @GetMapping("/{id}/spaces")
    public List<SpaceSummary> spaces(@PathVariable UUID id) {
        return buildingService.spacesIn(id);
    }
}
