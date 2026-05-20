package dev.tmmc.reservity.spaces.service;

import dev.tmmc.reservity.spaces.dto.VibeResponse;
import dev.tmmc.reservity.spaces.mapper.VibeMapper;
import dev.tmmc.reservity.spaces.repository.VibeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VibeService {

    private final VibeRepository vibeRepository;
    private final VibeMapper vibeMapper;

    public List<VibeResponse> listActive() {
        return vibeRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(vibeMapper::toResponse)
                .toList();
    }
}
