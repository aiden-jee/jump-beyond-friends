package com.aidenjee.jumpbeyondfriends;

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

    @Column(nullable = false, length = 50)
    private String name;

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

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
