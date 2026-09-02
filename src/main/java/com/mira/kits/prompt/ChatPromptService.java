package com.mira.kits.prompt;

import com.mira.core.api.MiraCore;
import com.mira.kits.MiraKitsPlugin;
import com.mira.kits.gui.KitGuiService;
import com.mira.kits.model.AdminEditSession;
import com.mira.kits.service.AdminSessionService;
import com.mira.kits.service.EssentialsKitService;
import com.mira.kits.util.KitText;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatPromptService implements Listener {
    private enum Type { CREATE_NAME, COOLDOWN, PRICE }
    private record Prompt(Type type) { }

    private final MiraKitsPlugin plugin;
    private final MiraCore core;
    private final EssentialsKitService kits;
    private final AdminSessionService sessions;
    private final KitGuiService gui;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public ChatPromptService(MiraKitsPlugin plugin, MiraCore core, EssentialsKitService kits,
                             AdminSessionService sessions, KitGuiService gui) {
        this.plugin = plugin;
        this.core = core;
        this.kits = kits;
        this.sessions = sessions;
        this.gui = gui;
    }

    public void beginName(Player player) {
        if (!player.hasPermission("mirakits.admin")) return;
        beginPrompt(player, new Prompt(Type.CREATE_NAME),
                "&dCreating a kit. &fEnter the kit name in chat, or type &ccancel&f.");
    }

    public void beginCooldown(Player player) {
        if (!player.hasPermission("mirakits.admin") || sessions.get(player.getUniqueId()).isEmpty()) return;
        beginPrompt(player, new Prompt(Type.COOLDOWN),
                "&dSet Kit Cooldown. &fEnter the cooldown in minutes, or type &ccancel&f.");
    }

    public void beginPrice(Player player) {
        if (!player.hasPermission("mirakits.admin") || sessions.get(player.getUniqueId()).isEmpty()) return;
        beginPrompt(player, new Prompt(Type.PRICE),
                "&dSet Kit Price. &fEnter the price as a number, or type &ccancel&f.");
    }

    private void beginPrompt(Player player, Prompt prompt, String message) {
        UUID playerId = player.getUniqueId();

        // Inventory controls are clicked from InventoryClickEvent. Arm the prompt on the
        // following server tick so the inventory transaction is fully finished before we
        // close the GUI and switch the player into private chat-input mode.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            prompts.put(playerId, prompt);
            player.closeInventory();
            core.messages().send(player, message);
            core.messages().send(player, "&7Your next chat message is private and will not be broadcast.");
        });
    }

    public void clear(UUID playerId) {
        prompts.remove(playerId);
    }

    public boolean awaiting(UUID playerId) {
        return prompts.containsKey(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Prompt prompt = prompts.get(player.getUniqueId());
        if (prompt == null) return;

        event.setCancelled(true);
        String value = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> handle(player, prompt, value));
    }

    private void handle(Player player, Prompt prompt, String value) {
        if (!player.isOnline()) {
            clear(player.getUniqueId());
            return;
        }

        // Ignore a stale scheduled chat callback if a newer prompt replaced it.
        if (!prompt.equals(prompts.get(player.getUniqueId()))) return;

        if (value.equalsIgnoreCase("cancel")) {
            clear(player.getUniqueId());
            core.messages().send(player, "&eKit input cancelled.");
            reopenAfterPrompt(player, prompt.type() == Type.CREATE_NAME);
            return;
        }

        switch (prompt.type()) {
            case CREATE_NAME -> handleName(player, value);
            case COOLDOWN -> handleCooldown(player, value);
            case PRICE -> handlePrice(player, value);
        }
    }

    private void handleName(Player player, String value) {
        if (value.isBlank() || value.length() > 40) {
            core.messages().send(player, "&cKit names must be between 1 and 40 characters. Try again or type cancel.");
            return;
        }
        String id = KitText.normalizeId(value);
        if (id.isBlank() || id.length() > 40) {
            core.messages().send(player, "&cThat name cannot be converted into a valid kit ID. Try again.");
            return;
        }
        if (kits.exists(id)) {
            core.messages().send(player, "&cA kit with that name already exists in Essentials. Try another name.");
            return;
        }

        AdminEditSession session = kits.newDraft(value);
        sessions.put(player.getUniqueId(), session);
        clear(player.getUniqueId());
        core.messages().send(player, "&aKit draft created. &7Use the GUI to copy your inventory, set cooldown/price, then save.");
        reopenAfterPrompt(player, false);
    }

    private void handleCooldown(Player player, String value) {
        AdminEditSession session = sessions.get(player.getUniqueId()).orElse(null);
        if (session == null) {
            clear(player.getUniqueId());
            gui.openAdminList(player);
            return;
        }
        try {
            long minutes = Long.parseLong(value.replace(",", ""));
            if (minutes < 0 || minutes > 5_256_000L) throw new NumberFormatException();
            session.delaySeconds(Math.multiplyExact(minutes, 60L));
            clear(player.getUniqueId());
            core.messages().send(player, "&aCooldown set to &f" + minutes + " minute" + (minutes == 1 ? "" : "s") + "&a.");
            reopenAfterPrompt(player, false);
        } catch (ArithmeticException | NumberFormatException ex) {
            core.messages().send(player, "&cEnter a whole number of minutes from 0 to 5,256,000, or type cancel.");
        }
    }

    private void handlePrice(Player player, String value) {
        AdminEditSession session = sessions.get(player.getUniqueId()).orElse(null);
        if (session == null) {
            clear(player.getUniqueId());
            gui.openAdminList(player);
            return;
        }
        try {
            String cleaned = value.replace(",", "").replace(kits.currencySymbol(), "").replace("$", "").trim();
            BigDecimal price = new BigDecimal(cleaned);
            if (price.signum() < 0 || price.scale() > 2) throw new NumberFormatException();
            session.price(price);
            clear(player.getUniqueId());
            core.messages().send(player, "&aKit price set to &6" + KitText.money(price, kits.currencySymbol()) + "&a.");
            reopenAfterPrompt(player, false);
        } catch (NumberFormatException ex) {
            core.messages().send(player, "&cEnter a non-negative number with up to 2 decimal places, or type cancel.");
        }
    }

    private void reopenAfterPrompt(Player player, boolean adminList) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (adminList) gui.openAdminList(player);
            else gui.openEditor(player);
        });
    }
}
