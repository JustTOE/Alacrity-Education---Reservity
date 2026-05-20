package dev.tmmc.reservity.user.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.user.dto.*;
import dev.tmmc.reservity.user.service.UserService;
import dev.tmmc.reservity.user.service.UserSettingsService;
import dev.tmmc.reservity.user.service.UserSocialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSettingsService settingsService;
    private final UserSocialService socialService;

    @GetMapping("/me")
    public UserResponse getMe(@AuthenticationPrincipal SecurityUser principal) {
        return userService.getById(requirePrincipal(principal));
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal SecurityUser principal,
                                 @Valid @RequestBody UserUpdateRequest req) {
        return userService.update(requirePrincipal(principal), req);
    }

    @GetMapping("/{handleOrId}")
    public UserResponse getOther(@PathVariable String handleOrId) {
        try {
            UUID id = UUID.fromString(handleOrId);
            return userService.getById(id);
        } catch (IllegalArgumentException notUuid) {
            return userService.getByHandle(handleOrId);
        }
    }

    @GetMapping("/me/settings")
    public UserSettingsResponse getSettings(@AuthenticationPrincipal SecurityUser principal) {
        return settingsService.get(requirePrincipal(principal));
    }

    @PatchMapping("/me/settings")
    public UserSettingsResponse updateSettings(@AuthenticationPrincipal SecurityUser principal,
                                               @Valid @RequestBody UserSettingsUpdateRequest req) {
        return settingsService.update(requirePrincipal(principal), req);
    }

    @GetMapping("/me/socials")
    public List<UserSocialDto> getSocials(@AuthenticationPrincipal SecurityUser principal) {
        return socialService.list(requirePrincipal(principal));
    }

    @PutMapping("/me/socials")
    public List<UserSocialDto> replaceSocials(@AuthenticationPrincipal SecurityUser principal,
                                              @Valid @RequestBody List<UserSocialDto> socials) {
        return socialService.replace(requirePrincipal(principal), socials);
    }

    private static UUID requirePrincipal(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}
