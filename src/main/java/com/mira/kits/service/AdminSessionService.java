package com.mira.kits.service;

import com.mira.kits.model.AdminEditSession;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AdminSessionService {
    private final Map<UUID, AdminEditSession> sessions = new ConcurrentHashMap<>();

    public void put(UUID playerId, AdminEditSession session) {
        sessions.put(playerId, session);
    }

    public Optional<AdminEditSession> get(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public void clear(UUID playerId) {
        sessions.remove(playerId);
    }
}
