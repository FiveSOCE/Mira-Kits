package com.mira.kits.service;

import com.mira.kits.MiraKitsPlugin;

import java.time.Instant;
import java.util.Locale;

public final class KitWindowService {
    private final MiraKitsPlugin plugin;

    public KitWindowService(MiraKitsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean active(String kitId) {
        String base = base(kitId);
        if (!plugin.getConfig().getBoolean(base + ".enabled", false)) return true;
        Instant now = Instant.now();
        Instant start = parse(plugin.getConfig().getString(base + ".start", ""));
        Instant end = parse(plugin.getConfig().getString(base + ".end", ""));
        if (start != null && now.isBefore(start)) return false;
        return end == null || now.isBefore(end);
    }

    public boolean eventKit(String kitId) {
        return plugin.getConfig().getBoolean(base(kitId) + ".enabled", false);
    }

    public String status(String kitId) {
        if (!eventKit(kitId)) return "Permanent";
        String base = base(kitId);
        return active(kitId)
                ? "Active until " + plugin.getConfig().getString(base + ".end", "manual")
                : "Inactive (" + plugin.getConfig().getString(base + ".start", "?") + " -> " + plugin.getConfig().getString(base + ".end", "?") + ")";
    }

    private String base(String id) {
        return "event-kits." + id.toLowerCase(Locale.ROOT);
    }

    private Instant parse(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return Instant.parse(raw.trim()); }
        catch (Exception ex) {
            plugin.getLogger().warning("Invalid MiraKits event timestamp '" + raw + "'. Use ISO-8601 UTC.");
            return null;
        }
    }
}
