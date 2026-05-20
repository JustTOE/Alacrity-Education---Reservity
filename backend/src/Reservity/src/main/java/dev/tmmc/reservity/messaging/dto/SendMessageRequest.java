package dev.tmmc.reservity.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /api/conversations/{id}/messages}. The DB CHECK
 * {@code char_length(trim(content)) > 0} is the safety net behind the
 * {@code @NotBlank} bean validation.
 */
public record SendMessageRequest(
        @NotBlank @Size(max = 4000) String content
) {}
