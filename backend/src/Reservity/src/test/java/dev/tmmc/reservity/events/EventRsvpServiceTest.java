package dev.tmmc.reservity.events;

import dev.tmmc.reservity.auth.repository.RefreshTokenRepository;
import dev.tmmc.reservity.common.exception.IllegalStatusTransitionException;
import dev.tmmc.reservity.events.dto.EventCreateRequest;
import dev.tmmc.reservity.events.entity.Event;
import dev.tmmc.reservity.events.entity.EventRsvp;
import dev.tmmc.reservity.events.entity.enums.EventCategory;
import dev.tmmc.reservity.events.entity.enums.EventHostType;
import dev.tmmc.reservity.events.entity.enums.EventVisibility;
import dev.tmmc.reservity.events.entity.enums.RsvpStatus;
import dev.tmmc.reservity.events.repository.EventRepository;
import dev.tmmc.reservity.events.repository.EventRsvpRepository;
import dev.tmmc.reservity.events.service.EventRsvpService;
import dev.tmmc.reservity.events.service.EventService;
import dev.tmmc.reservity.messaging.repository.ConversationParticipantRepository;
import dev.tmmc.reservity.messaging.repository.ConversationRepository;
import dev.tmmc.reservity.messaging.repository.MessageRepository;
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

@SpringBootTest
@ActiveProfiles("test")
class EventRsvpServiceTest {

    @Autowired private EventService eventService;
    @Autowired private EventRsvpService rsvpService;
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
    void first_GOING_under_capacity_lands_GOING() {
        Event event = createEvent(2, "rsvp-cap2-a");
        User attendee = fixtures.ownerUser("att-a@test.local", "atta");
        EventRsvp rsvp = rsvpService.rsvp(event.getId(), attendee.getId(), RsvpStatus.GOING);
        assertEquals(RsvpStatus.GOING, rsvp.getStatus());
        assertEquals(1, eventRepository.findById(event.getId()).orElseThrow().getAttendeesCount());
    }

    @Test
    void GOING_at_capacity_lands_WAITLIST() {
        Event event = createEvent(1, "rsvp-cap1");
        User a = fixtures.ownerUser("att-cap-a@test.local", "atca");
        User b = fixtures.ownerUser("att-cap-b@test.local", "atcb");
        rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.GOING);
        EventRsvp bRsvp = rsvpService.rsvp(event.getId(), b.getId(), RsvpStatus.GOING);
        assertEquals(RsvpStatus.WAITLIST, bRsvp.getStatus());
        // attendees_count remains 1 — only GOING counts.
        assertEquals(1, eventRepository.findById(event.getId()).orElseThrow().getAttendeesCount());
    }

    @Test
    void cancel_a_GOING_decrements_count_and_promotes_oldest_WAITLIST() {
        Event event = createEvent(1, "rsvp-promote");
        User a = fixtures.ownerUser("att-pr-a@test.local", "atpa");
        User b = fixtures.ownerUser("att-pr-b@test.local", "atpb");
        User c = fixtures.ownerUser("att-pr-c@test.local", "atpc");
        rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.GOING);   // GOING
        rsvpService.rsvp(event.getId(), b.getId(), RsvpStatus.GOING);   // WAITLIST (older)
        rsvpService.rsvp(event.getId(), c.getId(), RsvpStatus.GOING);   // WAITLIST (newer)

        // A leaves; B should be auto-promoted.
        rsvpService.cancel(event.getId(), a.getId());

        EventRsvp aRow = eventRsvpRepository.findByEventIdAndUserId(event.getId(), a.getId())
                .orElseThrow();
        assertEquals(RsvpStatus.CANCELLED, aRow.getStatus());

        EventRsvp bRow = eventRsvpRepository.findByEventIdAndUserId(event.getId(), b.getId())
                .orElseThrow();
        assertEquals(RsvpStatus.GOING, bRow.getStatus(),
                "Oldest WAITLIST (B) should be auto-promoted");

        EventRsvp cRow = eventRsvpRepository.findByEventIdAndUserId(event.getId(), c.getId())
                .orElseThrow();
        assertEquals(RsvpStatus.WAITLIST, cRow.getStatus(),
                "C remains on the WAITLIST");

        assertEquals(1, eventRepository.findById(event.getId()).orElseThrow().getAttendeesCount());
    }

    @Test
    void capacity_zero_means_unlimited_GOING() {
        Event event = createEvent(0, "rsvp-unlim");
        User a = fixtures.ownerUser("att-unl-a@test.local", "atula");
        User b = fixtures.ownerUser("att-unl-b@test.local", "atulb");
        rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.GOING);
        rsvpService.rsvp(event.getId(), b.getId(), RsvpStatus.GOING);
        assertEquals(2, eventRepository.findById(event.getId()).orElseThrow().getAttendeesCount());
    }

    @Test
    void idempotent_repeat_GOING_does_not_double_count() {
        Event event = createEvent(0, "rsvp-idem");
        User a = fixtures.ownerUser("att-idm@test.local", "atidm");
        rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.GOING);
        rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.GOING);
        assertEquals(1, eventRepository.findById(event.getId()).orElseThrow().getAttendeesCount());
    }

    @Test
    void rsvp_on_CANCELLED_event_throws_409() {
        Event event = createEvent(0, "rsvp-onclled");
        eventService.cancel(event.getId(), "cancelled", findHostUserId(event));
        User a = fixtures.ownerUser("att-onc@test.local", "atonc");
        assertThrows(IllegalStatusTransitionException.class,
                () -> rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.GOING));
    }

    @Test
    void rsvp_with_WAITLIST_status_directly_throws_400() {
        Event event = createEvent(1, "rsvp-direct-wl");
        User a = fixtures.ownerUser("att-dwl@test.local", "atdwl");
        // WAITLIST is server-assigned only; clients cannot request it directly.
        assertThrows(IllegalArgumentException.class,
                () -> rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.WAITLIST));
    }

    @Test
    void INTERESTED_does_not_increment_attendees() {
        Event event = createEvent(0, "rsvp-interested");
        User a = fixtures.ownerUser("att-int@test.local", "atint");
        EventRsvp r = rsvpService.rsvp(event.getId(), a.getId(), RsvpStatus.INTERESTED);
        assertEquals(RsvpStatus.INTERESTED, r.getStatus());
        assertEquals(0, eventRepository.findById(event.getId()).orElseThrow().getAttendeesCount());
    }

    private Event createEvent(int capacity, String slugSuffix) {
        User host = fixtures.ownerUser("host-" + slugSuffix + "@test.local",
                truncateHandle("h" + slugSuffix));
        Instant start = Instant.now().plus(Duration.ofDays(1));
        Instant end = start.plus(Duration.ofHours(2));
        EventCreateRequest req = new EventCreateRequest(
                "Event " + slugSuffix, "blurb", null, null,
                start, end,
                EventHostType.USER, host.getId(),
                null, null, null, "Auditorium",
                EventCategory.PERFORMANCE, "Open", capacity, false, EventVisibility.PUBLIC);
        return eventService.create(req, host.getId());
    }

    private UUID findHostUserId(Event event) {
        return event.getHostId();
    }

    private static String truncateHandle(String h) {
        // handle ~ '^[a-z0-9_]{3,30}$' — keep 3-30 chars, strip non-conforming.
        String safe = h.toLowerCase().replaceAll("[^a-z0-9_]", "");
        if (safe.length() < 3) safe = "h" + safe + "x";
        if (safe.length() > 30) safe = safe.substring(0, 30);
        return safe;
    }
}
