package com.aidenjee.jumpbeyondfriends.rsvp;

import com.aidenjee.jumpbeyondfriends.game.Game;
import com.aidenjee.jumpbeyondfriends.player.Player;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity 
@Table(name="rsvps")
public class Rsvp {
    @EmbeddedId
    private RsvpId id;

    @MapsId("playerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private RsvpStatus status;

    protected Rsvp() {}

    public Rsvp(Player player, Game game, RsvpStatus status) {
        this.player = player;
        this.game = game;
        this.id = new RsvpId(player.getId(), game.getId());
        this.status = status;
    }

    public RsvpId getId() {
        return id;
    }

    public RsvpStatus getStatus() {
        return status;
    }

    public void changeStatus(RsvpStatus status) {
        this.status = status;
    }
}
