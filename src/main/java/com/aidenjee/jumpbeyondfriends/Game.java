package com.aidenjee.jumpbeyondfriends;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name="games") 
public class Game {
    /* Fields */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private int threshold;

    @Column(name="start_time", nullable=false)
    private Instant startTime;

    @Column(name="end_time", nullable=false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    /* Constructors */
    protected Game() {
    }

    Game(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                "Start or End Time cannot be null"
            );
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time!");
        }
        threshold = AppConstants.DEFAULT_THRESHOLD;
        this.startTime = startTime;
        this.endTime = endTime;
        status = GameStatus.PROPOSED;
    }

    /* Getters */
    public Long getId() {
        return id;
    }

    public int getThreshold() {
        return threshold;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public GameStatus getStatus() {
        return status;
    }

    /* Setters */
    public void setThreshold(int threshold) {
        if (threshold < 2) {
            throw new IllegalArgumentException(
                "Threshold must be at least 2"
            );
        }
        this.threshold = threshold;
    }

    public void reschedule(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                "Start or End Time cannot be null"
            );
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                "End time must be after start time!"
            );
        }

        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

}
