package dev.tmmc.reservity.spaces.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.spaces.dto.WaitlistRequest;
import dev.tmmc.reservity.spaces.dto.WaitlistResponse;
import dev.tmmc.reservity.spaces.service.SpaceWaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SpaceWaitlistController {

    private final SpaceWaitlistService waitlistService;

    @PostMapping("/api/spaces/{slugOrId}/waitlist")
    public ResponseEntity<WaitlistResponse> add(@AuthenticationPrincipal SecurityUser principal,
                                                @PathVariable String slugOrId,
                                                @RequestBody(required = false) WaitlistRequest req) {
        WaitlistResponse resp = waitlistService.add(requireUserId(principal), slugOrId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @DeleteMapping("/api/spaces/{slugOrId}/waitlist/{wlId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal SecurityUser principal,
                       @PathVariable String slugOrId,
                       @PathVariable UUID wlId) {
        waitlistService.remove(requireUserId(principal), wlId);
    }

    @GetMapping("/api/users/me/waitlist")
    public List<WaitlistResponse> myWaitlist(@AuthenticationPrincipal SecurityUser principal) {
        return waitlistService.listForUser(requireUserId(principal));
    }

    private static UUID requireUserId(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}
