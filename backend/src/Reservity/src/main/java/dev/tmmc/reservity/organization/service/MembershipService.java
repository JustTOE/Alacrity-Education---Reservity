package dev.tmmc.reservity.organization.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.organization.dto.AddMemberRequest;
import dev.tmmc.reservity.organization.dto.MembershipResponse;
import dev.tmmc.reservity.organization.dto.UpdateMemberRoleRequest;
import dev.tmmc.reservity.organization.entity.MembershipRole;
import dev.tmmc.reservity.organization.entity.OrgMembership;
import dev.tmmc.reservity.organization.entity.OrgMembershipId;
import dev.tmmc.reservity.organization.entity.Organization;
import dev.tmmc.reservity.organization.mapper.OrganizationMapper;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final OrgMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final OrganizationMapper organizationMapper;

    @Transactional(readOnly = true)
    public List<MembershipResponse> listForOrg(UUID actorId, UUID orgId) {
        Organization org = organizationService.loadBySlugOrId(orgId.toString());
        return membershipRepository.findByOrganization(org).stream()
                .map(organizationMapper::toMembershipResponse)
                .toList();
    }

    @Transactional
    public MembershipResponse add(UUID actorId, UUID orgId, AddMemberRequest req) {
        Organization org = organizationService.loadBySlugOrId(orgId.toString());
        organizationService.requireAdmin(actorId, org);

        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (membershipRepository.existsByUserAndOrganization(user, org)) {
            throw new DataIntegrityViolationException("User already a member");
        }

        MembershipRole role = req.role() != null ? req.role() : MembershipRole.MEMBER;
        if (role == MembershipRole.OWNER) {
            throw new ForbiddenOperationException("Cannot directly assign OWNER role");
        }

        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new EntityNotFoundException("Actor not found"));

        OrgMembership m = OrgMembership.builder()
                .id(new OrgMembershipId(user.getId(), org.getId()))
                .user(user)
                .organization(org)
                .role(role)
                .invitedBy(actor)
                .build();
        return organizationMapper.toMembershipResponse(membershipRepository.save(m));
    }

    @Transactional
    public void remove(UUID actorId, UUID orgId, UUID userId) {
        Organization org = organizationService.loadBySlugOrId(orgId.toString());
        organizationService.requireAdmin(actorId, org);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        OrgMembership m = membershipRepository.findByUserAndOrganization(user, org)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found"));

        if (m.getRole() == MembershipRole.OWNER) {
            throw new ForbiddenOperationException("Cannot remove the org owner — transfer ownership first");
        }
        membershipRepository.delete(m);
    }

    @Transactional
    public MembershipResponse updateRole(UUID actorId, UUID orgId, UUID userId, UpdateMemberRoleRequest req) {
        Organization org = organizationService.loadBySlugOrId(orgId.toString());
        organizationService.requireOwner(actorId, org);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        OrgMembership m = membershipRepository.findByUserAndOrganization(user, org)
                .orElseThrow(() -> new EntityNotFoundException("Membership not found"));

        if (req.role() == MembershipRole.OWNER) {
            throw new ForbiddenOperationException("Use transfer-ownership endpoint to change OWNER");
        }
        if (m.getRole() == MembershipRole.OWNER) {
            throw new ForbiddenOperationException("Cannot demote OWNER directly");
        }
        m.setRole(req.role());
        return organizationMapper.toMembershipResponse(membershipRepository.save(m));
    }
}
