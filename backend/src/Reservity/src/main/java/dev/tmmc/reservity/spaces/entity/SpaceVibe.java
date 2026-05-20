package dev.tmmc.reservity.spaces.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "space_vibes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceVibe {

    @EmbeddedId
    private SpaceVibeId id;

    @MapsId("spaceId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id")
    private Space space;

    @MapsId("vibeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vibe_id")
    private Vibe vibe;
}
