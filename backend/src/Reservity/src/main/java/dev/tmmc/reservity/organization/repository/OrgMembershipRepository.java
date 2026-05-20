package dev.tmmc.reservity.organization.repository;

import dev.tmmc.reservity.organization.entity.MembershipRole;
import dev.tmmc.reservity.organization.entity.OrgMembership;
import dev.tmmc.reservity.organization.entity.OrgMembershipId;
import dev.tmmc.reservity.organization.entity.Organization;
import dev.tmmc.reservity.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgMembershipRepository extends JpaRepository<OrgMembership, OrgMembershipId> {

    List<OrgMembership> findByOrganization(Organization organization);

    List<OrgMembership> findByUser(User user);

    Optional<OrgMembership> findByUserAndOrganization(User user, Organization organization);

    boolean existsByUserAndOrganization(User user, Organization organization);

    /**
     * Resolve the OWNER user for an organization. V3 schema requires exactly
     * one OWNER role per org, so this returns the first match.
     */
    @Query("""
            SELECT m.user.id
              FROM OrgMembership m
             WHERE m.organization.id = :orgId
               AND m.role = :role
            """)
    Optional<UUID> findUserIdByOrganizationIdAndRole(@Param("orgId") UUID orgId,
                                                     @Param("role") MembershipRole role);
}
