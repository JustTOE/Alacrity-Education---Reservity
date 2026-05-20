package dev.tmmc.reservity.spaces.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SpaceVibeId implements Serializable {

    @Column(name = "space_id", nullable = false)
    private UUID spaceId;

    @Column(name = "vibe_id", nullable = false, length = 20)
    private String vibeId;
}
