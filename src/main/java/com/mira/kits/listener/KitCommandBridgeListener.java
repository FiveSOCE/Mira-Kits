package com.mira.kits.listener;

import com.mira.core.api.MiraCore;
import com.mira.kits.MiraKitsPlugin;
import com.mira.kits.gui.KitGuiService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;

public final class KitCommandBridgeListener implements Listener {
    private final MiraKitsPlugin plugin;
    private final MiraCore core;
    private final KitGuiService gui;

    public KitCommandBridgeListener(MiraKitsPlugin plugin, MiraCore core, KitGuiService gui) {
        this.plugin = plugin;
        this.core = core;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("intercept-essentials-kit-commands", true)) return;
        String raw = event.getMessage().substring(1).trim();
        if (raw.isEmpty()) return;
        String[] parts = raw.split("\\s+");
        String label = parts[0].toLowerCase(Locale.ROOT);
        if (!isKitLabel(label)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (parts.length == 1) {
                gui.openPlayerList(player);
            } else if (parts.length == 2) {
                if (!gui.openDetail(player, parts[1])) {
                    core.messages().send(player, "&cThat kit is not currently available.");
                }
            } else {
                core.messages().send(player, "&eKit claiming is GUI-only. Use &f/kits&e.");
                gui.openPlayerList(player);
            }
        });
    }

    /**
     * Compatibility bridge for console callers such as MiraItems vouchers. Essentials' normal
     * /kit flow fires KitClaimEvent, which MiraKits intentionally blocks for GUI-only player
     * claims. Console kit delivery is administrative, so route it through the internal bypass
     * path instead.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsoleKit(ServerCommandEvent event) {
        String raw = event.getCommand() == null ? "" : event.getCommand().trim();
        if (raw.isEmpty()) return;
        String[] parts = raw.split("\\s+");
        if (parts.length != 3 || !isKitLabel(parts[0].toLowerCase(Locale.ROOT))) return;

        Player target = Bukkit.getPlayerExact(parts[2]);
        String matched = plugin.kits().match(parts[1]);
        if (target == null || matched == null) return;

        event.setCancelled(true);
        if (plugin.kits().grantVoucher(target, matched)) {
            core.messages().send(event.getSender(), "&aGranted kit &f" + matched + " &ato &f" + target.getName() + "&a instantly.");
            core.messages().send(target, "&aYou received kit &f" + matched + "&a.");
        } else {
            core.messages().send(event.getSender(), "&cCould not grant kit &f" + matched + "&c to &f" + target.getName() + "&c.");
        }
    }

    private boolean isKitLabel(String label) {
        return label.equals("kit") || label.equals("kits")
                || label.equals("essentials:kit") || label.equals("essentials:kits");
    }
}
