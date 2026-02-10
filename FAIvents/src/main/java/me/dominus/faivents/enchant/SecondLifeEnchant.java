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

public final class SecondLifeEnchant extends Enchantment {

    private static final String LORE = "\u00a77\u0412\u0442\u043e\u0440\u0430\u044f \u0436\u0438\u0437\u043d\u044c";
    private static SecondLifeEnchant instance;
    private static NamespacedKey key;

    private SecondLifeEnchant(NamespacedKey key) {
        super(key);
    }

    public static SecondLifeEnchant get() {
        return instance;
    }

    public static NamespacedKey getKeyStatic() {
        return key;
    }

    public static void register(JavaPlugin plugin) {
        if (instance != null) {
            return;
        }
        key = new NamespacedKey(plugin, "second_life");
        Enchantment existing = Enchantment.getByKey(key);
        if (existing instanceof SecondLifeEnchant) {
            instance = (SecondLifeEnchant) existing;
            return;
        }
        SecondLifeEnchant enchant = new SecondLifeEnchant(key);
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
        return "\u0412\u0442\u043e\u0440\u0430\u044f \u0436\u0438\u0437\u043d\u044c";
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
        return EnchantmentTarget.ARMOR_TORSO;
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
        return item.getType().name().endsWith("_CHESTPLATE");
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

