package com.aidenjee.jumpbeyondfriends.player;

import java.time.Instant;

import org.apache.logging.log4j.CloseableThreadContext;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity 
@Table(name="players")
public class Player {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "google_sub", nullable = false, unique = true)
    private String googleSub;

    @Column(name = "avatar_url")
    private String avatarUrl;

    protected Player() {}

    public Player(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be blank");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("Player name cannot exceed 50 characters");
        }
        this.name = name;
    }

    public Player(String name, String googleSub, String avatarUrl) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be blank");
        }
        this.name = name;
        this.googleSub = googleSub;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
