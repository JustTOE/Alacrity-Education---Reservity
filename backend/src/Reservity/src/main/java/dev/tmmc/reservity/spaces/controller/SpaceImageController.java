package dev.tmmc.reservity.spaces.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.spaces.dto.ImageUpdateRequest;
import dev.tmmc.reservity.spaces.dto.SpaceImageResponse;
import dev.tmmc.reservity.spaces.entity.SpaceImage;
import dev.tmmc.reservity.spaces.mapper.SpaceImageMapper;
import dev.tmmc.reservity.spaces.service.SpaceImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SpaceImageController {

    private final SpaceImageService imageService;
    private final SpaceImageMapper imageMapper;

    @GetMapping("/api/spaces/{slugOrId}/images")
    public List<SpaceImageResponse> list(@PathVariable String slugOrId) {
        return imageService.list(slugOrId).stream()
                .map(imageMapper::toResponse)
                .toList();
    }

    @PostMapping(value = "/api/spaces/{slugOrId}/images", consumes = "multipart/form-data")
    public ResponseEntity<SpaceImageResponse> upload(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable String slugOrId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "isPrimary", required = false) Boolean isPrimary) {
        SpaceImage created = imageService.upload(slugOrId, file, altText, isPrimary, requireUserId(principal));
        return ResponseEntity.status(HttpStatus.CREATED).body(imageMapper.toResponse(created));
    }

    @PatchMapping("/api/spaces/{slugOrId}/images/{imageId}")
    public SpaceImageResponse update(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable String slugOrId,
            @PathVariable UUID imageId,
            @RequestBody ImageUpdateRequest req) {
        return imageMapper.toResponse(
                imageService.update(slugOrId, imageId, req, requireUserId(principal)));
    }

    @DeleteMapping("/api/spaces/{slugOrId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable String slugOrId,
            @PathVariable UUID imageId) {
        imageService.delete(slugOrId, imageId, requireUserId(principal));
    }

    private static UUID requireUserId(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}
