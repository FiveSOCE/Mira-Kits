package com.mira.kits.gui;

import com.mira.core.api.MiraCore;
import com.mira.kits.model.AdminEditSession;
import com.mira.kits.model.KitMeta;
import com.mira.kits.prompt.ChatPromptService;
import com.mira.kits.service.AdminSessionService;
import com.mira.kits.service.EssentialsKitService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class KitGuiListener implements Listener {
    private static final int PAGE_SIZE = 45;
    private static final int PLAYER_CLAIM_SLOT = 22;

    private final MiraCore core;
    private final EssentialsKitService kits;
    private final AdminSessionService sessions;
    private final KitGuiService gui;
    private final ChatPromptService prompts;

    public KitGuiListener(MiraCore core, EssentialsKitService kits, AdminSessionService sessions,
                          KitGuiService gui, ChatPromptService prompts) {
        this.core = core;
        this.kits = kits;
        this.sessions = sessions;
        this.gui = gui;
        this.prompts = prompts;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof KitGuiService.Holder holder)) return;

        event.setCancelled(true);
        int raw = event.getRawSlot();
        if (raw < 0) return;

        switch (holder.screen()) {
            case PLAYER_LIST -> playerListClick(player, holder, raw);
            case PLAYER_DETAIL -> detailClick(player, holder, raw);
            case ADMIN_LIST -> adminListClick(player, holder, raw);
            case EDITOR -> editorClick(player, event, raw);
            case DELETE_CONFIRM -> deleteClick(player, raw);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof KitGuiService.Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessions.clear(event.getPlayer().getUniqueId());
        prompts.clear(event.getPlayer().getUniqueId());
    }

    private void playerListClick(Player player, KitGuiService.Holder holder, int raw) {
        if (raw >= 0 && raw < PAGE_SIZE) {
            String id = playerListId(holder.page(), raw);
            if (id != null) gui.openDetail(player, id);
            return;
        }
        if (raw == 45 && holder.page() > 0) gui.openPlayerList(player, holder.page() - 1);
        else if (raw == 49) player.closeInventory();
        else if (raw == 53) gui.openPlayerList(player, holder.page() + 1);
    }

    private void detailClick(Player player, KitGuiService.Holder holder, int raw) {
        if (raw == PLAYER_CLAIM_SLOT && holder.kitId() != null) {
            gui.claimAndRespond(player, holder.kitId());
        }
    }

    private void adminListClick(Player player, KitGuiService.Holder holder, int raw) {
        if (!player.hasPermission("mirakits.admin")) {
            player.closeInventory();
            return;
        }
        if (raw >= 0 && raw < PAGE_SIZE) {
            String id = adminListId(holder.page(), raw);
            if (id != null) {
                sessions.put(player.getUniqueId(), kits.loadDraft(id));
                gui.openEditor(player);
            }
            return;
        }
        if (raw == 45 && holder.page() > 0) gui.openAdminList(player, holder.page() - 1);
        else if (raw == 48) prompts.beginName(player);
        else if (raw == 49) player.closeInventory();
        else if (raw == 50) {
            kits.reloadAll();
            core.messages().send(player, "&aReloaded Essentials kits and MiraKits metadata.");
            gui.openAdminList(player, holder.page());
        } else if (raw == 53) gui.openAdminList(player, holder.page() + 1);
    }

    private void editorClick(Player player, InventoryClickEvent event, int raw) {
        if (!player.hasPermission("mirakits.admin")) {
            player.closeInventory();
            return;
        }
        AdminEditSession session = sessions.get(player.getUniqueId()).orElse(null);
        if (session == null) {
            gui.openAdminList(player);
            return;
        }
        Inventory top = event.getView().getTopInventory();

        if (raw >= 0 && raw < 45) {
            if (top.getItem(raw) != null) {
                session.removeItem(raw);
                top.setItem(raw, null);
            }
            return;
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getBottomInventory())) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            int free = session.firstFreeSlot();
            if (free < 0) {
                core.messages().send(player, "&cThat kit editor is full.");
                return;
            }
            session.putItem(free, clicked);
            top.setItem(free, clicked.clone());
            return;
        }

        switch (raw) {
            case 45 -> {
                sessions.clear(player.getUniqueId());
                gui.openAdminList(player);
            }
            case 46 -> {
                kits.copyPlayerInventory(player, session);
                core.messages().send(player, "&aCopied your current inventory into the kit editor.");
                gui.openEditor(player);
            }
            case 47 -> prompts.beginCooldown(player);
            case 48 -> prompts.beginPrice(player);
            case 49 -> {
                session.visible(!session.visible());
                gui.openEditor(player);
            }
            case 50 -> {
                session.enabled(!session.enabled());
                gui.openEditor(player);
            }
            case 52 -> {
                if (!session.newKit()) gui.openDeleteConfirm(player);
            }
            case 53 -> {
                if (!kits.saveDraft(session)) {
                    core.messages().send(player, "&cA kit must contain at least one item or preserved Essentials kit line before it can be saved.");
                    return;
                }
                core.messages().send(player, "&aSaved &f" + session.displayName() + "&a directly into Essentials kits.");
                sessions.clear(player.getUniqueId());
                gui.openAdminList(player);
            }
            default -> { }
        }
    }

    private void deleteClick(Player player, int raw) {
        AdminEditSession session = sessions.get(player.getUniqueId()).orElse(null);
        if (session == null) {
            gui.openAdminList(player);
            return;
        }
        if (raw == 11) {
            gui.openEditor(player);
        } else if (raw == 15 && !session.newKit()) {
            kits.deleteKit(session.id());
            core.messages().send(player, "&cDeleted &f" + session.displayName() + "&c from Essentials.");
            sessions.clear(player.getUniqueId());
            gui.openAdminList(player);
        }
    }

    private String playerListId(int page, int slot) {
        List<String> visible = kits.kitIds().stream()
                .filter(id -> {
                    KitMeta meta = kits.meta(id);
                    return meta.visible() && meta.enabled();
                })
                .toList();
        int index = page * PAGE_SIZE + slot;
        return index >= 0 && index < visible.size() ? visible.get(index) : null;
    }

    private String adminListId(int page, int slot) {
        List<String> all = new ArrayList<>(kits.kitIds());
        int index = page * PAGE_SIZE + slot;
        return index >= 0 && index < all.size() ? all.get(index) : null;
    }
}
