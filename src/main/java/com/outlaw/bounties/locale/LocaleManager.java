package com.outlaw.bounties.locale;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class LocaleManager {
    private final BountyPlugin plugin;
    private FileConfiguration lang;

    public LocaleManager(BountyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String code = plugin.getConfig().getString("locale", "fr");
        String filename = code.equalsIgnoreCase("en") ? "en.yml" : "fr.yml";
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) {
            plugin.saveResource(filename, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(filename)) {
            if (in != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                config.setDefaults(defaults);
                config.options().copyDefaults(true);
                try {
                    config.save(file);
                } catch (IOException ex) {
                    plugin.getLogger().log(Level.WARNING, "Unable to persist updated locale defaults", ex);
                }
            }
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Unable to read bundled locale defaults", ex);
        }
        lang = config;
    }

    public String tr(String path) {
        String s = lang.getString(path, path);
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public String tr(String path, java.util.Map<String, String> vars) {
        String s = tr(path);
        for (var e : vars.entrySet()) {
            s = s.replace("%" + e.getKey() + "%", e.getValue());
        }
        return s;
    }
}
