package com.mira.kits.gui;

import com.mira.core.api.MiraCore;
import com.mira.kits.MiraKitsPlugin;
import com.mira.kits.model.AdminEditSession;
import com.mira.kits.model.ClaimResult;
import com.mira.kits.model.KitMeta;
import com.mira.kits.service.AdminSessionService;
import com.mira.kits.service.EssentialsKitService;
import com.mira.kits.util.KitText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class KitGuiService {
    public enum Screen { PLAYER_LIST, PLAYER_DETAIL, ADMIN_LIST, EDITOR, DELETE_CONFIRM }

    public static final class Holder implements InventoryHolder {
        private final Screen screen;
        private final String kitId;
        private final int page;

        public Holder(Screen screen, String kitId, int page) {
            this.screen = screen;
            this.kitId = kitId;
            this.page = page;
        }

        public Screen screen() { return screen; }
        public String kitId() { return kitId; }
        public int page() { return page; }
        @Override public Inventory getInventory() { return null; }
    }

    private static final int PAGE_SIZE = 45;
    private static final int PLAYER_CLAIM_SLOT = 22;

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
                    return meta.visible() && meta.enabled();
                })
                .toList();
        int maxPage = Math.max(0, (visible.size() - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.PLAYER_LIST, null, safePage),
                54,
                Component.text(plugin.getConfig().getString("player-gui-title", "Kits"), NamedTextColor.DARK_PURPLE)
        );

        int start = safePage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < visible.size(); i++) {
            inventory.setItem(i, kitPlaceholder(visible.get(start + i)));
        }
        if (safePage > 0) inventory.setItem(45, control(Material.ARROW, "Previous Page", NamedTextColor.YELLOW));
        inventory.setItem(49, control(Material.BARRIER, "Close", NamedTextColor.RED));
        if (safePage < maxPage) inventory.setItem(53, control(Material.ARROW, "Next Page", NamedTextColor.YELLOW));
        player.openInventory(inventory);
    }

    public boolean openDetail(Player player, String input) {
        String id = kits.match(input);
        if (id == null) return false;
        KitMeta meta = kits.meta(id);
        if ((!meta.visible() || !meta.enabled()) && !player.hasPermission("mirakits.admin")) return false;

        // The player-facing kit screen deliberately exposes no price, cooldown or admin
        // controls. Those settings remain enforced by claim() and are editable only in the
        // mirakits.admin editor. Players see one central action: Claim Kit.
        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.PLAYER_DETAIL, id, 0),
                45,
                Component.text(meta.displayName(), NamedTextColor.DARK_PURPLE)
        );
        inventory.setItem(PLAYER_CLAIM_SLOT, control(Material.LIME_CONCRETE, "Claim Kit", NamedTextColor.GREEN));
        player.openInventory(inventory);
        return true;
    }

    public void openAdminList(Player player) {
        openAdminList(player, 0);
    }

    public void openAdminList(Player player, int page) {
        if (!player.hasPermission("mirakits.admin")) {
            core.messages().send(player, "&cYou do not have permission to administer kits.");
            return;
        }
        List<String> all = new ArrayList<>(kits.kitIds());
        int maxPage = Math.max(0, (all.size() - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, maxPage));
        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.ADMIN_LIST, null, safePage),
                54,
                Component.text(plugin.getConfig().getString("admin-gui-title", "Kit Administration"), NamedTextColor.DARK_RED)
        );
        int start = safePage * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE && start + i < all.size(); i++) {
            inventory.setItem(i, kitPlaceholder(all.get(start + i)));
        }
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
        if (session == null) {
            openAdminList(player);
            return;
        }
        String prefix = plugin.getConfig().getString("editor-gui-title-prefix", "Edit Kit: ");
        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.EDITOR, session.id(), 0),
                54,
                Component.text(prefix + session.displayName(), NamedTextColor.DARK_RED)
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
                List.of(
                        Component.text("Click a kit item above to remove it.", NamedTextColor.GRAY),
                        Component.text("Click an item in your inventory to copy it in.", NamedTextColor.GRAY)
                )));
        if (!session.newKit()) {
            inventory.setItem(52, control(Material.TNT, "Delete Kit", NamedTextColor.RED));
        }
        inventory.setItem(53, control(Material.EMERALD_BLOCK, "Save Kit", NamedTextColor.GREEN));
        player.openInventory(inventory);
    }

    public void openDeleteConfirm(Player player) {
        AdminEditSession session = sessions.get(player.getUniqueId()).orElse(null);
        if (session == null || session.newKit()) {
            openAdminList(player);
            return;
        }
        Inventory inventory = Bukkit.createInventory(
                new Holder(Screen.DELETE_CONFIRM, session.id(), 0),
                27,
                Component.text("Delete " + session.displayName() + "?", NamedTextColor.DARK_RED)
        );
        inventory.setItem(11, control(Material.LIME_CONCRETE, "Keep Kit", NamedTextColor.GREEN));
        inventory.setItem(15, control(Material.RED_CONCRETE, "Delete Permanently", NamedTextColor.RED));
        player.openInventory(inventory);
    }

    public void claimAndRespond(Player player, String id) {
        ClaimResult result = kits.claim(player, id);
        switch (result) {
            case SUCCESS -> {
                KitMeta meta = kits.meta(id);
                core.messages().send(player, "&aClaimed &f" + meta.displayName() + "&a.");
                player.closeInventory();
            }
            case NOT_FOUND -> core.messages().send(player, "&cThat kit no longer exists in Essentials.");
            case DISABLED -> core.messages().send(player, "&cThat kit is currently disabled.");
            case NO_PERMISSION -> core.messages().send(player, "&cYou do not have permission to claim that kit.");
            case COOLDOWN -> {
                long next = kits.nextUse(player, id);
                String wait = next < 0 ? "This one-time kit has already been claimed."
                        : "That kit is ready in " + KitText.duration(next - System.currentTimeMillis()) + ".";
                core.messages().send(player, "&e" + wait);
            }
            case INSUFFICIENT_FUNDS -> core.messages().send(player, "&cYou cannot afford that kit.");
            case INVENTORY_FULL_OR_CANCELLED -> core.messages().send(player, "&cThe kit could not be delivered. Check your inventory space.");
            case ERROR -> core.messages().send(player, "&cMiraKits could not deliver that kit. Check the server console.");
        }
    }

    public ItemStack kitPlaceholder(String id) {
        KitMeta metaData = kits.meta(id);
        ItemStack chest = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = chest.getItemMeta();
        meta.displayName(Component.text(metaData.displayName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.setEnchantmentGlintOverride(true);
        meta.lore(buildKitLore(id));
        chest.setItemMeta(meta);
        return chest;
    }

    private List<Component> buildKitLore(String id) {
        List<Component> lore = new ArrayList<>();
        for (ItemStack item : kits.previewItems(id)) {
            lore.add(itemLine(item));
            item.getEnchantments().entrySet().stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().getKey().getKey()))
                    .forEach(entry -> lore.add(Component.text(" - ", NamedTextColor.WHITE)
                            .append(Component.text(enchantName(entry.getKey()) + " " + KitText.roman(entry.getValue()), NamedTextColor.GREEN))
                            .decoration(TextDecoration.ITALIC, false)));
        }
        if (lore.isEmpty()) {
            lore.add(Component.text("Empty Kit", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        }
        return lore;
    }

    private Component itemLine(ItemStack item) {
        Component name;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            name = item.getItemMeta().displayName();
            if (name == null) name = Component.text(materialName(item.getType(), item.getAmount() > 1));
        } else {
            name = Component.text(materialName(item.getType(), item.getAmount() > 1), NamedTextColor.WHITE);
        }
        if (item.getAmount() > 1) {
            return Component.text(item.getAmount() + "x ", NamedTextColor.WHITE)
                    .append(name)
                    .decoration(TextDecoration.ITALIC, false);
        }
        return name.decoration(TextDecoration.ITALIC, false);
    }

    private String enchantName(Enchantment enchantment) {
        return KitText.prettyId(enchantment.getKey().getKey());
    }

    private String materialName(Material material, boolean plural) {
        String base = KitText.prettyId(material.name());
        if (!plural) return base;
        if (base.endsWith("s")) return base;
        if (base.endsWith("Axe")) return base + "s";
        if (base.endsWith("y") && base.length() > 1) return base.substring(0, base.length() - 1) + "ies";
        return base + "s";
    }

    private String cooldownLabel(long delaySeconds) {
        if (delaySeconds < 0) return "One-time kit";
        if (delaySeconds == 0) return "No cooldown";
        long minutes = delaySeconds / 60L;
        return minutes + " minute" + (minutes == 1 ? "" : "s");
    }

    private ItemStack control(Material material, String name, NamedTextColor color) {
        return controlWithLore(material, name, color, List.of());
    }

    private ItemStack controlWithLore(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        }
        item.setItemMeta(meta);
        return item;
    }
}
