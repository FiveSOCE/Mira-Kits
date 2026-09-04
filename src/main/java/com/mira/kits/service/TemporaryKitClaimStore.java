package com.mira.kits.service;

import com.mira.kits.MiraKitsPlugin;
import com.mira.kits.util.KitText;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class TemporaryKitClaimStore {
    private final MiraKitsPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public TemporaryKitClaimStore(MiraKitsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "temporary-claims.yml");
        reload();
    }

    public synchronized void reload() {
        plugin.getDataFolder().mkdirs();
        config = YamlConfiguration.loadConfiguration(file);
    }

    public synchronized boolean claimed(UUID playerId, String kitId) {
        return config.getBoolean(path(playerId, kitId), false);
    }

    public synchronized boolean markClaimed(UUID playerId, String kitId) {
        String path = path(playerId, kitId);
        if (config.getBoolean(path, false)) return false;
        config.set(path, true);
        save();
        return true;
    }

    public synchronized void clearKit(String kitId) {
        String normalized = KitText.normalizeId(kitId);
        if (normalized.isBlank()) return;

        boolean dirty = false;
        var players = config.getConfigurationSection("players");
        if (players != null) {
            for (String uuid : players.getKeys(false)) {
                String path = "players." + uuid + "." + normalized;
                if (config.contains(path)) {
                    config.set(path, null);
                    dirty = true;
                }
            }
        }
        if (dirty) save();
    }

    public synchronized Set<String> claimedKits(UUID playerId) {
        var section = config.getConfigurationSection("players." + playerId);
        if (section == null) return Set.of();

        Set<String> result = new LinkedHashSet<>();
        for (String kit : section.getKeys(false)) {
            if (section.getBoolean(kit, false)) result.add(kit);
        }
        return Set.copyOf(result);
    }

    private String path(UUID playerId, String kitId) {
        return "players." + playerId + "." + KitText.normalizeId(kitId);
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), exception);
        }
    }
}
