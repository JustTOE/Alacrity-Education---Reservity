package dev.tmmc.reservity.organization.mapper;

import dev.tmmc.reservity.organization.dto.MembershipResponse;
import dev.tmmc.reservity.organization.dto.OrganizationResponse;
import dev.tmmc.reservity.organization.entity.OrgMembership;
import dev.tmmc.reservity.organization.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    OrganizationResponse toResponse(Organization org);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userHandle", source = "user.handle")
    @Mapping(target = "userDisplayName", source = "user.displayName")
    @Mapping(target = "orgId", source = "organization.id")
    MembershipResponse toMembershipResponse(OrgMembership membership);
}
