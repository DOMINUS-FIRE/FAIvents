package me.dominus.faivents.command;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import me.dominus.faivents.enchant.AssassinEnchant;
import me.dominus.faivents.enchant.AutoSmeltEnchant;
import me.dominus.faivents.enchant.BoomLeggingsEnchant;
import me.dominus.faivents.enchant.DrillEnchant;
import me.dominus.faivents.enchant.FarmerEnchant;
import me.dominus.faivents.enchant.HornEnchant;
import me.dominus.faivents.enchant.LumberjackEnchant;
import me.dominus.faivents.enchant.MagnetEnchant;
import me.dominus.faivents.enchant.SecondLifeEnchant;
import me.dominus.faivents.enchant.ShellEnchant;
import me.dominus.faivents.quarry.QuarryManager;
import me.dominus.faivents.util.Msg;

public class ShopCommand implements CommandExecutor, Listener {

    private static final String TITLE_MAIN = ChatColor.DARK_GRAY + "\u041C\u0430\u0433\u0430\u0437\u0438\u043D";
    private static final String TITLE_ENCHANTS = ChatColor.DARK_AQUA + "\u041C\u0430\u0433\u0430\u0437\u0438\u043D: \u0417\u0430\u0447\u0430\u0440\u043E\u0432\u0430\u043D\u0438\u044F";
    private static final String TITLE_QUARRY = ChatColor.DARK_AQUA + "\u041C\u0430\u0433\u0430\u0437\u0438\u043D: \u041A\u0430\u0440\u044C\u0435\u0440\u044B";

    private final JavaPlugin plugin;
    private final QuarryManager quarryManager;
    private final Map<String, org.bukkit.NamespacedKey> exclusiveKeys = new HashMap<>();

    public ShopCommand(JavaPlugin plugin, QuarryManager quarryManager) {
        this.plugin = plugin;
        this.quarryManager = quarryManager;
        exclusiveKeys.put("second_life", new org.bukkit.NamespacedKey(plugin, "excl_second_life"));
        exclusiveKeys.put("assassin", new org.bukkit.NamespacedKey(plugin, "excl_assassin"));
        exclusiveKeys.put("horn", new org.bukkit.NamespacedKey(plugin, "excl_horn"));
        exclusiveKeys.put("shell", new org.bukkit.NamespacedKey(plugin, "excl_shell"));
        exclusiveKeys.put("boom", new org.bukkit.NamespacedKey(plugin, "excl_boom"));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Msg.send(sender, "&c\u041A\u043E\u043C\u0430\u043D\u0434\u0430 \u0442\u043E\u043B\u044C\u043A\u043E \u0434\u043B\u044F \u0438\u0433\u0440\u043E\u043A\u043E\u0432.");
            return true;
        }
        Player player = (Player) sender;
        player.openInventory(buildMainMenu());
        return true;
    }

    private Inventory buildMainMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN);
        inv.setItem(11, item(Material.ENCHANTED_BOOK, "&6\u0417\u0430\u0447\u0430\u0440\u043E\u0432\u0430\u043D\u0438\u044F"));
        inv.setItem(15, item(Material.DISPENSER, "&a\u041A\u0430\u0440\u044C\u0435\u0440\u044B"));
        return inv;
    }

    private Inventory buildEnchantMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_ENCHANTS);
        inv.setItem(10, pricedBook(DrillEnchant.get(), "&6\u0411\u0443\u0440", cost(Material.DIAMOND, 16),
                List.of(Msg.color("&7\u0411\u0443\u0440\u0438\u0442 \u0431\u043B\u043E\u043A\u0438 \u043D\u0430\u0441\u043A\u0432\u043E\u0437\u044C"))));
        inv.setItem(11, pricedBook(MagnetEnchant.get(), "&6\u041C\u0430\u0433\u043D\u0438\u0442", cost(Material.DIAMOND, 12),
                List.of(Msg.color("&7\u041F\u0440\u0438\u0442\u044F\u0433\u0438\u0432\u0430\u0435\u0442 \u0434\u0440\u043E\u043F\u044B \u043A \u0442\u0435\u0431\u0435"))));
        inv.setItem(12, pricedBook(AutoSmeltEnchant.get(), "&6\u0410\u0432\u0442\u043E\u043F\u043B\u0430\u0432\u043A\u0430", cost(Material.DIAMOND, 12),
                List.of(Msg.color("&7\u0421\u0440\u0430\u0437\u0443 \u043F\u043B\u0430\u0432\u0438\u0442 \u0440\u0443\u0434\u0443"))));
        inv.setItem(14, pricedBook(FarmerEnchant.get(), "&6\u0424\u0435\u0440\u043C\u0435\u0440", cost(Material.DIAMOND, 12),
                List.of(Msg.color("&7\u0410\u0432\u0442\u043E\u0441\u0431\u043E\u0440 \u0443\u0440\u043E\u0436\u0430\u044F"))));
        inv.setItem(15, pricedBook(LumberjackEnchant.get(), "&6\u041B\u0435\u0441\u043E\u0440\u0443\u0431", cost(Material.DIAMOND, 8),
                List.of(Msg.color("&7\u0420\u0443\u0431\u0438\u0442 \u0434\u0435\u0440\u0435\u0432\u043E \u0446\u0435\u043B\u0438\u043A\u043E\u043C"))));
        inv.setItem(16, exclusiveDisplay(player, "second_life",
                pricedBook(SecondLifeEnchant.get(), "&6\u0412\u0442\u043E\u0440\u0430\u044F \u0436\u0438\u0437\u043D\u044C", cost(Material.DIAMOND, 32),
                        List.of(Msg.color("&7\u0421\u043F\u0430\u0441\u0430\u0435\u0442 \u043E\u0442 \u0441\u043C\u0435\u0440\u0442\u0438 \u0440\u0430\u0437")))));
        inv.setItem(19, exclusiveDisplay(player, "assassin",
                pricedBook(AssassinEnchant.get(), "&6\u0423\u0431\u0438\u0439\u0446\u0430", cost(Material.DIAMOND, 28),
                        List.of(Msg.color("&7\u0414\u0430\u0451\u0442 \u043D\u0435\u0432\u0438\u0434\u0438\u043C\u043E\u0441\u0442\u044C \u043F\u043E\u0441\u043B\u0435 \u0443\u0434\u0430\u0440\u0430")))));
        inv.setItem(20, exclusiveDisplay(player, "horn",
                pricedBook(HornEnchant.get(), "&6\u0420\u043E\u0433", cost(Material.DIAMOND, 24),
                        List.of(Msg.color("&7\u041E\u0442\u0442\u0430\u043B\u043A\u0438\u0432\u0430\u0435\u0442 \u043F\u0440\u043E\u0442\u0438\u0432\u043D\u0438\u043A\u043E\u0432")))));
        inv.setItem(21, exclusiveDisplay(player, "shell",
                pricedBook(ShellEnchant.get(), "&6\u041F\u0430\u043D\u0446\u0438\u0440\u044C", cost(Material.DIAMOND, 24),
                        List.of(Msg.color("&7\u0417\u0430\u0449\u0438\u0442\u0430 \u043D\u0430 \u0448\u0438\u0444\u0442\u0435")))));
        inv.setItem(22, exclusiveDisplay(player, "boom",
                pricedBook(BoomLeggingsEnchant.get(), "&6\u041F\u043E\u0434\u0440\u044B\u0432", cost(Material.DIAMOND, 24),
                        List.of(Msg.color("&7\u0412\u0437\u0440\u044B\u0432 \u043F\u0440\u0438 \u0432\u044B\u0441\u043E\u043A\u043E\u043C \u0443\u0440\u043E\u043D\u0435")))));
        inv.setItem(25, item(Material.ARROW, "&e\u041D\u0430\u0437\u0430\u0434"));
        return inv;
    }

    private Inventory buildQuarryMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_QUARRY);
        inv.setItem(11, pricedItem(quarryManager.createQuarryItem(1), "&a\u041A\u0430\u0440\u044C\u0435\u0440 I", quarryCost1(), quarryFeatures(1)));
        inv.setItem(12, pricedItem(quarryManager.createQuarryItem(2), "&a\u041A\u0430\u0440\u044C\u0435\u0440 II", quarryCost2(), quarryFeatures(2)));
        inv.setItem(13, pricedItem(quarryManager.createQuarryItem(3), "&a\u041A\u0430\u0440\u044C\u0435\u0440 III", quarryCost3(), quarryFeatures(3)));
        inv.setItem(14, pricedItem(quarryManager.createQuarryItem(4), "&a\u041A\u0430\u0440\u044C\u0435\u0440 IV", quarryCost4(), quarryFeatures(4)));
        inv.setItem(15, pricedItem(quarryManager.createQuarryItem(5), "&a\u041A\u0430\u0440\u044C\u0435\u0440 V", quarryCost5(), quarryFeatures(5)));
        inv.setItem(25, item(Material.ARROW, "&e\u041D\u0430\u0437\u0430\u0434"));
        return inv;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = e.getView().getTitle();
        if (!title.equals(TITLE_MAIN) && !title.equals(TITLE_ENCHANTS) && !title.equals(TITLE_QUARRY)) {
            return;
        }
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (title.equals(TITLE_MAIN)) {
            if (slot == 11) {
                player.openInventory(buildEnchantMenu(player));
            } else if (slot == 15) {
                player.openInventory(buildQuarryMenu());
            }
            return;
        }

        if (title.equals(TITLE_ENCHANTS)) {
            if (slot == 25) {
                player.openInventory(buildMainMenu());
                return;
            }
            handleEnchantBuy(player, slot);
            return;
        }

        if (title.equals(TITLE_QUARRY)) {
            if (slot == 25) {
                player.openInventory(buildMainMenu());
                return;
            }
            int level = 0;
            Map<Material, Integer> cost = null;
            if (slot == 11) {
                level = 1;
                cost = quarryCost1();
            } else if (slot == 12) {
                level = 2;
                cost = quarryCost2();
            } else if (slot == 13) {
                level = 3;
                cost = quarryCost3();
            } else if (slot == 14) {
                level = 4;
                cost = quarryCost4();
            } else if (slot == 15) {
                level = 5;
                cost = quarryCost5();
            }
            if (level > 0) {
                if (!takeItems(player, cost)) {
                    Msg.send(player, "&c\u041D\u0435\u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u0440\u0435\u0441\u0443\u0440\u0441\u043E\u0432.");
                    return;
                }
                player.getInventory().addItem(quarryManager.createQuarryItem(level));
                Msg.send(player, "&a\u041A\u0430\u0440\u044C\u0435\u0440 \u043A\u0443\u043F\u043B\u0435\u043D.");
            }
        }
    }

    private void handleEnchantBuy(Player player, int slot) {
        Enchantment ench;
        String name;
        Map<Material, Integer> cost;
        String exclusiveKey = null;
        switch (slot) {
            case 10:
                ench = DrillEnchant.get();
                name = "&6\u0411\u0443\u0440";
                cost = cost(Material.DIAMOND, 16);
                break;
            case 11:
                ench = MagnetEnchant.get();
                name = "&6\u041C\u0430\u0433\u043D\u0438\u0442";
                cost = cost(Material.DIAMOND, 12);
                break;
            case 12:
                ench = AutoSmeltEnchant.get();
                name = "&6\u0410\u0432\u0442\u043E\u043F\u043B\u0430\u0432\u043A\u0430";
                cost = cost(Material.DIAMOND, 12);
                break;
            case 14:
                ench = FarmerEnchant.get();
                name = "&6\u0424\u0435\u0440\u043C\u0435\u0440";
                cost = cost(Material.DIAMOND, 12);
                break;
            case 15:
                ench = LumberjackEnchant.get();
                name = "&6\u041B\u0435\u0441\u043E\u0440\u0443\u0431";
                cost = cost(Material.DIAMOND, 8);
                break;
            case 16:
                ench = SecondLifeEnchant.get();
                name = "&6\u0412\u0442\u043E\u0440\u0430\u044F \u0436\u0438\u0437\u043D\u044C";
                cost = cost(Material.DIAMOND, 32);
                exclusiveKey = "second_life";
                break;
            case 19:
                ench = AssassinEnchant.get();
                name = "&6\u0423\u0431\u0438\u0439\u0446\u0430";
                cost = cost(Material.DIAMOND, 28);
                exclusiveKey = "assassin";
                break;
            case 20:
                ench = HornEnchant.get();
                name = "&6\u0420\u043E\u0433";
                cost = cost(Material.DIAMOND, 24);
                exclusiveKey = "horn";
                break;
            case 21:
                ench = ShellEnchant.get();
                name = "&6\u041F\u0430\u043D\u0446\u0438\u0440\u044C";
                cost = cost(Material.DIAMOND, 24);
                exclusiveKey = "shell";
                break;
            case 22:
                ench = BoomLeggingsEnchant.get();
                name = "&6\u041F\u043E\u0434\u0440\u044B\u0432";
                cost = cost(Material.DIAMOND, 24);
                exclusiveKey = "boom";
                break;
            default:
                return;
        }
        if (exclusiveKey != null && isExclusiveBought(player, exclusiveKey)) {
            Msg.send(player, "&c\u042D\u0442\u0430 \u043A\u043D\u0438\u0433\u0430 \u0443\u0436\u0435 \u043A\u0443\u043F\u043B\u0435\u043D\u0430.");
            return;
        }
        if (!takeItems(player, cost)) {
            Msg.send(player, "&c\u041D\u0435\u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u0440\u0435\u0441\u0443\u0440\u0441\u043E\u0432.");
            return;
        }
        player.getInventory().addItem(createBook(ench, name));
        if (exclusiveKey != null) {
            markExclusive(player, exclusiveKey);
            player.openInventory(buildEnchantMenu(player));
        }
        Msg.send(player, "&a\u041A\u0443\u043F\u043B\u0435\u043D\u043E: " + Msg.color(name));
    }

    private boolean isExclusiveBought(Player player, String key) {
        org.bukkit.NamespacedKey ns = exclusiveKeys.get(key);
        if (ns == null) {
            return false;
        }
        Byte val = player.getPersistentDataContainer().get(ns, org.bukkit.persistence.PersistentDataType.BYTE);
        return val != null && val == 1;
    }

    private void markExclusive(Player player, String key) {
        org.bukkit.NamespacedKey ns = exclusiveKeys.get(key);
        if (ns == null) {
            return;
        }
        player.getPersistentDataContainer().set(ns, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);
    }

    private ItemStack exclusiveDisplay(Player player, String key, ItemStack item) {
        if (!isExclusiveBought(player, key)) {
            return item;
        }
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.color("&c\u0423\u0436\u0435 \u043A\u0443\u043F\u043B\u0435\u043D\u043E"));
            List<String> lore = new ArrayList<>();
            lore.add(Msg.color("&7\u042D\u0442\u0430 \u043A\u043D\u0438\u0433\u0430 \u043F\u043E\u043A\u0443\u043F\u0430\u0435\u0442\u0441\u044F \u043E\u0434\u0438\u043D \u0440\u0430\u0437."));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private Map<Material, Integer> quarryCost1() {
        Map<Material, Integer> cost = new EnumMap<>(Material.class);
        cost.put(Material.EMERALD_BLOCK, 1);
        cost.put(Material.DIAMOND_BLOCK, 1);
        cost.put(Material.GOLD_BLOCK, 1);
        cost.put(Material.IRON_INGOT, 2);
        cost.put(Material.DIAMOND_PICKAXE, 1);
        cost.put(Material.IRON_BLOCK, 2);
        cost.put(Material.DISPENSER, 1);
        return cost;
    }

    private Map<Material, Integer> quarryCost2() {
        Map<Material, Integer> cost = new EnumMap<>(Material.class);
        cost.put(Material.EMERALD_BLOCK, 2);
        cost.put(Material.DIAMOND_BLOCK, 2);
        cost.put(Material.GOLD_BLOCK, 2);
        cost.put(Material.IRON_INGOT, 8);
        cost.put(Material.DIAMOND_PICKAXE, 1);
        cost.put(Material.IRON_BLOCK, 4);
        cost.put(Material.DISPENSER, 1);
        return cost;
    }

    private Map<Material, Integer> quarryCost3() {
        Map<Material, Integer> cost = new EnumMap<>(Material.class);
        cost.put(Material.EMERALD_BLOCK, 3);
        cost.put(Material.DIAMOND_BLOCK, 3);
        cost.put(Material.GOLD_BLOCK, 3);
        cost.put(Material.IRON_INGOT, 16);
        cost.put(Material.DIAMOND_PICKAXE, 1);
        cost.put(Material.IRON_BLOCK, 8);
        cost.put(Material.DISPENSER, 1);
        cost.put(Material.NETHER_STAR, 1);
        return cost;
    }

    private Map<Material, Integer> quarryCost4() {
        Map<Material, Integer> cost = new EnumMap<>(Material.class);
        cost.put(Material.EMERALD_BLOCK, 4);
        cost.put(Material.DIAMOND_BLOCK, 4);
        cost.put(Material.GOLD_BLOCK, 4);
        cost.put(Material.IRON_INGOT, 24);
        cost.put(Material.DIAMOND_PICKAXE, 1);
        cost.put(Material.IRON_BLOCK, 12);
        cost.put(Material.DISPENSER, 1);
        cost.put(Material.NETHER_STAR, 1);
        cost.put(Material.DIAMOND_HELMET, 1);
        return cost;
    }

    private Map<Material, Integer> quarryCost5() {
        Map<Material, Integer> cost = new EnumMap<>(Material.class);
        cost.put(Material.EMERALD_BLOCK, 6);
        cost.put(Material.DIAMOND_BLOCK, 6);
        cost.put(Material.GOLD_BLOCK, 6);
        cost.put(Material.IRON_INGOT, 32);
        cost.put(Material.DIAMOND_PICKAXE, 1);
        cost.put(Material.IRON_BLOCK, 16);
        cost.put(Material.DISPENSER, 1);
        cost.put(Material.NETHER_STAR, 2);
        cost.put(Material.ANCIENT_DEBRIS, 2);
        return cost;
    }

    private Map<Material, Integer> cost(Material mat, int amount) {
        Map<Material, Integer> map = new EnumMap<>(Material.class);
        map.put(mat, amount);
        return map;
    }

    private boolean takeItems(Player player, Map<Material, Integer> cost) {
        PlayerInventory inv = player.getInventory();
        for (Map.Entry<Material, Integer> e : cost.entrySet()) {
            if (!inv.containsAtLeast(new ItemStack(e.getKey()), e.getValue())) {
                return false;
            }
        }
        for (Map.Entry<Material, Integer> e : cost.entrySet()) {
            removeItems(inv, e.getKey(), e.getValue());
        }
        return true;
    }

    private void removeItems(PlayerInventory inv, Material mat, int amount) {
        int remaining = amount;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != mat) {
                continue;
            }
            int take = Math.min(remaining, it.getAmount());
            it.setAmount(it.getAmount() - take);
            remaining -= take;
            if (it.getAmount() <= 0) {
                inv.setItem(i, null);
            }
            if (remaining <= 0) {
                return;
            }
        }
    }

    private ItemStack item(Material mat, String name) {
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.color(name));
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack pricedItem(ItemStack base, String name, Map<Material, Integer> cost, List<String> extra) {
        ItemStack it = base.clone();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.color(name));
            List<String> lore = new ArrayList<>();
            lore.addAll(extra);
            lore.addAll(priceLore(cost));
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack pricedBook(Enchantment ench, String name, Map<Material, Integer> cost, List<String> description) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(ench, 1, true);
            meta.setDisplayName(Msg.color(name));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            List<String> lore = new ArrayList<>();
            if (description != null) {
                lore.addAll(description);
            }
            lore.addAll(priceLore(cost));
            meta.setLore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }

    private ItemStack createBook(Enchantment ench, String name) {
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

    private List<String> priceLore(Map<Material, Integer> cost) {
        List<String> lore = new ArrayList<>();
        lore.add(Msg.color("&7\u0426\u0435\u043D\u0430:"));
        for (Map.Entry<Material, Integer> e : cost.entrySet()) {
            lore.add(Msg.color("&f- " + prettyMaterial(e.getKey()) + ": &e" + e.getValue()));
        }
        return lore;
    }

    private List<String> quarryFeatures(int level) {
        List<String> lore = new ArrayList<>();
        lore.add(Msg.color("&7\u0411\u044B\u0441\u0442\u0440\u043E\u0442\u0430: &f" + level));
        if (level == 1) {
            lore.add(Msg.color("&7\u0422\u043E\u043F\u043B\u0438\u0432\u043E: &f\u043F\u0435\u0447\u043D\u043E\u0435"));
            lore.add(Msg.color("&7\u0425\u0440\u0430\u043D\u0438\u043B\u0438\u0449\u0435: &f\u043D\u0435\u0442"));
        } else if (level == 2) {
            lore.add(Msg.color("&7\u0422\u043E\u043F\u043B\u0438\u0432\u043E: &f\u043F\u0435\u0447\u043D\u043E\u0435 + \u0432\u043E\u0434\u0430"));
            lore.add(Msg.color("&7\u0425\u0440\u0430\u043D\u0438\u043B\u0438\u0449\u0435: &f6 \u0440\u044F\u0434\u043E\u0432"));
        } else if (level == 3) {
            lore.add(Msg.color("&7\u0421\u043E\u043B\u043D\u0435\u0447\u043D\u043E\u0435 \u043F\u0438\u0442\u0430\u043D\u0438\u0435"));
            lore.add(Msg.color("&7\u0425\u0440\u0430\u043D\u0438\u043B\u0438\u0449\u0435: &f18 \u0440\u044F\u0434\u043E\u0432"));
        } else if (level == 4) {
            lore.add(Msg.color("&7\u0424\u0438\u043B\u044C\u0442\u0440 \u0438 \u0448\u0451\u043B\u043A\u043E\u0432\u043E\u0435 \u043A\u0430\u0441\u0430\u043D\u0438\u0435"));
            lore.add(Msg.color("&7\u0425\u0440\u0430\u043D\u0438\u043B\u0438\u0449\u0435: &f20 \u0440\u044F\u0434\u043E\u0432"));
        } else if (level == 5) {
            lore.add(Msg.color("&7\u0427\u0430\u043D\u043A\u0438 \u0432\u043E\u043A\u0440\u0443\u0433, /chase"));
            lore.add(Msg.color("&7\u0425\u0440\u0430\u043D\u0438\u043B\u0438\u0449\u0435: &f30 \u0440\u044F\u0434\u043E\u0432"));
        }
        return lore;
    }

    private String prettyMaterial(Material mat) {
        return switch (mat) {
            case DIAMOND -> "\u0410\u043B\u043C\u0430\u0437\u044B";
            case EMERALD_BLOCK -> "\u0418\u0437\u0443\u043C\u0440\u0443\u0434\u043D\u044B\u0439 \u0431\u043B\u043E\u043A";
            case DIAMOND_BLOCK -> "\u0410\u043B\u043C\u0430\u0437\u043D\u044B\u0439 \u0431\u043B\u043E\u043A";
            case GOLD_BLOCK -> "\u0417\u043E\u043B\u043E\u0442\u043E\u0439 \u0431\u043B\u043E\u043A";
            case IRON_BLOCK -> "\u0416\u0435\u043B\u0435\u0437\u043D\u044B\u0439 \u0431\u043B\u043E\u043A";
            case IRON_INGOT -> "\u0416\u0435\u043B\u0435\u0437\u043D\u044B\u0439 \u0441\u043B\u0438\u0442\u043E\u043A";
            case DIAMOND_PICKAXE -> "\u0410\u043B\u043C\u0430\u0437\u043D\u0430\u044F \u043A\u0438\u0440\u043A\u0430";
            case DISPENSER -> "\u0420\u0430\u0437\u0434\u0430\u0442\u0447\u0438\u043A";
            case NETHER_STAR -> "\u0417\u0432\u0435\u0437\u0434\u0430 \u041D\u0435\u0437\u0435\u0440\u0430";
            case DIAMOND_HELMET -> "\u0410\u043B\u043C\u0430\u0437\u043D\u044B\u0439 \u0448\u043B\u0435\u043C";
            case ANCIENT_DEBRIS -> "\u0414\u0440\u0435\u0432\u043D\u0438\u0439 \u043E\u0431\u043B\u043E\u043C\u043E\u043A";
            default -> mat.name().toLowerCase().replace('_', ' ');
        };
    }
}
