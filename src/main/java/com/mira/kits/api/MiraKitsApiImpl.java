package com.mira.kits.api;

import com.mira.kits.gui.KitGuiService;
import com.mira.kits.service.EssentialsKitService;
import org.bukkit.entity.Player;

import java.util.Set;

public final class MiraKitsApiImpl implements MiraKitsApi {
    private final KitGuiService gui;
    private final EssentialsKitService kits;

    public MiraKitsApiImpl(KitGuiService gui, EssentialsKitService kits) {
        this.gui = gui;
        this.kits = kits;
    }

    @Override
    public void openKits(Player player) {
        gui.openPlayerList(player);
    }

    @Override
    public boolean openKit(Player player, String kitId) {
        return gui.openDetail(player, kitId);
    }

    @Override
    public Set<String> kitIds() {
        return Set.copyOf(kits.kitIds());
    }
}
