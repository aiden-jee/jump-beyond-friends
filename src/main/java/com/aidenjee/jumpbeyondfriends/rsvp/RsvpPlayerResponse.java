package com.aidenjee.jumpbeyondfriends.rsvp;

public record RsvpPlayerResponse(Long playerId, String name, String avatarUrl, RsvpStatus status) {}
