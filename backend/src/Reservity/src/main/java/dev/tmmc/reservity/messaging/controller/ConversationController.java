package dev.tmmc.reservity.messaging.controller;

import dev.tmmc.reservity.common.exception.EntityNotFoundException;
import dev.tmmc.reservity.common.pagination.PageResponse;
import dev.tmmc.reservity.common.security.SecurityUser;
import dev.tmmc.reservity.messaging.dto.ConversationCreateRequest;
import dev.tmmc.reservity.messaging.dto.ConversationResponse;
import dev.tmmc.reservity.messaging.dto.MessageResponse;
import dev.tmmc.reservity.messaging.dto.SendMessageRequest;
import dev.tmmc.reservity.messaging.entity.Conversation;
import dev.tmmc.reservity.messaging.entity.Message;
import dev.tmmc.reservity.messaging.mapper.MessageMapper;
import dev.tmmc.reservity.messaging.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {

    private final MessageService service;
    private final MessageMapper messageMapper;

    @GetMapping
    public PageResponse<ConversationResponse> list(
            @AuthenticationPrincipal SecurityUser principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listForUser(requireUserId(principal), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse createOrGet(
            @AuthenticationPrincipal SecurityUser principal,
            @Valid @RequestBody ConversationCreateRequest body) {
        UUID userId = requireUserId(principal);
        Conversation conv = service.findOrCreateForRequest(body.requestId(), userId);
        return service.getById(conv.getId(), userId);
    }

    @GetMapping("/{id}")
    public ConversationResponse getById(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id) {
        return service.getById(id, requireUserId(principal));
    }

    @GetMapping("/{id}/messages")
    public PageResponse<MessageResponse> listMessages(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "30") int size) {
        int capped = Math.min(Math.max(size, 1), 100);
        return service.listMessages(id, requireUserId(principal), before, capped);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest body) {
        Message msg = service.send(id, requireUserId(principal), body.content());
        return messageMapper.toResponse(msg);
    }

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @AuthenticationPrincipal SecurityUser principal,
            @PathVariable UUID id) {
        service.markRead(id, requireUserId(principal));
    }

    private static UUID requireUserId(SecurityUser principal) {
        if (principal == null) throw new EntityNotFoundException("Not authenticated");
        return principal.getUserId();
    }
}
