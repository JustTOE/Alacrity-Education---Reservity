package dev.tmmc.reservity.events;

import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.common.exception.IllegalStatusTransitionException;
import dev.tmmc.reservity.events.dto.EventCreateRequest;
import dev.tmmc.reservity.events.entity.Event;
import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventHostType;
import dev.tmmc.reservity.events.entity.enums.EventStatus;
import dev.tmmc.reservity.events.entity.enums.EventVisibility;
import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.events.repository.EventRsvpRepository;
import dev.tmmc.reservity.events.service.EventService;
import dev.tmmc.reservity.messaging.repository.ConversationParticipantRepository;
import dev.tmmc.reservity.messaging.repository.ConversationRepository;
import dev.tmmc.reservity.messaging.repository.MessageRepository;
import dev.tmmc.reservity.notifications.repository.NotificationRepository;
import dev.tmmc.reservity.organization.entity.MembershipRole;
import dev.tmmc.reservity.organization.entity.OrgMembership;
import dev.tmmc.reservity.organization.entity.OrgMembershipId;
import dev.tmmc.reservity.organization.entity.Organization;
import dev.tmmc.reservity.organization.entity.OrgType;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.organization.repository.OrganizationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRepository;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.reservations.repository.ReservationSeriesRepository;
import dev.tmmc.reservity.reservations.repository.SavedPassRepository;
import dev.tmmc.reservity.spaces.SpaceTestFixtures;
import dev.tmmc.reservity.spaces.repository.*;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EventServiceTest {

    @Autowired private EventService service;
    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRsvpRepository eventRsvpRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrgMembershipRepository orgMembershipRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private ConversationParticipantRepository participantRepository;
    @Autowired private ConversationRepository conversationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private SavedPassRepository savedPassRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationRequestRepository requestRepository;
    @Autowired private ReservationSeriesRepository seriesRepository;
    @Autowired private SpaceClosureRepository closureRepository;
    @Autowired private SpaceFavoriteRepository favoriteRepository;
    @Autowired private SpaceWaitlistRepository waitlistRepository;
    @Autowired private SpaceVibeRepository spaceVibeRepository;
    @Autowired private SpaceImageRepository spaceImageRepository;
    @Autowired private SpaceRepository spaceRepository;
    @Autowired private BuildingRepository buildingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanState() {
        eventRsvpRepository.deleteAll();
        eventRepository.deleteAll();
        messageRepository.deleteAll();
        participantRepository.deleteAll();
        conversationRepository.deleteAll();
        notificationRepository.deleteAll();
        savedPassRepository.deleteAll();
        reservationRepository.deleteAll();
        requestRepository.deleteAll();
        seriesRepository.deleteAll();
        closureRepository.deleteAll();
        favoriteRepository.deleteAll();
        waitlistRepository.deleteAll();
        spaceVibeRepository.deleteAll();
        spaceImageRepository.deleteAll();
        spaceRepository.deleteAll();
        orgMembershipRepository.deleteAll();
        organizationRepository.deleteAll();
        buildingRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void create_with_USER_host_succeeds_and_assigns_slug() {
        User host = fixtures.ownerUser("host-create@test.local", "hostc");
        EventCreateRequest req = baseRequest(host.getId(), "Test recital");
        Event event = service.create(req, host.getId());
        assertNotNull(event.getId());
        assertNotNull(event.getSlug());
        assertTrue(event.getSlug().startsWith("ev-test-recital-"));
        assertEquals(EventStatus.PUBLISHED, event.getStatus());
        assertEquals(0, event.getAttendeesCount());
    }

    @Test
    void create_with_ORGANIZATION_host_when_caller_is_member_succeeds() {
        User host = fixtures.ownerUser("orgmember@test.local", "orgmem");
        Organization org = saveOrg("test-arts");
        orgMembershipRepository.save(OrgMembership.builder()
                .id(new OrgMembershipId(host.getId(), org.getId()))
                .user(host)
                .organization(org)
                .role(MembershipRole.MEMBER)
                .joinedAt(Instant.now())
                .build());

        EventCreateRequest req = new EventCreateRequest(
                "Org talk", "Members only", null, null,
                Instant.now().plus(Duration.ofDays(2)),
                Instant.now().plus(Duration.ofDays(2)).plus(Duration.ofHours(1)),
                EventHostType.ORGANIZATION, org.getId(),
                null, null, null, "Auditorium 1",
                EventCategory.TALK, "Free", 50, false, EventVisibility.PUBLIC);
        Event event = service.create(req, host.getId());
        assertEquals(EventHostType.ORGANIZATION, event.getHostType());
        assertEquals(org.getId(), event.getHostId());
    }

    @Test
    void create_with_ORGANIZATION_host_when_caller_is_NOT_member_throws() {
        User stranger = fixtures.ownerUser("stranger-org@test.local", "strorg");
        Organization org = saveOrg("private-org");
        EventCreateRequest req = new EventCreateRequest(
                "Org talk", "Members only", null, null,
                Instant.now().plus(Duration.ofDays(2)),
                Instant.now().plus(Duration.ofDays(2)).plus(Duration.ofHours(1)),
                EventHostType.ORGANIZATION, org.getId(),
                null, null, null, "Auditorium 1",
                EventCategory.TALK, "Free", 50, false, EventVisibility.PUBLIC);
        assertThrows(ForbiddenOperationException.class,
                () -> service.create(req, stranger.getId()));
    }

    @Test
    void create_with_USER_host_other_than_caller_throws() {
        User caller = fixtures.ownerUser("caller-other@test.local", "callo");
        User other = fixtures.ownerUser("other-host@test.local", "othh");
        EventCreateRequest req = baseRequest(other.getId(), "Sneaky event");
        assertThrows(ForbiddenOperationException.class,
                () -> service.create(req, caller.getId()));
    }

    @Test
    void create_with_endsAt_before_startsAt_throws() {
        User host = fixtures.ownerUser("bad-time@test.local", "badt");
        Instant start = Instant.now().plus(Duration.ofDays(1));
        EventCreateRequest req = new EventCreateRequest(
                "Bad event", "blurb", null, null,
                start, start.minus(Duration.ofMinutes(1)),
                EventHostType.USER, host.getId(),
                null, null, null, null,
                EventCategory.OTHER, null, 0, false, EventVisibility.PUBLIC);
        assertThrows(IllegalArgumentException.class,
                () -> service.create(req, host.getId()));
    }

    @Test
    void cancel_as_host_flips_status() {
        User host = fixtures.ownerUser("cancel-host@test.local", "canh");
        Event created = service.create(baseRequest(host.getId(), "Cancellable"), host.getId());
        Event cancelled = service.cancel(created.getId(), "Got sick", host.getId());
        assertEquals(EventStatus.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
        assertEquals("Got sick", cancelled.getCancellationReason());
    }

    @Test
    void cancel_already_cancelled_throws_409() {
        User host = fixtures.ownerUser("twice-cancel@test.local", "twcn");
        Event created = service.create(baseRequest(host.getId(), "Cancel twice"), host.getId());
        service.cancel(created.getId(), null, host.getId());
        assertThrows(IllegalStatusTransitionException.class,
                () -> service.cancel(created.getId(), null, host.getId()));
    }

    @Test
    void cancel_by_non_host_throws_403() {
        User host = fixtures.ownerUser("cn-host@test.local", "cnh");
        User stranger = fixtures.ownerUser("cn-stranger@test.local", "cns");
        Event created = service.create(baseRequest(host.getId(), "Locked"), host.getId());
        assertThrows(ForbiddenOperationException.class,
                () -> service.cancel(created.getId(), null, stranger.getId()));
    }

    @Test
    void getBySlugOrId_by_slug_finds_event() {
        User host = fixtures.ownerUser("getby-host@test.local", "gbh");
        Event created = service.create(baseRequest(host.getId(), "Findable"), host.getId());
        var resp = service.getBySlugOrId(created.getSlug(), host.getId());
        assertEquals(created.getId(), resp.id());
        assertEquals(created.getSlug(), resp.slug());
    }

    @Test
    void getBySlugOrId_unknown_throws_404() {
        assertThrows(EntityNotFoundException.class,
                () -> service.getBySlugOrId("nonexistent-slug", null));
    }

    private EventCreateRequest baseRequest(UUID hostId, String title) {
        Instant start = Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofHours(1));
        Instant end = start.plus(Duration.ofHours(2));
        return new EventCreateRequest(
                title, "blurb for " + title, null, null,
                start, end,
                EventHostType.USER, hostId,
                null, null, null, "Auditorium",
                EventCategory.PERFORMANCE, "Open", 20, false, EventVisibility.PUBLIC);
    }

    private Organization saveOrg(String slug) {
        return organizationRepository.save(Organization.builder()
                .slug(slug)
                .name("Test Org " + slug)
                .shortName("Org-" + slug)
                .orgType(OrgType.OTHER)
                .verified(false)
                .build());
    }
}
