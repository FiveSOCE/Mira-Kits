package com.mira.kits.command;

import com.mira.core.api.MiraCore;
import com.mira.kits.gui.KitGuiService;
import com.mira.kits.service.EssentialsKitService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class MiraKitsCommand implements TabExecutor {
    private final MiraCore core;
    private final KitGuiService gui;
    private final EssentialsKitService kits;

    public MiraKitsCommand(MiraCore core, KitGuiService gui, EssentialsKitService kits) {
        this.core = core;
        this.gui = gui;
        this.kits = kits;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && (args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("give"))) {
            return grant(sender, args);
        }

        if (!(sender instanceof Player player)) {
            core.messages().send(sender, "&eConsole usage: &f/mirakits grant <player> <kit>");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!player.hasPermission("mirakits.admin")) {
                core.messages().send(player, "&cYou do not have permission.");
                return true;
            }
            gui.openAdminList(player);
        } else {
            gui.openPlayerList(player);
        }
        return true;
    }

    private boolean grant(CommandSender sender, String[] args) {
        if (sender instanceof Player player && !player.hasPermission("mirakits.admin")) {
            core.messages().send(sender, "&cYou do not have permission.");
            return true;
        }
        if (args.length < 3) {
            core.messages().send(sender, "&eUsage: &f/mirakits grant <player> <kit>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            core.messages().send(sender, "&cThat player must be online.");
            return true;
        }

        String matched = kits.match(args[2]);
        if (matched == null) {
            core.messages().send(sender, "&cThat kit does not exist.");
            return true;
        }

        if (!kits.grantVoucher(target, matched)) {
            core.messages().send(sender, "&cCould not grant that kit. The target inventory may be full or Essentials rejected the kit.");
            return true;
        }

        core.messages().send(sender, "&aGranted kit &f" + matched + " &ato &f" + target.getName() + "&a instantly.");
        core.messages().send(target, "&aYou received kit &f" + matched + "&a.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean admin = !(sender instanceof Player player) || player.hasPermission("mirakits.admin");
        if (args.length == 1) {
            List<String> values = new ArrayList<>();
            if (admin) values.addAll(List.of("admin", "grant", "give"));
            return complete(args[0], values);
        }
        if (admin && args.length == 2 && isGrant(args[0])) {
            return complete(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (admin && args.length == 3 && isGrant(args[0])) {
            return complete(args[2], kits.kitIds());
        }
        return List.of();
    }

    private boolean isGrant(String value) {
        return value.equalsIgnoreCase("grant") || value.equalsIgnoreCase("give");
    }

    private static List<String> complete(String prefix, Collection<String> values) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
