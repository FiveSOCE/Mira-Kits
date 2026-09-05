package com.mira.kits.gui;

import com.mira.core.api.MiraCore;
import com.mira.kits.MiraKitsPlugin;
import com.mira.kits.model.AdminEditSession;
import com.mira.kits.model.ClaimResult;
import com.mira.kits.model.KitMeta;
import com.mira.kits.service.AdminSessionService;
import com.mira.kits.service.EssentialsKitService;
import com.mira.kits.util.KitText;
import com.mira.kits.util.CosmeticsBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KitGuiService {
    public enum Screen { PLAYER_LIST, PLAYER_DETAIL, ADMIN_LIST, EDITOR, DELETE_CONFIRM }

    public static final class Holder implements InventoryHolder {
        private final Screen screen;
        private final String kitId;
        private final int page;
        private final Map<Integer, String> kitSlots;

        public Holder(Screen screen, String kitId, int page) {
            this(screen, kitId, page, Map.of());
        }

        public Holder(Screen screen, String kitId, int page, Map<Integer, String> kitSlots) {
            this.screen = screen;
            this.kitId = kitId;
            this.page = page;
            this.kitSlots = Map.copyOf(kitSlots);
        }

        public Screen screen() { return screen; }
        public String kitId() { return kitId; }
        public int page() { return page; }
        public String kitAt(int slot) { return kitSlots.get(slot); }
        @Override public Inventory getInventory() { return null; }
    }

    private static final int PAGE_SIZE = 45;
    private static final int PLAYER_KITS_PER_PAGE = 28;

    private final MiraKitsPlugin plugin;
    private final MiraCore core;
    private final EssentialsKitService kits;
    private final AdminSessionService sessions;

    public KitGuiService(MiraKitsPlugin plugin, MiraCore core, EssentialsKitService kits, AdminSessionService sessions) {
        this.plugin = plugin;
        this.core = core;
        this.kits = kits;
        this.sessions = sessions;
    }

    public void openPlayerList(Player player) {
        openPlayerList(player, 0);
    }

    public void openPlayerList(Player player, int page) {
        if (!player.hasPermission("mirakits.use")) {
            core.messages().send(player, "&cYou do not have permission to use kits.");
            return;
        }

        List<String> visible = kits.kitIds().stream()
                .filter(id -> {
                    KitMeta meta = kits.meta(id);
                    return meta.visible() && meta.enabled() && kits.visibleTo(player, id);
                })
                .toList();

        int maxPage = Math.max(0, (visible.size() - 1) / PLAYER_KITS_PER_PAGE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        int start = safePage * PLAYER_KITS_PER_PAGE;
        List<String> pageKits = visible.subList(start, Math.min(start + PLAYER_KITS_PER_PAGE, visible.size()));

        int contentRows = Math.max(1, (pageKits.size() + 6) / 7);
        int rows = Math.max(3, Math.min(6, contentRows + 2));
        int size = rows * 9;
        Map<Integer, String> kitSlots = buildPlayerKitSlots(pageKits, rows);

        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.PLAYER_LIST, null, safePage, kitSlots),
                size,
                Component.text(plugin.getConfig().getString("player-gui-title", "Kits"), NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.ITALIC, false)
        );

        ItemStack filler = glowingGlass();
        for (int slot = 0; slot < size; slot++) inventory.setItem(slot, filler.clone());
        kitSlots.forEach((slot, id) -> inventory.setItem(slot, kitPlaceholder(id)));

        int bottomStart = size - 9;
        if (safePage > 0) inventory.setItem(bottomStart + 1, control(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));
        inventory.setItem(bottomStart + 4, control(Material.BARRIER, "Close", NamedTextColor.RED));
        if (safePage < maxPage) inventory.setItem(bottomStart + 7, control(Material.ARROW, "Next Page", NamedTextColor.YELLOW));
        player.openInventory(inventory);
    }

    /**
     * Exact read-only kit inspection. The first 45 slots mirror the parsed Essentials kit
     * inventory positions using cloned ItemStacks, preserving names, lore, enchants, PDC,
     * quantities and all other item metadata. The bottom row contains only summary/details.
     */
    public boolean openDetail(Player player, String input) {
        String id = kits.match(input);
        if (id == null) return false;
        KitMeta meta = kits.meta(id);
        if ((!meta.visible() || !meta.enabled() || !kits.visibleTo(player, id))
                && !player.hasPermission("mirakits.admin")) return false;

        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.PLAYER_DETAIL, id, 0),
                54,
                Component.text("Inspect: " + meta.displayName(), NamedTextColor.DARK_PURPLE)
                        .decoration(TextDecoration.ITALIC, false)
        );

        kits.previewSlots(id).forEach((slot, item) -> {
            if (slot >= 0 && slot < 45 && item != null && !item.getType().isAir()) {
                inventory.setItem(slot, item.clone());
            }
        });

        ItemStack filler = glowingGlass();
        for (int slot = 45; slot < 54; slot++) inventory.setItem(slot, filler.clone());
        inventory.setItem(45, control(Material.ARROW, "Back", NamedTextColor.YELLOW));
        inventory.setItem(49, kitInfo(id));
        inventory.setItem(53, controlWithLore(Material.ENDER_CHEST, meta.displayName(), NamedTextColor.GOLD,
                List.of(
                        Component.text("Left-click this kit in /kits to claim it.", NamedTextColor.GRAY),
                        Component.text("This screen is inspection only.", NamedTextColor.DARK_GRAY)
                )));
        player.openInventory(inventory);
        return true;
    }

    public void openAdminList(Player player) { openAdminList(player, 0); }

    public void openAdminList(Player player, int page) {
        if (!player.hasPermission("mirakits.admin")) {
            core.messages().send(player, "&cYou do not have permission to administer kits.");
            return;
        }
        List<String> all = new ArrayList<>(kits.kitIds());
        int maxPage = Math.max(0, (all.size() - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.ADMIN_LIST, null, safePage), 54,
                Component.text(plugin.getConfig().getString("admin-gui-title", "Kit Administration"), NamedTextColor.DARK_RED)
                        .decoration(TextDecoration.ITALIC, false)
        );
        int start = safePage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < all.size(); i++) inventory.setItem(i, kitPlaceholder(all.get(start + i)));
        if (safePage > 0) inventory.setItem(45, control(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));
        inventory.setItem(48, control(Material.EMERALD_BLOCK, "Create Kit", NamedTextColor.GREEN));
        inventory.setItem(49, control(Material.BARRIER, "Close", NamedTextColor.RED));
        inventory.setItem(50, controlWithLore(Material.COMPARATOR, "Reload Essentials Kits", NamedTextColor.AQUA,
                List.of(Component.text("Reload kits.yml and Mira metadata.", NamedTextColor.GRAY))));
        if (safePage < maxPage) inventory.setItem(53, control(Material.ARROW, "Next Page", NamedTextColor.YELLOW));
        player.openInventory(inventory);
    }

    public void openEditor(Player player) {
        AdminEditSession session = sessions.get(player.getUniqueId()).orElse(null);
        if (session == null) { openAdminList(player); return; }
        String prefix = plugin.getConfig().getString("editor-gui-title-prefix", "Edit Kit: ");
        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.EDITOR, session.id(), 0), 54,
                Component.text(prefix + session.displayName(), NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)
        );
        session.items().forEach((slot, item) -> {
            if (slot >= 0 && slot < 45) inventory.setItem(slot, item.clone());
        });

        inventory.setItem(45, control(Material.ARROW, "Cancel", NamedTextColor.YELLOW));
        inventory.setItem(46, controlWithLore(Material.CHEST, "Use My Inventory", NamedTextColor.AQUA,
                List.of(Component.text("Copies your current inventory into this kit.", NamedTextColor.GRAY))));
        inventory.setItem(47, controlWithLore(Material.CLOCK, "Set Cooldown", NamedTextColor.AQUA,
                List.of(Component.text(cooldownLabel(session.delaySeconds()), NamedTextColor.GRAY))));
        inventory.setItem(48, controlWithLore(Material.GOLD_INGOT, "Set Price", NamedTextColor.GOLD,
                List.of(Component.text(KitText.money(session.price(), kits.currencySymbol()), NamedTextColor.GRAY))));
        inventory.setItem(49, control(session.visible() ? Material.LIME_DYE : Material.GRAY_DYE,
                session.visible() ? "Visible" : "Hidden", session.visible() ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        inventory.setItem(50, control(session.enabled() ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                session.enabled() ? "Enabled" : "Disabled", session.enabled() ? NamedTextColor.GREEN : NamedTextColor.RED));
        inventory.setItem(51, controlWithLore(Material.PAPER, "Editor Help", NamedTextColor.YELLOW,
                List.of(Component.text("Click a kit item above to remove it.", NamedTextColor.GRAY),
                        Component.text("Click an item in your inventory to copy it in.", NamedTextColor.GRAY))));
        if (!session.newKit()) inventory.setItem(52, control(Material.TNT, "Delete Kit", NamedTextColor.RED));
        inventory.setItem(53, control(Material.EMERALD_BLOCK, "Save Kit", NamedTextColor.GREEN));
        player.openInventory(inventory);
    }

    public void openDeleteConfirm(Player player) {
        AdminEditSession session = sessions.get(player.getUniqueId()).orElse(null);
        if (session == null || session.newKit()) { openAdminList(player); return; }
        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.DELETE_CONFIRM, session.id(), 0), 27,
                Component.text("Delete " + session.displayName() + "?", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false)
        );
        inventory.setItem(11, control(Material.LIME_CONCRETE, "Keep Kit", NamedTextColor.GREEN));
        inventory.setItem(15, control(Material.RED_CONCRETE, "Delete Permanently", NamedTextColor.RED));
        player.openInventory(inventory);
    }

    public void claimAndRespond(Player player, String id) {
        boolean temporary = kits.temporaryKit(player, id);
        boolean eventKit = kits.windows().eventKit(id);
        ClaimResult result = kits.claim(player, id);
        switch (result) {
            case SUCCESS -> {
                KitMeta meta = kits.meta(id);
                core.messages().send(player, "&aClaimed &f" + meta.displayName() + "&a.");
                String cosmeticEvent = eventKit ? "kit_event_claim" : (temporary ? "kit_temp_claim" : "kit_claim");
                CosmeticsBridge.play(player, cosmeticEvent, player.getLocation());
                if (temporary) openPlayerList(player);
                else player.closeInventory();
            }
            case NOT_FOUND -> {
                CosmeticsBridge.play(player, "kit_error", player.getLocation());
                core.messages().send(player, "&cThat kit no longer exists in Essentials.");
            }
            case DISABLED -> {
                CosmeticsBridge.play(player, "kit_error", player.getLocation());
                core.messages().send(player, "&cThat kit is currently disabled.");
            }
            case NO_PERMISSION -> {
                CosmeticsBridge.play(player, "kit_error", player.getLocation());
                core.messages().send(player, "&cYou do not have permission to claim that kit.");
            }
            case ALREADY_CLAIMED -> {
                CosmeticsBridge.play(player, "kit_error", player.getLocation());
                core.messages().send(player, "&eThat temporary kit has already been claimed.");
            }
            case COOLDOWN -> {
                CosmeticsBridge.play(player, "kit_error", player.getLocation());
                long next = kits.nextUse(player, id);
                String wait = next < 0 ? "This one-time kit has already been claimed."
                        : "That kit is ready in " + KitText.duration(next - System.currentTimeMillis()) + ".";
                core.messages().send(player, "&e" + wait);
            }
            case INSUFFICIENT_FUNDS -> {
                CosmeticsBridge.play(player, "kit_error", player.getLocation());
                core.messages().send(player, "&cYou cannot afford that kit.");
            }
            case INVENTORY_FULL_OR_CANCELLED -> {
                CosmeticsBridge.play(player, "kit_error", player.getLocation());
                core.messages().send(player, "&cThe kit could not be delivered. Check your inventory space.");
            }
            case ERROR -> {
                CosmeticsBridge.play(player, "kit_error", player.getLocation());
                core.messages().send(player, "&cMiraKits could not deliver that kit. Check the server console.");
            }
        }
    }

    /** Player list icon: intentionally just the kit name, no giant contents lore. */
    public ItemStack kitPlaceholder(String id) {
        KitMeta metaData = kits.meta(id);
        ItemStack chest = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = chest.getItemMeta();
        meta.displayName(Component.text(metaData.displayName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        meta.setEnchantmentGlintOverride(false);
        meta.lore(null);
        chest.setItemMeta(meta);
        return chest;
    }

    private ItemStack kitInfo(String id) {
        KitMeta meta = kits.meta(id);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Price: ", NamedTextColor.GRAY)
                .append(Component.text(KitText.money(meta.price(), kits.currencySymbol()), NamedTextColor.WHITE)));
        lore.add(Component.text("Cooldown: ", NamedTextColor.GRAY)
                .append(Component.text(cooldownLabel(kits.delaySeconds(id)), NamedTextColor.WHITE)));
        lore.add(Component.text("Type: ", NamedTextColor.GRAY)
                .append(Component.text(kits.windows().configuredTemporary(id)
                        ? "Temporary (One-Time)" : "Normal", NamedTextColor.WHITE)));
        lore.add(Component.text("Permission: ", NamedTextColor.GRAY)
                .append(Component.text(kits.windows().configuredTemporary(id)
                        ? kits.temporaryPermission(id) : kits.normalPermission(id), NamedTextColor.WHITE)));
        lore.add(Component.empty());
        lore.add(Component.text("Items shown above are exact copies", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("of the configured Essentials kit items.", NamedTextColor.DARK_GRAY));

        List<String> additional = kits.previewAdditionalLines(id);
        if (!additional.isEmpty()) {
            lore.add(Component.empty());
            lore.add(Component.text("Additional kit rewards/actions:", NamedTextColor.YELLOW));
            for (String line : additional) {
                lore.add(Component.text(line, NamedTextColor.GRAY));
            }
        }
        return controlWithLore(Material.BOOK, "Kit Details", NamedTextColor.AQUA, lore);
    }

    private Map<Integer, String> buildPlayerKitSlots(List<String> pageKits, int rows) {
        Map<Integer, String> slots = new LinkedHashMap<>();
        if (pageKits.isEmpty()) return slots;
        int contentRows = rows - 2;
        int base = pageKits.size() / contentRows;
        int remainder = pageKits.size() % contentRows;
        int index = 0;
        for (int row = 0; row < contentRows; row++) {
            int count = base + (row < remainder ? 1 : 0);
            if (count <= 0) continue;
            for (int column : centeredColumns(count)) {
                if (index >= pageKits.size()) break;
                slots.put((row + 1) * 9 + column, pageKits.get(index++));
            }
        }
        return slots;
    }

    private List<Integer> centeredColumns(int count) {
        return switch (count) {
            case 1 -> List.of(4);
            case 2 -> List.of(3, 5);
            case 3 -> List.of(2, 4, 6);
            case 4 -> List.of(1, 3, 5, 7);
            case 5 -> List.of(1, 2, 4, 6, 7);
            case 6 -> List.of(1, 2, 3, 5, 6, 7);
            default -> List.of(1, 2, 3, 4, 5, 6, 7);
        };
    }

    private ItemStack glowingGlass() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        meta.setEnchantmentGlintOverride(true);
        glass.setItemMeta(meta);
        return glass;
    }

    private String cooldownLabel(long delaySeconds) {
        if (delaySeconds < 0) return "One-time kit";
        if (delaySeconds == 0) return "No cooldown";
        return KitText.duration(delaySeconds * 1000L);
    }

    private ItemStack control(Material material, String name, NamedTextColor color) {
        return controlWithLore(material, name, color, List.of());
    }

    private ItemStack controlWithLore(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }
}
