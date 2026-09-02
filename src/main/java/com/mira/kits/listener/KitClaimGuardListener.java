package com.mira.kits.listener;

import com.mira.core.api.MiraCore;
import com.mira.kits.MiraKitsPlugin;
import com.mira.kits.service.EssentialsKitService;
import net.ess3.api.events.KitClaimEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class KitClaimGuardListener implements Listener {
    private final MiraKitsPlugin plugin;
    private final MiraCore core;
    private final EssentialsKitService kits;

    public KitClaimGuardListener(MiraKitsPlugin plugin, MiraCore core, EssentialsKitService kits) {
        this.plugin = plugin;
        this.core = core;
        this.kits = kits;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKitClaim(KitClaimEvent event) {
        if (!plugin.getConfig().getBoolean("enforce-gui-claims", true)) return;
        Player player = event.getUser().getBase();
        if (player == null || kits.isInternalClaim(player.getUniqueId())) return;
        event.setCancelled(true);
        core.messages().send(player, "&eKits are managed through the MiraKits GUI. Use &f/kits&e.");
    }
}
