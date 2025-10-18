package com.outlaw.bounties.item;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

/**
 * Represents an item stack described in the configuration with rich metadata.
 * <p>
 * The instance acts as a prototype and can be cloned with {@link #createStack(int)}
 * whenever a new stack is required. Any colour codes present in the display name
 * are already translated.
 */
public class ConfiguredItem {

    private final ItemStack prototype;

    public ConfiguredItem(ItemStack prototype) {
        this.prototype = prototype;
    }

    /**
     * Create a clone of the configured item.
     *
     * @param amountOverride the desired amount, or {@code <= 0} to keep the
     *                       prototype amount.
     * @return A cloned {@link ItemStack} ready to be given to a player.
     */
    public ItemStack createStack(int amountOverride) {
        ItemStack copy = prototype.clone();
        if (amountOverride > 0) {
            copy.setAmount(amountOverride);
        }
        return copy;
    }

    public ItemStack prototype() {
        return prototype.clone();
    }

    public int baseAmount() {
        return prototype.getAmount();
    }

    public Material material() {
        return prototype.getType();
    }

    public boolean isStackable() {
        return prototype.getMaxStackSize() > 1;
    }

    /**
     * Retrieve the coloured display name if present.
     */
    public String displayName() {
        ItemMeta meta = prototype.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return null;
    }

    /**
     * Retrieve a plain text representation of the item suitable for lore lines.
     */
    public String plainName() {
        String display = displayName();
        if (display != null) {
            return ChatColor.stripColor(display);
        }
        String formatted = prototype.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }

    /**
     * Returns the display name preserving colours if any, otherwise the formatted
     * material name.
     */
    public String formattedName() {
        String display = displayName();
        if (display != null && !display.isEmpty()) {
            return display;
        }
        String formatted = prototype.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        formatted = Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
        return ChatColor.YELLOW + formatted;
    }
}

