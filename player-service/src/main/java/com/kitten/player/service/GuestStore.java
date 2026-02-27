package com.kitten.player.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory store for guest display names so lobby/game can resolve guest ids via GET /api/player/{id}.
 */
@Component
public class GuestStore {

  private final Map<String, String> guestIdToName = new ConcurrentHashMap<>();

  public void put(String guestId, String displayName) {
    guestIdToName.put(guestId, displayName);
  }

  public String get(String guestId) {
    return guestIdToName.get(guestId);
  }

  public String getOrDefault(String guestId, String defaultName) {
    return guestIdToName.getOrDefault(guestId, defaultName);
  }
}
