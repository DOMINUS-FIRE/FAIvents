package me.dominus.faivents.enchant;

import me.dominus.faivents.FAIventsPlugin;
import me.dominus.faivents.util.Msg;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CustomEnchantBooks {

    private CustomEnchantBooks() {
    }

    public static void maybeAddCustomBook(FAIventsPlugin plugin, List<ItemStack> loot, String path) {
        if (plugin == null || loot == null) {
            return;
        }
        boolean enabled = plugin.getEventsConfig().getBoolean(path + ".enabled", true);
        if (!enabled) {
            return;
        }
        double chance = plugin.getEventsConfig().getDouble(path + ".chance", 0.25);
        if (Math.random() > chance) {
            return;
        }

        List<Map<?, ?>> list = plugin.getEventsConfig().getMapList(path + ".list");
        List<BookEntry> entries = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            for (Map<?, ?> raw : list) {
                String type = raw.get("type") != null ? raw.get("type").toString() : "";
                String name = raw.get("name") != null ? raw.get("name").toString() : defaultName(type);
                int weight = 1;
                if (raw.get("weight") != null) {
                    try {
                        weight = Integer.parseInt(raw.get("weight").toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
                entries.add(new BookEntry(type, name, Math.max(1, weight)));
            }
        }
        if (entries.isEmpty()) {
            entries.add(new BookEntry("DRILL", "&6\u0411\u0443\u0440", 5));
            entries.add(new BookEntry("MAGNET", "&6\u041C\u0430\u0433\u043D\u0438\u0442", 3));
            entries.add(new BookEntry("AUTOSMELT", "&6\u0410\u0432\u0442\u043E\u043F\u043B\u0430\u0432\u043A\u0430", 2));
        }

        BookEntry pick = pick(entries);
        Enchantment ench = resolve(pick.type);
        if (ench == null) {
            return;
        }
        loot.add(createBook(ench, pick.name));
    }

    private static Enchantment resolve(String type) {
        if (type == null) {
            return null;
        }
        switch (type.toUpperCase()) {
            case "DRILL":
                return DrillEnchant.get() != null ? DrillEnchant.get() : Enchantment.getByKey(DrillEnchant.getKeyStatic());
            case "MAGNET":
                return MagnetEnchant.get() != null ? MagnetEnchant.get() : Enchantment.getByKey(MagnetEnchant.getKeyStatic());
            case "AUTOSMELT":
                return AutoSmeltEnchant.get() != null ? AutoSmeltEnchant.get() : Enchantment.getByKey(AutoSmeltEnchant.getKeyStatic());
            case "FARMER":
                return FarmerEnchant.get() != null ? FarmerEnchant.get() : Enchantment.getByKey(FarmerEnchant.getKeyStatic());
            case "LUMBERJACK":
                return LumberjackEnchant.get() != null ? LumberjackEnchant.get() : Enchantment.getByKey(LumberjackEnchant.getKeyStatic());
            case "SECOND_LIFE":
                return SecondLifeEnchant.get() != null ? SecondLifeEnchant.get() : Enchantment.getByKey(SecondLifeEnchant.getKeyStatic());
            case "ASSASSIN":
                return AssassinEnchant.get() != null ? AssassinEnchant.get() : Enchantment.getByKey(AssassinEnchant.getKeyStatic());
            case "HORN":
                return HornEnchant.get() != null ? HornEnchant.get() : Enchantment.getByKey(HornEnchant.getKeyStatic());
            case "SHELL":
                return ShellEnchant.get() != null ? ShellEnchant.get() : Enchantment.getByKey(ShellEnchant.getKeyStatic());
            case "BOOM_LEGS":
                return BoomLeggingsEnchant.get() != null ? BoomLeggingsEnchant.get() : Enchantment.getByKey(BoomLeggingsEnchant.getKeyStatic());
            case "UNBREAKABLE":
                return UnbreakableEnchant.get() != null ? UnbreakableEnchant.get() : Enchantment.getByKey(UnbreakableEnchant.getKeyStatic());
            case "PUMPKIN":
                return PumpkinEnchant.get() != null ? PumpkinEnchant.get() : Enchantment.getByKey(PumpkinEnchant.getKeyStatic());
            default:
                return null;
        }
    }

    public static ItemStack createBook(Enchantment ench, String name) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(ench, 1, true);
            meta.setDisplayName(Msg.color(name));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            book.setItemMeta(meta);
        }
        return book;
    }

    private static String defaultName(String type) {
        if (type == null) {
            return "&6\u041A\u043D\u0438\u0433\u0430";
        }
        switch (type.toUpperCase()) {
            case "DRILL":
                return "&6\u0411\u0443\u0440";
            case "MAGNET":
                return "&6\u041C\u0430\u0433\u043D\u0438\u0442";
            case "AUTOSMELT":
                return "&6\u0410\u0432\u0442\u043E\u043F\u043B\u0430\u0432\u043A\u0430";
            case "FARMER":
                return "&6\u0424\u0435\u0440\u043C\u0435\u0440";
            case "LUMBERJACK":
                return "&6\u041B\u0435\u0441\u043E\u0440\u0443\u0431";
            case "SECOND_LIFE":
                return "&6\u0412\u0442\u043E\u0440\u0430\u044F \u0436\u0438\u0437\u043D\u044C";
            case "ASSASSIN":
                return "&6\u0410\u0441\u0441\u0430\u0441\u0438\u043D";
            case "HORN":
                return "&6\u0420\u043E\u0433";
            case "SHELL":
                return "&6\u041F\u0430\u043D\u0446\u0438\u0440\u044C";
            case "BOOM_LEGS":
                return "&6\u041F\u043E\u0434\u0440\u044B\u0432";
            case "UNBREAKABLE":
                return "&6\u041D\u0435\u0440\u0430\u0437\u0440\u0443\u0448\u0438\u043C\u043E\u0441\u0442\u044C";
            case "PUMPKIN":
                return "&6\u0422\u044B\u043A\u0432\u0430";
            default:
                return "&6\u041A\u043D\u0438\u0433\u0430";
        }
    }

    private static BookEntry pick(List<BookEntry> entries) {
        int total = 0;
        for (BookEntry e : entries) {
            total += e.weight;
        }
        int roll = (int) (Math.random() * total);
        int sum = 0;
        for (BookEntry e : entries) {
            sum += e.weight;
            if (roll < sum) {
                return e;
            }
        }
        return entries.get(0);
    }

    private static class BookEntry {
        final String type;
        final String name;
        final int weight;

        BookEntry(String type, String name, int weight) {
            this.type = type;
            this.name = name;
            this.weight = weight;
        }
    }
}
