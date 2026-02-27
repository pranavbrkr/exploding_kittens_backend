package com.kitten.player.dto;

public class PlayerResponse {
    private String playerId;
    private String name;

    public PlayerResponse(String playerId, String name) {
        this.playerId = playerId;
        this.name = name;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }
}
