package com.mira.kits.model;

import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AdminEditSession {
    private final String id;
    private final boolean newKit;
    private String displayName;
    private long delaySeconds;
    private BigDecimal price;
    private boolean visible;
    private boolean enabled;
    private final Map<Integer, ItemStack> items = new LinkedHashMap<>();
    private final List<String> passthroughLines = new ArrayList<>();

    public AdminEditSession(String id, boolean newKit, String displayName, long delaySeconds,
                            BigDecimal price, boolean visible, boolean enabled) {
        this.id = id;
        this.newKit = newKit;
        this.displayName = displayName;
        this.delaySeconds = delaySeconds;
        this.price = price;
        this.visible = visible;
        this.enabled = enabled;
    }

    public String id() { return id; }
    public boolean newKit() { return newKit; }
    public String displayName() { return displayName; }
    public void displayName(String displayName) { this.displayName = displayName; }
    public long delaySeconds() { return delaySeconds; }
    public void delaySeconds(long delaySeconds) { this.delaySeconds = delaySeconds; }
    public BigDecimal price() { return price; }
    public void price(BigDecimal price) { this.price = price; }
    public boolean visible() { return visible; }
    public void visible(boolean visible) { this.visible = visible; }
    public boolean enabled() { return enabled; }
    public void enabled(boolean enabled) { this.enabled = enabled; }
    public Map<Integer, ItemStack> items() { return items; }
    public List<String> passthroughLines() { return passthroughLines; }

    public void putItem(int slot, ItemStack item) {
        if (slot < 0 || slot >= 45 || item == null || item.getType().isAir()) return;
        items.put(slot, item.clone());
    }

    public void removeItem(int slot) {
        items.remove(slot);
    }

    public void clearItems() {
        items.clear();
    }

    public int firstFreeSlot() {
        for (int slot = 0; slot < 45; slot++) {
            if (!items.containsKey(slot)) return slot;
        }
        return -1;
    }
}
