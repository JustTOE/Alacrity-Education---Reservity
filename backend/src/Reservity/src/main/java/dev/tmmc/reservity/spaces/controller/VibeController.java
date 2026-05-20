package dev.tmmc.reservity.spaces.controller;

import dev.tmmc.reservity.spaces.dto.VibeResponse;
import dev.tmmc.reservity.spaces.service.VibeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vibes")
@RequiredArgsConstructor
public class VibeController {

    private final VibeService vibeService;

    @GetMapping
    public List<VibeResponse> list() {
        return vibeService.listActive();
    }
}
