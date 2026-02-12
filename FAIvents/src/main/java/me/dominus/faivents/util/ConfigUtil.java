package me.dominus.faivents.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class ConfigUtil {

    private static final Random RANDOM = new Random();

    private ConfigUtil() {
    }

    public static String getString(FileConfiguration cfg, String path, String def) {
        String v = cfg.getString(path);
        return v != null ? v : def;
    }

    public static int getInt(FileConfiguration cfg, String path, int def) {
        return cfg.getInt(path, def);
    }

    public static double getDouble(FileConfiguration cfg, String path, double def) {
        return cfg.getDouble(path, def);
    }

    public static boolean getBoolean(FileConfiguration cfg, String path, boolean def) {
        return cfg.getBoolean(path, def);
    }

    public static List<String> getStringList(FileConfiguration cfg, String path) {
        List<String> list = cfg.getStringList(path);
        return list != null ? list : new ArrayList<>();
    }

    public static World getWorld(FileConfiguration cfg, String path, World def) {
        String name = cfg.getString(path);
        if (name == null) {
            return def;
        }
        World w = Bukkit.getWorld(name);
        return w != null ? w : def;
    }

    public static Location getLocation(FileConfiguration cfg, String path, World defWorld) {
        World world = getWorld(cfg, path + ".world", defWorld);
        double x = cfg.getDouble(path + ".x");
        double y = cfg.getDouble(path + ".y");
        double z = cfg.getDouble(path + ".z");
        return new Location(world, x, y, z);
    }

    public static Material getMaterial(String name, Material def) {
        if (name == null) {
            return def;
        }
        Material m = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return m != null ? m : def;
    }

    public static EntityType getEntityType(String name, EntityType def) {
        if (name == null) {
            return def;
        }
        try {
            return EntityType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }

    public static PotionEffectType getPotionEffectType(String name) {
        if (name == null) {
            return null;
        }
        return PotionEffectType.getByName(name.toUpperCase(Locale.ROOT));
    }

    public static PotionEffect parseEffect(String spec, int defaultSeconds) {
        if (spec == null) {
            return null;
        }
        String[] parts = spec.split(":");
        if (parts.length == 0) {
            return null;
        }
        PotionEffectType type = getPotionEffectType(parts[0]);
        if (type == null) {
            return null;
        }
        int level = 0;
        int seconds = defaultSeconds;
        if (parts.length >= 2) {
            try {
                level = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        if (parts.length >= 3) {
            try {
                seconds = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
            }
        }
        return new PotionEffect(type, seconds * 20, Math.max(0, level));
    }

    public static List<ItemStack> rollLoot(FileConfiguration cfg, String path) {
        List<ItemStack> out = new ArrayList<>();
        List<Map<?, ?>> list = cfg.getMapList(path);
        if (list == null || list.isEmpty()) {
            return out;
        }
        for (Map<?, ?> raw : list) {
            String matName = toString(raw.get("material"));
            Material mat = getMaterial(matName, null);
            if (mat == null) {
                continue;
            }
            double chance = toDouble(raw.get("chance"), 1.0);
            if (RANDOM.nextDouble() > chance) {
                continue;
            }
            int min = (int) toDouble(raw.get("min"), 1);
            int max = (int) toDouble(raw.get("max"), min);
            int amount = RandUtil.nextInt(Math.max(1, min), Math.max(1, max));
            ItemStack item = new ItemStack(mat, amount);
            ItemMeta meta = item.getItemMeta();
            String name = toString(raw.get("name"));
            if (meta != null && name != null && !name.isEmpty()) {
                meta.setDisplayName(Msg.color(name));
            }
            List<String> lore = toStringList(raw.get("lore"));
            if (meta != null && lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<>();
                for (String l : lore) {
                    colored.add(Msg.color(l));
                }
                meta.setLore(colored);
            }
            List<String> ench = toStringList(raw.get("enchants"));
            if (meta != null && ench != null) {
                for (String e : ench) {
                    String[] p = e.split(":");
                    if (p.length >= 2) {
                        Enchantment enchantment = Enchantment.getByName(p[0].toUpperCase(Locale.ROOT));
                        if (enchantment != null) {
                            int lvl = 1;
                            try {
                                lvl = Integer.parseInt(p[1]);
                            } catch (NumberFormatException ignored) {
                            }
                            meta.addEnchant(enchantment, lvl, true);
                        }
                    }
                }
            }
            if (meta != null) {
                item.setItemMeta(meta);
            }
            out.add(item);
        }
        return out;
    }

    private static String toString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static List<String> toStringList(Object o) {
        if (o instanceof List) {
            List<?> list = (List<?>) o;
            List<String> out = new ArrayList<>();
            for (Object v : list) {
                if (v != null) {
                    out.add(String.valueOf(v));
                }
            }
            return out;
        }
        return new ArrayList<>();
    }

    private static double toDouble(Object o, double def) {
        if (o == null) {
            return def;
        }
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}




