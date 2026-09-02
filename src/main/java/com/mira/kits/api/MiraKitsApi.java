package com.mira.kits.api;

import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Public entry point intended for MiraNPC and other Mira modules.
 */
public interface MiraKitsApi {
    void openKits(Player player);

    boolean openKit(Player player, String kitId);

    Set<String> kitIds();
}
