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

    public String tr(String path, java.util.Map<String, ?> vars) {
        String s = tr(path);
        if (vars == null || vars.isEmpty()) {
            return s;
        }
        for (var e : vars.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String value = e.getValue() != null ? String.valueOf(e.getValue()) : "";
            s = s.replace("%" + e.getKey() + "%", value);
        }
        return s;
    }

    public String tr(String path, Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return tr(path);
        }
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key == null) {
                continue;
            }
            map.put(String.valueOf(key), value != null ? String.valueOf(value) : "");
        }
        return tr(path, map);
    }
}
