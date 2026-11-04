package com.aidenjee.jumpbeyondfriends.rsvp;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;

@Embeddable 
public class RsvpId implements Serializable {
    private Long playerId;
    private Long gameId;

    protected RsvpId() {}

    public RsvpId(Long playerId, Long gameId) {
        this.playerId = playerId;
        this.gameId = gameId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getGameId() {
        return gameId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof RsvpId other)) {
            return false;
        }

        return Objects.equals(playerId, other.playerId)
            && Objects.equals(gameId, other.gameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, gameId);
    }
}
