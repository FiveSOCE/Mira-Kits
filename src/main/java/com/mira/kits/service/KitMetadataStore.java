package com.mira.kits.service;

import com.mira.kits.MiraKitsPlugin;
import com.mira.kits.model.KitMeta;
import com.mira.kits.util.KitText;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.logging.Level;

public final class KitMetadataStore {
    private final MiraKitsPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public KitMetadataStore(MiraKitsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kit-meta.yml");
        reload();
    }

    public void reload() {
        plugin.getDataFolder().mkdirs();
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public KitMeta get(String id) {
        String base = "kits." + id + ".";
        String displayName = config.getString(base + "display-name", KitText.prettyId(id));
        BigDecimal price;
        try {
            price = new BigDecimal(config.getString(base + "price", "0"));
        } catch (NumberFormatException ex) {
            price = BigDecimal.ZERO;
        }
        if (price.signum() < 0) price = BigDecimal.ZERO;
        boolean visible = config.getBoolean(base + "visible", true);
        boolean enabled = config.getBoolean(base + "enabled", true);
        return new KitMeta(id, displayName, price, visible, enabled);
    }

    public void save(KitMeta meta) {
        String base = "kits." + meta.id() + ".";
        config.set(base + "display-name", meta.displayName());
        config.set(base + "price", meta.price().max(BigDecimal.ZERO).stripTrailingZeros().toPlainString());
        config.set(base + "visible", meta.visible());
        config.set(base + "enabled", meta.enabled());
        saveFile();
    }

    public void delete(String id) {
        config.set("kits." + id, null);
        saveFile();
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + file.getName(), ex);
        }
    }
}
