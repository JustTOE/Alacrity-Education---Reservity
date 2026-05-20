package dev.tmmc.reservity.events;

import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.events.dto.EventCreateRequest;
import dev.tmmc.reservity.events.dto.EventSummary;
import dev.tmmc.reservity.events.entity.Event;
import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventHostType;
import dev.tmmc.reservity.events.entity.enums.EventVisibility;
import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.events.repository.EventRsvpRepository;
import dev.tmmc.reservity.events.service.EventRsvpService;
import dev.tmmc.reservity.events.service.EventService;
import dev.tmmc.reservity.events.service.EventService.EventListFilters;
import dev.tmmc.reservity.notifications.repository.NotificationRepository;
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

/**
 * Visibility filter for the list endpoint:
 * <ul>
 *   <li>anonymous → only PUBLIC</li>
 *   <li>authenticated → PUBLIC + (UNLISTED where caller hosts or has RSVP)</li>
 *   <li>PRIVATE never appears in list (only direct GET)</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
class EventVisibilityFilterTest {

    @Autowired private EventService eventService;
    @Autowired private EventRsvpService rsvpService;
    @Autowired private SpaceTestFixtures fixtures;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRsvpRepository eventRsvpRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private OrgMembershipRepository orgMembershipRepository;
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
    void anonymous_sees_only_PUBLIC() {
        User host = fixtures.ownerUser("vis-host@test.local", "vish");
        Event publicE = createEvent(host, "VisPublic", EventVisibility.PUBLIC);
        createEvent(host, "VisUnlisted", EventVisibility.UNLISTED);
        createEvent(host, "VisPrivate", EventVisibility.PRIVATE);

        PageResponse<EventSummary> page = eventService.list(EventListFilters.empty(), null, 0, 50);
        assertEquals(1, page.getTotalElements());
        assertEquals(publicE.getId(), page.getContent().get(0).id());
    }

    @Test
    void host_sees_their_PUBLIC_and_UNLISTED_but_not_PRIVATE_in_list() {
        User host = fixtures.ownerUser("vis-host2@test.local", "vish2");
        createEvent(host, "VisPublic2", EventVisibility.PUBLIC);
        createEvent(host, "VisUnlisted2", EventVisibility.UNLISTED);
        createEvent(host, "VisPrivate2", EventVisibility.PRIVATE);

        PageResponse<EventSummary> page = eventService.list(EventListFilters.empty(),
                host.getId(), 0, 50);
        // PUBLIC + UNLISTED (host) but PRIVATE never shows in list.
        assertEquals(2, page.getTotalElements());
    }

    @Test
    void rsvp_promotes_visibility_of_UNLISTED() {
        User host = fixtures.ownerUser("vis-host3@test.local", "vish3");
        User other = fixtures.ownerUser("vis-other3@test.local", "viso3");
        Event unlistedE = createEvent(host, "VisUnlisted3", EventVisibility.UNLISTED);

        // Other does not see UNLISTED yet.
        PageResponse<EventSummary> before = eventService.list(EventListFilters.empty(),
                other.getId(), 0, 50);
        assertEquals(0, before.getTotalElements());

        // Other RSVPs.
        rsvpService.rsvp(unlistedE.getId(), other.getId(), RsvpStatus.GOING);

        // Now sees it.
        PageResponse<EventSummary> after = eventService.list(EventListFilters.empty(),
                other.getId(), 0, 50);
        assertEquals(1, after.getTotalElements());
    }

    @Test
    void direct_get_PRIVATE_by_non_host_non_RSVP_throws_404() {
        User host = fixtures.ownerUser("vis-host4@test.local", "vish4");
        User stranger = fixtures.ownerUser("vis-str4@test.local", "vistr4");
        Event privE = createEvent(host, "VisPrivate4", EventVisibility.PRIVATE);

        assertThrows(dev.tmmc.reservity.common.exception.EntityNotFoundException.class,
                () -> eventService.getBySlugOrId(privE.getSlug(), stranger.getId()));
    }

    @Test
    void direct_get_PRIVATE_by_host_succeeds() {
        User host = fixtures.ownerUser("vis-host5@test.local", "vish5");
        Event privE = createEvent(host, "VisPrivate5", EventVisibility.PRIVATE);

        var resp = eventService.getBySlugOrId(privE.getSlug(), host.getId());
        assertEquals(privE.getId(), resp.id());
    }

    private Event createEvent(User host, String title, EventVisibility visibility) {
        Instant start = Instant.now().plus(Duration.ofDays(1));
        Instant end = start.plus(Duration.ofHours(1));
        EventCreateRequest req = new EventCreateRequest(
                title, "blurb", null, null,
                start, end,
                EventHostType.USER, host.getId(),
                null, null, null, "Loc",
                EventCategory.OTHER, "Tag", 0, false, visibility);
        return eventService.create(req, host.getId());
    }
}
