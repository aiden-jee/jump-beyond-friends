package com.aidenjee.jumpbeyondfriends.rsvp;

public record PlayerRsvpSummary(
    Long playerId,
    RsvpStatus status
) {}
