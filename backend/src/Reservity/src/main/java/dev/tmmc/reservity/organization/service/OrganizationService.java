package dev.tmmc.reservity.organization.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.organization.dto.OrganizationCreateRequest;
import dev.tmmc.reservity.organization.dto.OrganizationResponse;
import dev.tmmc.reservity.organization.dto.OrganizationUpdateRequest;
import dev.tmmc.reservity.organization.entity.MembershipRole;
import dev.tmmc.reservity.organization.entity.OrgMembership;
import dev.tmmc.reservity.organization.entity.OrgMembershipId;
import dev.tmmc.reservity.organization.entity.OrgType;
import dev.tmmc.reservity.organization.entity.Organization;
import dev.tmmc.reservity.organization.mapper.OrganizationMapper;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.organization.repository.OrganizationRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrgMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrganizationMapper organizationMapper;

    @Transactional(readOnly = true)
    public Page<OrganizationResponse> list(Pageable pageable) {
        return organizationRepository.findAll(pageable).map(organizationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(String slugOrId) {
        return organizationMapper.toResponse(loadBySlugOrId(slugOrId));
    }

    @Transactional
    public OrganizationResponse create(UUID creatorId, OrganizationCreateRequest req) {
        if (organizationRepository.existsBySlug(req.slug())) {
            throw new DataIntegrityViolationException("Slug already taken");
        }
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Organization org = Organization.builder()
                .slug(req.slug())
                .name(req.name())
                .shortName(req.shortName())
                .description(req.description())
                .websiteUrl(req.websiteUrl())
                .coverGradient(req.coverGradient())
                .orgType(req.orgType() != null ? req.orgType() : OrgType.OTHER)
                .verified(false)
                .build();
        org = organizationRepository.save(org);

        // Make the creator the OWNER.
        OrgMembership ownerMembership = OrgMembership.builder()
                .id(new OrgMembershipId(creator.getId(), org.getId()))
                .user(creator)
                .organization(org)
                .role(MembershipRole.OWNER)
                .build();
        membershipRepository.save(ownerMembership);

        return organizationMapper.toResponse(org);
    }

    @Transactional
    public OrganizationResponse update(UUID actorId, UUID orgId, OrganizationUpdateRequest req) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
        requireAdmin(actorId, org);

        if (req.name() != null) org.setName(req.name());
        if (req.shortName() != null) org.setShortName(req.shortName());
        if (req.description() != null) org.setDescription(req.description());
        if (req.websiteUrl() != null) org.setWebsiteUrl(req.websiteUrl());
        if (req.coverGradient() != null) org.setCoverGradient(req.coverGradient());
        if (req.orgType() != null) org.setOrgType(req.orgType());
        return organizationMapper.toResponse(organizationRepository.save(org));
    }

    @Transactional
    public void delete(UUID actorId, UUID orgId) {
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
        requireOwner(actorId, org);
        organizationRepository.delete(org);
    }

    public Organization loadBySlugOrId(String slugOrId) {
        try {
            UUID id = UUID.fromString(slugOrId);
            return organizationRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
        } catch (IllegalArgumentException notUuid) {
            return organizationRepository.findBySlug(slugOrId)
                    .orElseThrow(() -> new EntityNotFoundException("Organization not found"));
        }
    }

    public void requireAdmin(UUID actorId, Organization org) {
        OrgMembership m = membershipRepository
                .findByUserAndOrganization(loadUser(actorId), org)
                .orElseThrow(() -> new ForbiddenOperationException("Not a member of this organization"));
        if (m.getRole() != MembershipRole.OWNER && m.getRole() != MembershipRole.ADMIN) {
            throw new ForbiddenOperationException("Admin role required");
        }
    }

    public void requireOwner(UUID actorId, Organization org) {
        OrgMembership m = membershipRepository
                .findByUserAndOrganization(loadUser(actorId), org)
                .orElseThrow(() -> new ForbiddenOperationException("Not a member of this organization"));
        if (m.getRole() != MembershipRole.OWNER) {
            throw new ForbiddenOperationException("Owner role required");
        }
    }

    private User loadUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
