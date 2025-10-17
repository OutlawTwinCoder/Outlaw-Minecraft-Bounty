package com.outlaw.bounties.locale;

import com.outlaw.bounties.BountyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
        lang = YamlConfiguration.loadConfiguration(file);

        try (var in = plugin.getResource(filename)) {
            if (in != null) {
                try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    var defaults = YamlConfiguration.loadConfiguration(reader);
                    lang.setDefaults(defaults);
                    lang.options().copyDefaults(true);
                    lang.save(file);
                }
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Unable to merge locale defaults: " + ex.getMessage());
        }
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
