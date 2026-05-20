package dev.tmmc.reservity.notifications.service;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.notifications.dto.NotificationResponse;
import dev.tmmc.reservity.notifications.entity.Notification;
import dev.tmmc.reservity.notifications.entity.enums.NotificationChannel;
import dev.tmmc.reservity.notifications.entity.enums.NotificationType;
import dev.tmmc.reservity.notifications.mapper.NotificationMapper;
import dev.tmmc.reservity.notifications.repository.NotificationRepository;
import dev.tmmc.reservity.user.entity.User;
import dev.tmmc.reservity.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;
    private final UserRepository userRepository;

    @Transactional
    public Notification createInApp(UUID userId,
                                    NotificationType type,
                                    String title,
                                    String body,
                                    String linkUrl,
                                    String relatedType,
                                    UUID relatedId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
        Notification n = Notification.builder()
                .user(user)
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .title(title)
                .body(body)
                .linkUrl(linkUrl)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .build();
        return repository.save(n);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listForUser(UUID userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> result = unreadOnly
                ? repository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(result.map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID userId, UUID notificationId) {
        Notification n = repository.findFirstByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification", notificationId));
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
        }
    }

    @Transactional
    public int markAllRead(UUID userId) {
        return repository.markAllRead(userId, Instant.now());
    }

    @Transactional
    public void delete(UUID userId, UUID notificationId) {
        Notification n = repository.findFirstByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification", notificationId));
        repository.delete(n);
    }
}
