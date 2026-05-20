package dev.tmmc.reservity.messaging.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Body for {@code POST /api/conversations}. The other party is derived from
 * the request: if the caller is the requester, it's the space owner; if the
 * caller is the owner, it's the requester. M7 supports two-party only.
 */
public record ConversationCreateRequest(
        @NotNull UUID requestId
) {}
