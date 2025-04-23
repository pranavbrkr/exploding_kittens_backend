// com.kitten.lobby.dto.PlayerResponse.java
package com.kitten.lobby.dto;

public class PlayerResponse {
    private String playerId;
    private String name;

    public PlayerResponse() {} // Required by Jackson

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

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public void setName(String name) {
        this.name = name;
    }
}