package dev.tmmc.reservity.spaces.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.spaces.dto.SpaceSummary;
import dev.tmmc.reservity.spaces.service.SpaceFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
public class SpaceFavoriteController {

    private final SpaceFavoriteService favoriteService;

    @PostMapping("/{slugOrId}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favorite(@AuthenticationPrincipal SecurityUser principal,
                         @PathVariable String slugOrId) {
        favoriteService.favorite(requireUserId(principal), slugOrId);
    }

    @DeleteMapping("/{slugOrId}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfavorite(@AuthenticationPrincipal SecurityUser principal,
                           @PathVariable String slugOrId) {
        favoriteService.unfavorite(requireUserId(principal), slugOrId);
    }

    @GetMapping("/favorites")
    public List<SpaceSummary> myFavorites(@AuthenticationPrincipal SecurityUser principal) {
        return favoriteService.listForUser(requireUserId(principal));
    }

    private static UUID requireUserId(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}

