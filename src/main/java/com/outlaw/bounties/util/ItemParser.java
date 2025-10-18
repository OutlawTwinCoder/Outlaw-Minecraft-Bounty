package com.outlaw.bounties.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.outlaw.bounties.BountyPlugin;
import com.outlaw.bounties.item.ConfiguredItem;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Utility responsible for reading item descriptions from configuration files.
 */
public final class ItemParser {

    private ItemParser() {}

    public static ConfiguredItem parseItem(BountyPlugin plugin, Object spec) {
        return parseItem(plugin, spec, -1);
    }

    public static ConfiguredItem parseItem(BountyPlugin plugin, Object spec, int defaultAmount) {
        if (spec == null) return null;

        if (spec instanceof ConfiguredItem configured) {
            return configured;
        }

        if (spec instanceof ItemStack stack) {
            return new ConfiguredItem(stack);
        }

        if (spec instanceof String str) {
            Material mat = Material.matchMaterial(str.toUpperCase(Locale.ROOT));
            if (mat == null) {
                return null;
            }
            ItemStack stack = new ItemStack(mat, defaultAmount > 0 ? defaultAmount : 1);
            return new ConfiguredItem(stack);
        }

        if (spec instanceof Map<?,?> map) {
            MemoryConfiguration mem = new MemoryConfiguration();
            toConfiguration(mem, map);
            return parseItem(plugin, mem, defaultAmount);
        }

        if (spec instanceof ConfigurationSection section) {
            return parseItem(plugin, section, defaultAmount);
        }

        return null;
    }

    private static ConfiguredItem parseItem(BountyPlugin plugin, ConfigurationSection section, int defaultAmount) {
        String materialName = section.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
        if (material == null) {
            material = Material.STONE;
        }

        int amount = Math.max(1, section.getInt("amount", defaultAmount > 0 ? defaultAmount : 1));
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            if (section.isString("name")) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("name")));
            }

            if (section.isList("lore")) {
                List<String> rawLore = new ArrayList<>();
                for (Object line : section.getList("lore")) {
                    rawLore.add(ChatColor.translateAlternateColorCodes('&', String.valueOf(line)));
                }
                meta.setLore(rawLore);
            }

            if (section.getBoolean("unbreakable", false)) {
                meta.setUnbreakable(true);
            }

            if (section.isInt("custom_model_data")) {
                meta.setCustomModelData(section.getInt("custom_model_data"));
            }

            if (section.isList("flags")) {
                for (Object raw : section.getList("flags")) {
                    ItemFlag flag = parseFlag(String.valueOf(raw));
                    if (flag != null) {
                        meta.addItemFlags(flag);
                    }
                }
            }

            if (meta instanceof LeatherArmorMeta leather && section.isString("color")) {
                leather.setColor(parseColor(section.getString("color")));
            }

            if (meta instanceof PotionMeta potion) {
                if (section.isString("base_potion")) {
                    PotionType type = PotionType.valueOf(section.getString("base_potion").toUpperCase(Locale.ROOT));
                    potion.setBasePotionData(new PotionData(type));
                }
                if (section.isList("potion_effects")) {
                    for (Object raw : section.getList("potion_effects")) {
                        PotionEffect effect = parsePotionEffect(raw);
                        if (effect != null) {
                            potion.addCustomEffect(effect, true);
                        }
                    }
                }
            }

            if (meta instanceof SkullMeta skull) {
                if (section.isString("skull_owner")) {
                    skull.setOwner(section.getString("skull_owner"));
                }
                if (section.isString("head_texture")) {
                    applyHeadTexture(skull, section.getString("head_texture"));
                }
            }

            if (section.isList("attributes")) {
                for (Object raw : section.getList("attributes")) {
                    if (!(raw instanceof Map<?,?> map)) continue;
                    Attribute attribute = parseAttribute(String.valueOf(map.get("attribute")));
                    if (attribute == null) continue;
                    double amountValue = toDouble(map.get("amount"), 0.0);
                    AttributeModifier.Operation operation = parseOperation(String.valueOf(map.getOrDefault("operation", "ADD_NUMBER")));
                    EquipmentSlot slot = parseSlot(map.get("slot"));
                    AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "obo-config", amountValue, operation, slot);
                    meta.addAttributeModifier(attribute, modifier);
                }
            }

            PersistentDataContainer container = meta.getPersistentDataContainer();
            if (section.isConfigurationSection("auraskill")) {
                ConfigurationSection auraskill = section.getConfigurationSection("auraskill");
                for (String key : auraskill.getKeys(false)) {
                    NamespacedKey namespacedKey = new NamespacedKey("auraskill", key.toLowerCase(Locale.ROOT));
                    Object value = auraskill.get(key);
                    applyPersistentData(container, namespacedKey, value);
                }
            }

            if (section.isList("persistent_data")) {
                for (Object raw : section.getList("persistent_data")) {
                    if (!(raw instanceof Map<?,?> map)) continue;
                    Object keyObj = map.get("key");
                    if (keyObj == null) continue;
                    String namespace = String.valueOf(map.getOrDefault("namespace", plugin.getName().toLowerCase(Locale.ROOT)));
                    String key = String.valueOf(keyObj);
                    NamespacedKey namespacedKey = new NamespacedKey(namespace, key);
                    Object value = map.get("value");
                    applyPersistentData(container, namespacedKey, value);
                }
            }

            stack.setItemMeta(meta);
        }

        if (section.isConfigurationSection("enchants")) {
            for (String key : section.getConfigurationSection("enchants").getKeys(false)) {
                Enchantment enchantment = parseEnchantment(key);
                if (enchantment == null) continue;
                int level = section.getInt("enchants." + key, 1);
                stack.addUnsafeEnchantment(enchantment, Math.max(1, level));
            }
        } else if (section.isList("enchants")) {
            for (Object raw : section.getList("enchants")) {
                if (!(raw instanceof Map<?,?> map)) continue;
                Enchantment enchantment = parseEnchantment(String.valueOf(map.get("type")));
                if (enchantment == null) continue;
                int level = map.containsKey("level") ? toInt(map.get("level"), 1) : 1;
                stack.addUnsafeEnchantment(enchantment, Math.max(1, level));
            }
        }

        return new ConfiguredItem(stack);
    }

    private static void applyPersistentData(PersistentDataContainer container, NamespacedKey key, Object value) {
        if (value instanceof Integer i) {
            container.set(key, PersistentDataType.INTEGER, i);
        } else if (value instanceof Long l) {
            container.set(key, PersistentDataType.LONG, l);
        } else if (value instanceof Double d) {
            container.set(key, PersistentDataType.DOUBLE, d);
        } else if (value instanceof Float f) {
            container.set(key, PersistentDataType.FLOAT, f);
        } else if (value instanceof String s) {
            container.set(key, PersistentDataType.STRING, s);
        } else if (value instanceof Boolean b) {
            container.set(key, PersistentDataType.INTEGER, b ? 1 : 0);
        } else {
            container.set(key, PersistentDataType.STRING, String.valueOf(value));
        }
    }

    public static PotionEffect parsePotionEffect(Object raw) {
        if (raw instanceof ConfigurationSection section) {
            return parsePotionEffectSection(section);
        }
        if (raw instanceof Map<?,?> map) {
            MemoryConfiguration mem = new MemoryConfiguration();
            toConfiguration(mem, map);
            return parsePotionEffectSection(mem);
        }
        return null;
    }

    private static PotionEffect parsePotionEffectSection(ConfigurationSection section) {
        if (section == null) return null;
        String typeName = section.getString("type");
        if (typeName == null) return null;
        PotionEffectType type = PotionEffectType.getByName(typeName.toUpperCase(Locale.ROOT));
        if (type == null) return null;
        int durationSeconds = section.getInt("duration", section.getInt("duration_seconds", 30));
        int durationTicks = section.getInt("duration_ticks", durationSeconds * 20);
        int amplifier = section.getInt("amplifier", 0);
        boolean ambient = section.getBoolean("ambient", false);
        boolean particles = section.getBoolean("particles", true);
        boolean icon = section.getBoolean("icon", true);
        return new PotionEffect(type, Math.max(1, durationTicks), Math.max(0, amplifier), ambient, particles, icon);
    }

    private static int toInt(Object object, int def) {
        if (object instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(object));
        } catch (Exception ex) {
            return def;
        }
    }

    private static double toDouble(Object object, double def) {
        if (object instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(object));
        } catch (Exception ex) {
            return def;
        }
    }

    private static Attribute parseAttribute(String value) {
        if (value == null) return null;
        try {
            return Attribute.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static AttributeModifier.Operation parseOperation(String value) {
        try {
            return AttributeModifier.Operation.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return AttributeModifier.Operation.ADD_NUMBER;
        }
    }

    private static EquipmentSlot parseSlot(Object value) {
        if (value == null) return null;
        try {
            return EquipmentSlot.valueOf(String.valueOf(value).toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return null;
        }
    }

    private static ItemFlag parseFlag(String value) {
        try {
            return ItemFlag.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Enchantment parseEnchantment(String value) {
        if (value == null) {
            return null;
        }
        value = value.toLowerCase(Locale.ROOT);
        Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(value));
        if (enchantment != null) {
            return enchantment;
        }
        enchantment = Enchantment.getByName(value.toUpperCase(Locale.ROOT));
        return enchantment;
    }

    private static void applyHeadTexture(SkullMeta meta, String texture) {
        if (texture == null || texture.isEmpty()) {
            return;
        }
        GameProfile profile = new GameProfile(UUID.randomUUID(), "custom-head");
        profile.getProperties().put("textures", new Property("textures", texture));
        try {
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    private static void toConfiguration(MemoryConfiguration config, Map<?,?> map) {
        for (Map.Entry<?,?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Map<?,?> sub) {
                MemoryConfiguration child = new MemoryConfiguration();
                toConfiguration(child, sub);
                config.createSection(key, child.getValues(true));
            } else if (value instanceof List<?> list) {
                config.set(key, convertList(list));
            } else {
                config.set(key, value);
            }
        }
    }

    private static List<Object> convertList(List<?> list) {
        List<Object> converted = new ArrayList<>();
        for (Object value : list) {
            if (value instanceof Map<?,?> map) {
                MemoryConfiguration child = new MemoryConfiguration();
                toConfiguration(child, map);
                converted.add(child.getValues(true));
            } else if (value instanceof List<?> subList) {
                converted.add(convertList(subList));
            } else {
                converted.add(value);
            }
        }
        return converted;
    }

    private static Color parseColor(String input) {
        input = input.trim();
        if (input.startsWith("#") && (input.length() == 7 || input.length() == 4)) {
            return Color.fromRGB(Integer.decode(input));
        }
        if (input.contains(",")) {
            String[] parts = input.split(",");
            if (parts.length == 3) {
                int r = Math.min(255, Math.max(0, Integer.parseInt(parts[0].trim())));
                int g = Math.min(255, Math.max(0, Integer.parseInt(parts[1].trim())));
                int b = Math.min(255, Math.max(0, Integer.parseInt(parts[2].trim())));
                return Color.fromRGB(r, g, b);
            }
        }
        try {
            java.awt.Color awtColor = (java.awt.Color) java.awt.Color.class.getField(input.toUpperCase(Locale.ROOT)).get(null);
            return Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
        } catch (Exception ignored) {
        }
        return Color.fromRGB(255, 255, 255);
    }
}

