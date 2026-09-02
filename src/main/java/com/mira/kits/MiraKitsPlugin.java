package com.mira.kits;

import com.earth2me.essentials.IEssentials;
import com.mira.core.api.MiraCore;
import com.mira.core.api.MiraCoreProvider;
import com.mira.core.api.ModuleHealth;
import com.mira.kits.api.MiraKitsApi;
import com.mira.kits.api.MiraKitsApiImpl;
import com.mira.kits.command.MiraKitsCommand;
import com.mira.kits.gui.KitGuiListener;
import com.mira.kits.gui.KitGuiService;
import com.mira.kits.listener.KitClaimGuardListener;
import com.mira.kits.listener.KitCommandBridgeListener;
import com.mira.kits.prompt.ChatPromptService;
import com.mira.kits.service.AdminSessionService;
import com.mira.kits.service.EssentialsKitService;
import com.mira.kits.service.KitMetadataStore;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class MiraKitsPlugin extends JavaPlugin {
    private MiraCore core;
    private IEssentials essentials;
    private KitMetadataStore metadata;
    private EssentialsKitService kits;
    private AdminSessionService sessions;
    private KitGuiService gui;
    private MiraKitsApi api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        core = MiraCoreProvider.require();

        Plugin essentialsPlugin = getServer().getPluginManager().getPlugin("Essentials");
        if (!(essentialsPlugin instanceof IEssentials essentialsApi) || !essentialsPlugin.isEnabled()) {
            throw new IllegalStateException("MiraKits requires an enabled EssentialsX plugin.");
        }
        essentials = essentialsApi;

        metadata = new KitMetadataStore(this);
        kits = new EssentialsKitService(this, essentials, metadata);
        sessions = new AdminSessionService();
        gui = new KitGuiService(this, core, kits, sessions);
        ChatPromptService prompts = new ChatPromptService(this, core, kits, sessions, gui);
        api = new MiraKitsApiImpl(gui, kits);

        core.modules().register(this, "MiraKits");
        core.services().register(MiraKitsApi.class, api);

        getServer().getPluginManager().registerEvents(prompts, this);
        getServer().getPluginManager().registerEvents(new KitGuiListener(core, kits, sessions, gui, prompts), this);
        getServer().getPluginManager().registerEvents(new KitCommandBridgeListener(this, core, gui), this);
        getServer().getPluginManager().registerEvents(new KitClaimGuardListener(this, core, kits), this);

        MiraKitsCommand executor = new MiraKitsCommand(core, gui);
        PluginCommand command = getCommand("mirakits");
        if (command == null) {
            core.modules().setHealth(this, ModuleHealth.UNHEALTHY, "mirakits command missing from plugin.yml");
            throw new IllegalStateException("mirakits command missing from plugin.yml");
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);

        core.modules().setHealth(this, ModuleHealth.HEALTHY,
                "EssentialsX kit GUI, economy, cooldown and admin editor ready");
        getLogger().info("MiraKits v" + getPluginMeta().getVersion() + " enabled with " + kits.kitIds().size() + " Essentials kit(s).");
    }

    @Override
    public void onDisable() {
        if (core != null) {
            if (api != null) core.services().unregister(MiraKitsApi.class, api);
            core.modules().unregister(this);
        }
    }

    public IEssentials essentials() {
        return essentials;
    }

    public EssentialsKitService kits() {
        return kits;
    }

    public KitGuiService gui() {
        return gui;
    }
}
