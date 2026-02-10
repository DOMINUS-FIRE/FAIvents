package me.dominus.faivents.enchant;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentTarget;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FarmerEnchant extends Enchantment {

    private static final String LORE = "\u00a77\u0424\u0435\u0440\u043c\u0435\u0440";
    private static FarmerEnchant instance;
    private static NamespacedKey key;

    private FarmerEnchant(NamespacedKey key) {
        super(key);
    }

    public static FarmerEnchant get() {
        return instance;
    }

    public static NamespacedKey getKeyStatic() {
        return key;
    }

    public static void register(JavaPlugin plugin) {
        if (instance != null) {
            return;
        }
        key = new NamespacedKey(plugin, "farmer");
        Enchantment existing = Enchantment.getByKey(key);
        if (existing instanceof FarmerEnchant) {
            instance = (FarmerEnchant) existing;
            return;
        }
        FarmerEnchant enchant = new FarmerEnchant(key);
        try {
            Field f = Enchantment.class.getDeclaredField("acceptingNew");
            f.setAccessible(true);
            f.set(null, true);
        } catch (Exception ignored) {
        }
        try {
            Enchantment.registerEnchantment(enchant);
        } catch (IllegalArgumentException ignored) {
        }
        instance = enchant;
    }

    @Override
    public String getName() {
        return "\u0424\u0435\u0440\u043c\u0435\u0440";
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getStartLevel() {
        return 1;
    }

    @Override
    public EnchantmentTarget getItemTarget() {
        return EnchantmentTarget.TOOL;
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public boolean isCursed() {
        return false;
    }

    @Override
    public boolean conflictsWith(Enchantment other) {
        return false;
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        return item.getType().name().endsWith("_HOE") || item.getType() == Material.BOOK || item.getType() == Material.ENCHANTED_BOOK;
    }

    public static boolean has(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (instance != null && item.containsEnchantment(instance)) {
            return true;
        }
        if (key != null && item.getItemMeta() != null) {
            Map<Enchantment, Integer> enchants = item.getItemMeta().getEnchants();
            for (Enchantment e : enchants.keySet()) {
                if (e != null && key.equals(e.getKey())) {
                    return true;
                }
            }
        }
        return hasLore(item, LORE);
    }

    public static void addLore(ItemStack item) {
        addLoreLine(item, LORE);
    }

    private static boolean hasLore(ItemStack item, String line) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getLore() == null) {
            return false;
        }
        for (String l : meta.getLore()) {
            if (line.equals(l)) {
                return true;
            }
        }
        return false;
    }

    private static void addLoreLine(ItemStack item, String line) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        if (!lore.contains(line)) {
            lore.add(line);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }
}

