package dev.tmmc.reservity.organization.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.organization.dto.*;
import dev.tmmc.reservity.organization.service.MembershipService;
import dev.tmmc.reservity.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final MembershipService membershipService;

    @GetMapping
    public PageResponse<OrganizationResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(organizationService.list(pageable));
    }

    @GetMapping("/{slugOrId}")
    public OrganizationResponse get(@PathVariable String slugOrId) {
        return organizationService.get(slugOrId);
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(@AuthenticationPrincipal SecurityUser principal,
                                                       @Valid @RequestBody OrganizationCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationService.create(requirePrincipal(principal), req));
    }

    @PatchMapping("/{id}")
    public OrganizationResponse update(@AuthenticationPrincipal SecurityUser principal,
                                       @PathVariable UUID id,
                                       @Valid @RequestBody OrganizationUpdateRequest req) {
        return organizationService.update(requirePrincipal(principal), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal SecurityUser principal, @PathVariable UUID id) {
        organizationService.delete(requirePrincipal(principal), id);
    }

    @GetMapping("/{id}/members")
    public List<MembershipResponse> listMembers(@AuthenticationPrincipal SecurityUser principal,
                                                @PathVariable UUID id) {
        return membershipService.listForOrg(requirePrincipal(principal), id);
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse addMember(@AuthenticationPrincipal SecurityUser principal,
                                        @PathVariable UUID id,
                                        @Valid @RequestBody AddMemberRequest req) {
        return membershipService.add(requirePrincipal(principal), id, req);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal SecurityUser principal,
                             @PathVariable UUID id, @PathVariable UUID userId) {
        membershipService.remove(requirePrincipal(principal), id, userId);
    }

    @PatchMapping("/{id}/members/{userId}")
    public MembershipResponse updateMemberRole(@AuthenticationPrincipal SecurityUser principal,
                                               @PathVariable UUID id,
                                               @PathVariable UUID userId,
                                               @Valid @RequestBody UpdateMemberRoleRequest req) {
        return membershipService.updateRole(requirePrincipal(principal), id, userId, req);
    }

    private static UUID requirePrincipal(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}
