package com.mira.kits.command;

import com.mira.core.api.MiraCore;
import com.mira.kits.gui.KitGuiService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public final class MiraKitsCommand implements TabExecutor {
    private final MiraCore core;
    private final KitGuiService gui;

    public MiraKitsCommand(MiraCore core, KitGuiService gui) {
        this.core = core;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            core.messages().send(sender, "&cMiraKits is GUI-only and must be opened by a player.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            gui.openAdminList(player);
        } else {
            gui.openPlayerList(player);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1 || !player.hasPermission("mirakits.admin")) return List.of();
        String input = args[0].toLowerCase(Locale.ROOT);
        return "admin".startsWith(input) ? List.of("admin") : List.of();
    }
}
