package dev.tmmc.reservity.messaging.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.exception.ForbiddenOperationException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.messaging.dto.ConversationResponse;
import dev.tmmc.reservity.messaging.dto.MessagePreview;
import dev.tmmc.reservity.messaging.dto.MessageResponse;
import dev.tmmc.reservity.messaging.entity.Conversation;
import dev.tmmc.reservity.messaging.entity.ConversationParticipant;
import dev.tmmc.reservity.messaging.entity.Message;
import dev.tmmc.reservity.messaging.event.MessageSentEvent;
import dev.tmmc.reservity.messaging.mapper.ConversationMapper;
import dev.tmmc.reservity.messaging.mapper.MessageMapper;
import dev.tmmc.reservity.messaging.repository.ConversationParticipantRepository;
import dev.tmmc.reservity.messaging.repository.ConversationRepository;
import dev.tmmc.reservity.messaging.repository.MessageRepository;
import dev.tmmc.reservity.organization.entity.MembershipRole;
import dev.tmmc.reservity.organization.repository.OrgMembershipRepository;
import dev.tmmc.reservity.reservations.entity.ReservationRequest;
import dev.tmmc.reservity.reservations.repository.ReservationRequestRepository;
import dev.tmmc.reservity.spaces.entity.Space;
import dev.tmmc.reservity.spaces.entity.SpaceOwnerType;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final int PREVIEW_MAX = 200;

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final ReservationRequestRepository requestRepository;
    private final OrgMembershipRepository orgMembershipRepository;
    private final UserRepository userRepository;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Lazy find-or-create. Caller must be either the request's requester or the
     * space's owner (USER ownership) / a member of the owning org. Anyone else
     * gets 403.
     */
    @Transactional
    public Conversation findOrCreateForRequest(UUID requestId, UUID callerUserId) {
        ReservationRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("ReservationRequest", requestId));

        UUID requesterId = req.getRequester().getId();
        UUID ownerUserId = resolveOwnerUserId(req.getSpace(), callerUserId);

        if (!callerUserId.equals(requesterId) && !callerUserId.equals(ownerUserId)) {
            throw new ForbiddenOperationException(
                    "Only the requester or space owner can open this conversation");
        }

        Optional<Conversation> existing = conversationRepository.findFirstByRequestId(requestId);
        if (existing.isPresent()) return existing.get();

        Conversation conv = conversationRepository.save(
                Conversation.builder().request(req).build());

        User caller = userRepository.findById(callerUserId)
                .orElseThrow(() -> new EntityNotFoundException("User", callerUserId));
        UUID otherId = callerUserId.equals(requesterId) ? ownerUserId : requesterId;
        User other = userRepository.findById(otherId)
                .orElseThrow(() -> new EntityNotFoundException("User", otherId));

        participantRepository.save(ConversationParticipant.builder()
                .conversation(conv).user(caller).build());
        participantRepository.save(ConversationParticipant.builder()
                .conversation(conv).user(other).build());

        return conv;
    }

    @Transactional
    public Message send(UUID conversationId, UUID senderId, String content) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));

        List<ConversationParticipant> participants = participantRepository
                .findByConversationId(conversationId);
        UUID recipientId = participants.stream()
                .map(p -> p.getUser().getId())
                .filter(id -> !id.equals(senderId))
                .findFirst()
                .orElseThrow(() -> new ForbiddenOperationException(
                        "Sender is not a participant of this conversation"));

        boolean senderIsParticipant = participants.stream()
                .anyMatch(p -> p.getUser().getId().equals(senderId));
        if (!senderIsParticipant) {
            throw new ForbiddenOperationException(
                    "Sender is not a participant of this conversation");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("User", senderId));

        String trimmed = content == null ? "" : content.trim();
        Message msg = messageRepository.save(Message.builder()
                .conversation(conv)
                .sender(sender)
                .content(trimmed)
                .build());

        conv.setUpdatedAt(Instant.now());
        conversationRepository.save(conv);

        String preview = trimmed.length() > PREVIEW_MAX
                ? trimmed.substring(0, PREVIEW_MAX) : trimmed;

        eventPublisher.publishEvent(new MessageSentEvent(
                conversationId, msg.getId(), senderId, recipientId, preview));

        return msg;
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationResponse> listForUser(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> result = conversationRepository.findForUser(userId, pageable);
        return PageResponse.of(result.map(conv -> hydrate(conv, userId)));
    }

    @Transactional(readOnly = true)
    public ConversationResponse getById(UUID conversationId, UUID callerUserId) {
        Conversation conv = requireMember(conversationId, callerUserId);
        return hydrate(conv, callerUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(UUID conversationId,
                                                      UUID callerUserId,
                                                      Instant before,
                                                      int size) {
        requireMember(conversationId, callerUserId);
        Pageable pageable = PageRequest.of(0, size);
        Page<Message> result = before == null
                ? messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
                : messageRepository.findByConversationIdAndCreatedAtLessThanOrderByCreatedAtDesc(
                        conversationId, before, pageable);
        return PageResponse.of(result.map(messageMapper::toResponse));
    }

    @Transactional
    public void markRead(UUID conversationId, UUID callerUserId) {
        requireMember(conversationId, callerUserId);
        participantRepository.markRead(conversationId, callerUserId, Instant.now());
    }

    /**
     * For USER-owned spaces returns the owner directly. For ORG-owned spaces
     * returns the org's OWNER user (V3 schema requires exactly one).
     */
    UUID resolveOwnerUserId(Space space, UUID callerUserId) {
        if (space.getOwnerType() == SpaceOwnerType.USER) {
            return space.getOwnerId();
        }
        return orgMembershipRepository
                .findUserIdByOrganizationIdAndRole(space.getOwnerId(), MembershipRole.OWNER)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organization owner", space.getOwnerId()));
    }

    private Conversation requireMember(UUID conversationId, UUID userId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            // 404 not 403 — masquerade cross-tenant as not-found, mirroring M6.
            throw new EntityNotFoundException("Conversation", conversationId);
        }
        return conv;
    }

    private ConversationResponse hydrate(Conversation conv, UUID callerUserId) {
        User other = participantRepository.findByConversationId(conv.getId()).stream()
                .map(ConversationParticipant::getUser)
                .filter(u -> !u.getId().equals(callerUserId))
                .findFirst()
                .orElse(null);

        MessagePreview preview = messageRepository
                .findFirstByConversationIdOrderByCreatedAtDesc(conv.getId())
                .map(messageMapper::toPreview)
                .orElse(null);

        Instant lastReadAt = participantRepository
                .findByConversationIdAndUserId(conv.getId(), callerUserId)
                .map(ConversationParticipant::getLastReadAt)
                .orElse(null);
        long unread = lastReadAt == null
                ? messageRepository.countAllFromOthers(conv.getId(), callerUserId)
                : messageRepository.countUnreadSince(conv.getId(), callerUserId, lastReadAt);

        return conversationMapper.toResponse(conv, other, preview, unread);
    }
}
