package me.dominus.faivents.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import me.dominus.faivents.FAIventsPlugin;
import me.dominus.faivents.enchant.AssassinEnchant;
import me.dominus.faivents.enchant.AutoSmeltEnchant;
import me.dominus.faivents.enchant.BoomLeggingsEnchant;
import me.dominus.faivents.enchant.CustomEnchantBooks;
import me.dominus.faivents.enchant.DrillEnchant;
import me.dominus.faivents.enchant.FarmerEnchant;
import me.dominus.faivents.enchant.HornEnchant;
import me.dominus.faivents.enchant.LumberjackEnchant;
import me.dominus.faivents.enchant.MagnetEnchant;
import me.dominus.faivents.enchant.SecondLifeEnchant;
import me.dominus.faivents.enchant.ShellEnchant;
import me.dominus.faivents.quarry.QuarryManager;
import me.dominus.faivents.util.Msg;
import me.dominus.faivents.util.RandUtil;

public class CasinoCommand implements CommandExecutor, Listener {

    private static final String TITLE = ChatColor.DARK_GRAY + "\u041A\u0430\u0437\u0438\u043D\u043E";
    private static final String WHEEL_TITLE = ChatColor.DARK_GRAY + "\u041A\u0430\u0437\u0438\u043D\u043E: \u041A\u043E\u043B\u0435\u0441\u043E";
    private static final String COIN_TITLE = ChatColor.DARK_GRAY + "\u041A\u0430\u0437\u0438\u043D\u043E: \u041E\u0440\u0435\u043B/\u0420\u0435\u0448\u043A\u0430";
    private static final String BOX_TITLE = ChatColor.DARK_GRAY + "\u041A\u0430\u0437\u0438\u043D\u043E: \u0422\u0430\u0439\u043D\u044B\u0439 \u044F\u0449\u0438\u043A";
    private static final int[] BET_SLOTS = new int[] { 11, 12, 13, 14, 15 };
    private static final int[] SPIN_SLOTS = new int[] { 2, 3, 4, 5, 6 };
    private static final int[] COIN_BET_SLOTS = new int[] { 20, 21, 22 };
    private static final int[] BOX_BET_SLOTS = new int[] { 24, 25, 26 };
    private static final int BACK_SLOT = 18;

    private final FAIventsPlugin plugin;
    private final QuarryManager quarryManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Boolean> spinning = new HashMap<>();

    public CasinoCommand(FAIventsPlugin plugin, QuarryManager quarryManager) {
        this.plugin = plugin;
        this.quarryManager = quarryManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Msg.send(sender, "&c\u041A\u043E\u043C\u0430\u043D\u0434\u0430 \u0442\u043E\u043B\u044C\u043A\u043E \u0434\u043B\u044F \u0438\u0433\u0440\u043E\u043A\u043E\u0432.");
            return true;
        }
        if (!plugin.getCasinoConfig().getBoolean("casino.enabled", true)) {
            Msg.send(sender, "&c\u041A\u0430\u0437\u0438\u043D\u043E \u043E\u0442\u043A\u043B\u044E\u0447\u0435\u043D\u043E.");
            return true;
        }
        plugin.reloadCasinoConfig();
        Player player = (Player) sender;
        player.openInventory(buildMainMenu());
        return true;
    }

    private Inventory buildMainMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        inv.setItem(11, item(Material.NETHER_STAR, "&e\u041A\u043E\u043B\u0435\u0441\u043E \u0444\u043E\u0440\u0442\u0443\u043D\u044B",
                List.of(
                        Msg.color("&7\u0421\u0442\u0430\u0432\u043A\u0430 \u0441\u043D\u0438\u043C\u0430\u0435\u0442\u0441\u044F \u0441 \u0430\u043B\u043C\u0430\u0437\u043E\u0432"),
                        Msg.color("&7\u0427\u0435\u043C \u0431\u043E\u043B\u044C\u0448\u0435 \u0441\u0442\u0430\u0432\u043A\u0430, \u0442\u0435\u043C \u0432\u044B\u0448\u0435 \u0448\u0430\u043D\u0441")
                )));
        inv.setItem(13, item(Material.SUNFLOWER, "&6\u041E\u0440\u0435\u043B/\u0420\u0435\u0448\u043A\u0430",
                List.of(
                        Msg.color("&7\u0428\u0430\u043D\u0441: 50/50"),
                        Msg.color("&7\u0412\u044B\u0438\u0433\u0440\u044B\u0448: x2")
                )));
        inv.setItem(15, item(Material.CHEST, "&6\u0422\u0430\u0439\u043D\u044B\u0439 \u044F\u0449\u0438\u043A",
                List.of(
                        Msg.color("&7\u0428\u0430\u043D\u0441 \u0432\u044B\u0438\u0433\u0440\u044B\u0448\u0430 \u043E\u0442 \u0441\u0442\u0430\u0432\u043A\u0438"),
                        Msg.color("&7\u0412\u044B\u0438\u0433\u0440\u044B\u0448: x3")
                )));
        return inv;
    }

    private Inventory buildWheelMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, WHEEL_TITLE);
        List<Integer> bets = getBets();
        for (int i = 0; i < BET_SLOTS.length; i++) {
            int bet = i < bets.size() ? bets.get(i) : (i + 1);
            inv.setItem(BET_SLOTS[i], betItem(bet));
        }
        inv.setItem(4, item(Material.NETHER_STAR, "&e\u041A\u043E\u043B\u0435\u0441\u043E \u0444\u043E\u0440\u0442\u0443\u043D\u044B",
                List.of(
                        Msg.color("&7\u041D\u0430\u0436\u043C\u0438 \u043D\u0430 \u0441\u0442\u0430\u0432\u043A\u0443 \u043D\u0438\u0436\u0435")
                )));
        renderSpinRow(inv, defaultRewards());
        inv.setItem(BACK_SLOT, item(Material.BARRIER, "&c\u041D\u0430\u0437\u0430\u0434", null));
        return inv;
    }

    private Inventory buildCoinMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, COIN_TITLE);
        inv.setItem(COIN_BET_SLOTS[0], betItem(2, "&b\u0421\u0442\u0430\u0432\u043A\u0430: &f2"));
        inv.setItem(COIN_BET_SLOTS[1], betItem(6, "&b\u0421\u0442\u0430\u0432\u043A\u0430: &f6"));
        inv.setItem(COIN_BET_SLOTS[2], betItem(12, "&b\u0421\u0442\u0430\u0432\u043A\u0430: &f12"));
        inv.setItem(4, item(Material.SUNFLOWER, "&6\u041E\u0440\u0435\u043B/\u0420\u0435\u0448\u043A\u0430",
                List.of(
                        Msg.color("&7\u0428\u0430\u043D\u0441: 50/50"),
                        Msg.color("&7\u0412\u044B\u0438\u0433\u0440\u044B\u0448: x2"),
                        Msg.color("&7\u041D\u0430\u0436\u043C\u0438 \u043D\u0430 \u0441\u0442\u0430\u0432\u043A\u0443 \u043D\u0438\u0436\u0435")
                )));
        inv.setItem(BACK_SLOT, item(Material.BARRIER, "&c\u041D\u0430\u0437\u0430\u0434", null));
        return inv;
    }

    private Inventory buildBoxMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, BOX_TITLE);
        inv.setItem(BOX_BET_SLOTS[0], betItem(4, "&b\u0421\u0442\u0430\u0432\u043A\u0430: &f4"));
        inv.setItem(BOX_BET_SLOTS[1], betItem(10, "&b\u0421\u0442\u0430\u0432\u043A\u0430: &f10"));
        inv.setItem(BOX_BET_SLOTS[2], betItem(20, "&b\u0421\u0442\u0430\u0432\u043A\u0430: &f20"));
        inv.setItem(4, item(Material.CHEST, "&6\u0422\u0430\u0439\u043D\u044B\u0439 \u044F\u0449\u0438\u043A",
                List.of(
                        Msg.color("&7\u0428\u0430\u043D\u0441 \u0432\u044B\u0438\u0433\u0440\u044B\u0448\u0430 \u043E\u0442 \u0441\u0442\u0430\u0432\u043A\u0438"),
                        Msg.color("&7\u0412\u044B\u0438\u0433\u0440\u044B\u0448: x3"),
                        Msg.color("&7\u041D\u0430\u0436\u043C\u0438 \u043D\u0430 \u0441\u0442\u0430\u0432\u043A\u0443 \u043D\u0438\u0436\u0435")
                )));
        inv.setItem(BACK_SLOT, item(Material.BARRIER, "&c\u041D\u0430\u0437\u0430\u0434", null));
        return inv;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = e.getView().getTitle();
        if (!TITLE.equals(title) && !WHEEL_TITLE.equals(title) && !COIN_TITLE.equals(title) && !BOX_TITLE.equals(title)) {
            return;
        }
        e.setCancelled(true);
        Player player = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (TITLE.equals(title)) {
            handleMainClick(player, slot);
            return;
        }
        if (slot == BACK_SLOT) {
            player.openInventory(buildMainMenu());
            return;
        }
        if (WHEEL_TITLE.equals(title)) {
            handleWheelClick(player, slot);
            return;
        }
        if (COIN_TITLE.equals(title)) {
            handleCoinClick(player, slot);
            return;
        }
        if (BOX_TITLE.equals(title)) {
            handleBoxClick(player, slot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        if (slot == 11) {
            player.openInventory(buildWheelMenu());
            return;
        }
        if (slot == 13) {
            player.openInventory(buildCoinMenu());
            return;
        }
        if (slot == 15) {
            player.openInventory(buildBoxMenu());
        }
    }

    private void handleWheelClick(Player player, int slot) {
        int bet = betFromWheelSlot(slot);
        if (bet <= 0) {
            return;
        }
        if (Boolean.TRUE.equals(spinning.get(player.getUniqueId()))) {
            Msg.send(player, "&c\u041F\u043E\u0434\u043E\u0436\u0434\u0438\u0442\u0435, \u0438\u0434\u0451\u0442 \u043F\u0440\u043E\u043A\u0440\u0443\u0442\u043A\u0430.");
            return;
        }
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < 1500) {
            Msg.send(player, "&c\u041F\u043E\u0434\u043E\u0436\u0434\u0438\u0442\u0435 \u043D\u0435\u043C\u043D\u043E\u0433\u043E.");
            return;
        }
        if (!takeDiamonds(player, bet)) {
            Msg.send(player, "&c\u041D\u0435\u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u0430\u043B\u043C\u0430\u0437\u043E\u0432.");
            return;
        }
        cooldowns.put(player.getUniqueId(), now);
        spinning.put(player.getUniqueId(), true);
        Msg.send(player, "&e\u041A\u043E\u043B\u0435\u0441\u043E \u043A\u0440\u0443\u0442\u0438\u0442\u0441\u044F...");
        startSpin(player, bet);
    }

    private void handleCoinClick(Player player, int slot) {
        int bet = betFromCoinSlot(slot);
        if (bet <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < 1500) {
            Msg.send(player, "&c\u041F\u043E\u0434\u043E\u0436\u0434\u0438\u0442\u0435 \u043D\u0435\u043C\u043D\u043E\u0433\u043E.");
            return;
        }
        if (!takeDiamonds(player, bet)) {
            Msg.send(player, "&c\u041D\u0435\u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u0430\u043B\u043C\u0430\u0437\u043E\u0432.");
            return;
        }
        cooldowns.put(player.getUniqueId(), now);
        runCoinFlip(player, bet);
    }

    private void handleBoxClick(Player player, int slot) {
        int bet = betFromBoxSlot(slot);
        if (bet <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < 1500) {
            Msg.send(player, "&c\u041F\u043E\u0434\u043E\u0436\u0434\u0438\u0442\u0435 \u043D\u0435\u043C\u043D\u043E\u0433\u043E.");
            return;
        }
        if (!takeDiamonds(player, bet)) {
            Msg.send(player, "&c\u041D\u0435\u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u0430\u043B\u043C\u0430\u0437\u043E\u0432.");
            return;
        }
        cooldowns.put(player.getUniqueId(), now);
        runMysteryBox(player, bet);
    }

    private int betFromWheelSlot(int slot) {
        for (int i = 0; i < BET_SLOTS.length; i++) {
            if (BET_SLOTS[i] == slot) {
                List<Integer> bets = getBets();
                return i < bets.size() ? bets.get(i) : (i + 1);
            }
        }
        return 0;
    }

    private int betFromCoinSlot(int slot) {
        for (int i = 0; i < COIN_BET_SLOTS.length; i++) {
            if (COIN_BET_SLOTS[i] == slot) {
                return switch (i) {
                    case 0 -> 2;
                    case 1 -> 6;
                    default -> 12;
                };
            }
        }
        return 0;
    }

    private int betFromBoxSlot(int slot) {
        for (int i = 0; i < BOX_BET_SLOTS.length; i++) {
            if (BOX_BET_SLOTS[i] == slot) {
                return switch (i) {
                    case 0 -> 4;
                    case 1 -> 10;
                    default -> 20;
                };
            }
        }
        return 0;
    }

    private List<Integer> getBets() {
        List<Integer> bets = plugin.getCasinoConfig().getIntegerList("casino.wheel.bets");
        if (bets == null || bets.isEmpty()) {
            return List.of(1, 4, 8, 16, 32);
        }
        return bets;
    }

    private Reward rollReward(int bet) {
        List<Reward> rewards = loadRewards();
        if (rewards.isEmpty()) {
            return null;
        }
        int winBase = plugin.getCasinoConfig().getInt("casino.wheel.win_base", 25);
        int winPerBet = plugin.getCasinoConfig().getInt("casino.wheel.win_per_bet", 3);
        int loseBase = plugin.getCasinoConfig().getInt("casino.wheel.lose_base", 50);
        int winChance = Math.min(95, Math.max(5, winBase + winPerBet * bet));
        int roll = ThreadLocalRandom.current().nextInt(1, winChance + loseBase + 1);
        if (roll > winChance) {
            return new Reward(RewardType.NOTHING, 0, null, null);
        }
        List<Reward> winRewards = new ArrayList<>();
        for (Reward r : rewards) {
            if (r.type != RewardType.NOTHING) {
                winRewards.add(r);
            }
        }
        if (winRewards.isEmpty()) {
            return new Reward(RewardType.NOTHING, 0, null, null);
        }
        int total = 0;
        for (Reward r : winRewards) {
            total += Math.max(0, r.weight);
        }
        if (total <= 0) {
            return winRewards.get(0);
        }
        int pick = RandUtil.nextInt(1, total);
        int current = 0;
        for (Reward r : winRewards) {
            current += Math.max(0, r.weight);
            if (pick <= current) {
                return r;
            }
        }
        return winRewards.get(winRewards.size() - 1);
    }

    private List<Reward> loadRewards() {
        ConfigurationSection sec = plugin.getCasinoConfig().getConfigurationSection("casino.wheel.rewards");
        if (sec == null) {
            return defaultRewards();
        }
        List<Reward> list = new ArrayList<>();
        for (String key : sec.getKeys(false)) {
            ConfigurationSection r = sec.getConfigurationSection(key);
            if (r == null) {
                continue;
            }
            Reward reward = Reward.fromConfig(r, quarryManager);
            if (reward != null) {
                list.add(reward);
            }
        }
        return list.isEmpty() ? defaultRewards() : list;
    }

    private List<Reward> defaultRewards() {
        List<Reward> list = new ArrayList<>();
        list.add(new Reward(RewardType.NOTHING, 50, null, null));
        list.add(Reward.material(20, Material.DIAMOND, 2));
        list.add(Reward.material(8, Material.DIAMOND_BLOCK, 1));
        list.add(Reward.material(12, Material.IRON_INGOT, 16));
        list.add(Reward.material(10, Material.GOLD_INGOT, 8));
        list.add(Reward.material(6, Material.IRON_CHESTPLATE, 1));
        list.add(Reward.material(4, Material.DIAMOND_CHESTPLATE, 1));
        list.add(Reward.book(5, "DRILL"));
        list.add(Reward.book(5, "MAGNET"));
        list.add(Reward.book(3, "AUTOSMELT"));
        list.add(Reward.book(2, "SECOND_LIFE"));
        list.add(Reward.book(2, "ASSASSIN"));
        list.add(Reward.book(2, "HORN"));
        list.add(Reward.book(2, "SHELL"));
        list.add(Reward.book(2, "BOOM"));
        list.add(Reward.quarry(3, 1));
        list.add(Reward.quarry(2, 2));
        list.add(Reward.material(2, Material.NETHERITE_INGOT, 1));
        return list;
    }

    private void startSpin(Player player, int bet) {
        List<Reward> pool = loadRewards();
        if (pool.isEmpty()) {
            pool = defaultRewards();
        }
        List<Reward> displayPool = new ArrayList<>(pool);
        int ticks = plugin.getCasinoConfig().getInt("casino.wheel.spin_ticks", 60);
        int interval = plugin.getCasinoConfig().getInt("casino.wheel.spin_interval_ticks", 2);
        int totalSteps = Math.max(10, ticks / Math.max(1, interval));
        Inventory inv = player.getOpenInventory().getTopInventory();
        for (int step = 0; step < totalSteps; step++) {
            final int s = step;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                shiftSpinRow(inv, displayPool);
                player.updateInventory();
            }, (long) s * interval);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Reward reward = rollReward(bet);
            spinning.put(player.getUniqueId(), false);
            if (reward == null || reward.type == RewardType.NOTHING) {
                Msg.send(player, "&c\u041D\u0435 \u043F\u043E\u0432\u0435\u0437\u043B\u043E.");
                return;
            }
            ItemStack prize = reward.createItem();
            if (prize == null) {
                Msg.send(player, "&c\u041D\u0435 \u043F\u043E\u0432\u0435\u0437\u043B\u043E.");
                return;
            }
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(prize);
            if (!leftover.isEmpty()) {
                for (ItemStack it : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), it);
                }
            }
            Msg.send(player, "&a\u0412\u044B \u0432\u044B\u0438\u0433\u0440\u0430\u043B\u0438: &f" + reward.displayName());
        }, (long) totalSteps * interval + 2L);
    }

    private void runCoinFlip(Player player, int bet) {
        Msg.send(player, "&e\u0411\u0440\u043E\u0441\u043E\u043A \u043C\u043E\u043D\u0435\u0442\u044B...");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            boolean win = ThreadLocalRandom.current().nextBoolean();
            if (!win) {
                Msg.send(player, "&c\u041D\u0435 \u043F\u043E\u0432\u0435\u0437\u043B\u043E.");
                return;
            }
            int reward = bet * 2;
            player.getInventory().addItem(new ItemStack(Material.DIAMOND, reward));
            Msg.send(player, "&a\u0412\u044B \u0432\u044B\u0438\u0433\u0440\u0430\u043B\u0438: &f" + reward + " \u0430\u043B\u043C\u0430\u0437\u043E\u0432");
        }, 30L);
    }

    private void runMysteryBox(Player player, int bet) {
        int winBase = plugin.getCasinoConfig().getInt("casino.mystery.win_base", 20);
        int winPerBet = plugin.getCasinoConfig().getInt("casino.mystery.win_per_bet", 2);
        int winChance = Math.min(95, Math.max(5, winBase + winPerBet * bet));
        int roll = ThreadLocalRandom.current().nextInt(1, 101);
        if (roll > winChance) {
            Msg.send(player, "&c\u041D\u0435 \u043F\u043E\u0432\u0435\u0437\u043B\u043E.");
            return;
        }
        Reward reward = rollReward(Math.max(1, bet / 2));
        if (reward == null || reward.type == RewardType.NOTHING) {
            Msg.send(player, "&c\u041D\u0435 \u043F\u043E\u0432\u0435\u0437\u043B\u043E.");
            return;
        }
        ItemStack prize = reward.createItem();
        if (prize == null) {
            Msg.send(player, "&c\u041D\u0435 \u043F\u043E\u0432\u0435\u0437\u043B\u043E.");
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(prize);
        if (!leftover.isEmpty()) {
            for (ItemStack it : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), it);
            }
        }
        Msg.send(player, "&a\u042F\u0449\u0438\u043A \u0432\u044B\u0434\u0430\u043B: &f" + reward.displayName());
    }

    private void renderSpinRow(Inventory inv, List<Reward> rewards) {
        for (int i = 0; i < SPIN_SLOTS.length; i++) {
            Reward r = rewards.get(i % rewards.size());
            inv.setItem(SPIN_SLOTS[i], r.icon());
        }
    }

    private void shiftSpinRow(Inventory inv, List<Reward> rewards) {
        if (rewards.isEmpty()) {
            return;
        }
        Reward r = rewards.remove(0);
        rewards.add(r);
        for (int i = 0; i < SPIN_SLOTS.length; i++) {
            Reward item = rewards.get(i % rewards.size());
            inv.setItem(SPIN_SLOTS[i], item.icon());
        }
    }

    private boolean takeDiamonds(Player player, int amount) {
        PlayerInventory inv = player.getInventory();
        if (!inv.containsAtLeast(new ItemStack(Material.DIAMOND), amount)) {
            return false;
        }
        int remaining = amount;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != Material.DIAMOND) {
                continue;
            }
            int take = Math.min(remaining, it.getAmount());
            it.setAmount(it.getAmount() - take);
            remaining -= take;
            if (it.getAmount() <= 0) {
                inv.setItem(i, null);
            }
            if (remaining <= 0) {
                break;
            }
        }
        return true;
    }

    private ItemStack betItem(int bet) {
        return betItem(bet, "&b\u0421\u0442\u0430\u0432\u043A\u0430: &f" + bet);
    }

    private ItemStack betItem(int bet, String name) {
        ItemStack it = new ItemStack(Material.DIAMOND, bet);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.color(name));
            meta.setLore(List.of(Msg.color("&7\u041D\u0430\u0436\u043C\u0438, \u0447\u0442\u043E\u0431\u044B \u0441\u044B\u0433\u0440\u0430\u0442\u044C")));
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.color(name));
            if (lore != null) {
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static class Reward {
        private final RewardType type;
        private final int weight;
        private final ItemStack item;
        private final QuarryManager quarryManager;
        private final int quarryLevel;
        private final String bookKey;
        private final int amount;

        private Reward(RewardType type, int weight, ItemStack item, QuarryManager quarryManager, int quarryLevel, String bookKey, int amount) {
            this.type = type;
            this.weight = weight;
            this.item = item;
            this.quarryManager = quarryManager;
            this.quarryLevel = quarryLevel;
            this.bookKey = bookKey;
            this.amount = amount;
        }

        private Reward(RewardType type, int weight, ItemStack item, QuarryManager quarryManager) {
            this(type, weight, item, quarryManager, 0, null, 1);
        }

        public static Reward material(int weight, Material mat, int amount) {
            return new Reward(RewardType.MATERIAL, weight, new ItemStack(mat, Math.max(1, amount)), null, 0, null, amount);
        }

        public static Reward book(int weight, String key) {
            return new Reward(RewardType.BOOK, weight, null, null, 0, key, 1);
        }

        public static Reward quarry(int weight, int level) {
            return new Reward(RewardType.QUARRY, weight, null, null, level, null, 1);
        }

        public static Reward fromConfig(ConfigurationSection sec, QuarryManager quarryManager) {
            String typeStr = sec.getString("type", "NOTHING").trim().toUpperCase();
            int weight = sec.getInt("weight", 0);
            RewardType type;
            try {
                type = RewardType.valueOf(typeStr);
            } catch (Exception e) {
                return null;
            }
            switch (type) {
                case NOTHING:
                    return new Reward(type, weight, null, null);
                case MATERIAL: {
                    String matStr = sec.getString("material", "");
                    int amount = sec.getInt("amount", 1);
                    Material mat = Material.matchMaterial(matStr);
                    if (mat == null) {
                        return null;
                    }
                    return new Reward(type, weight, new ItemStack(mat, Math.max(1, amount)), null, 0, null, amount);
                }
                case BOOK: {
                    String key = sec.getString("enchant", "");
                    return new Reward(type, weight, null, null, 0, key, 1);
                }
                case QUARRY: {
                    int level = sec.getInt("level", 1);
                    return new Reward(type, weight, null, quarryManager, level, null, 1);
                }
                default:
                    return null;
            }
        }

        public ItemStack createItem() {
            return switch (type) {
                case NOTHING -> null;
                case MATERIAL -> item.clone();
                case BOOK -> createBook(bookKey);
                case QUARRY -> quarryManager != null ? quarryManager.createQuarryItem(quarryLevel) : null;
            };
        }

        public String displayName() {
            return switch (type) {
                case NOTHING -> "\u043D\u0438\u0447\u0435\u0433\u043E";
                case MATERIAL -> prettyMaterial(item.getType()) + " x" + item.getAmount();
                case BOOK -> "\u041A\u043D\u0438\u0433\u0430: " + bookKey;
                case QUARRY -> "\u041A\u0430\u0440\u044C\u0435\u0440 " + quarryLevel;
            };
        }

        public ItemStack icon() {
            return switch (type) {
                case NOTHING -> new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                case MATERIAL -> item.clone();
                case BOOK -> new ItemStack(Material.ENCHANTED_BOOK);
                case QUARRY -> new ItemStack(Material.DISPENSER);
            };
        }

        private ItemStack createBook(String key) {
            if (key == null) {
                return null;
            }
            String k = key.trim().toUpperCase();
            return switch (k) {
                case "DRILL" -> CustomEnchantBooks.createBook(DrillEnchant.get(), "&6\u0411\u0443\u0440");
                case "MAGNET" -> CustomEnchantBooks.createBook(MagnetEnchant.get(), "&6\u041C\u0430\u0433\u043D\u0438\u0442");
                case "AUTOSMELT" -> CustomEnchantBooks.createBook(AutoSmeltEnchant.get(), "&6\u0410\u0432\u0442\u043E\u043F\u043B\u0430\u0432\u043A\u0430");
                case "FARMER" -> CustomEnchantBooks.createBook(FarmerEnchant.get(), "&6\u0424\u0435\u0440\u043C\u0435\u0440");
                case "LUMBERJACK" -> CustomEnchantBooks.createBook(LumberjackEnchant.get(), "&6\u041B\u0435\u0441\u043E\u0440\u0443\u0431");
                case "SECOND_LIFE" -> CustomEnchantBooks.createBook(SecondLifeEnchant.get(), "&6\u0412\u0442\u043E\u0440\u0430\u044F \u0436\u0438\u0437\u043D\u044C");
                case "ASSASSIN" -> CustomEnchantBooks.createBook(AssassinEnchant.get(), "&6\u0410\u0441\u0441\u0430\u0441\u0438\u043D");
                case "HORN" -> CustomEnchantBooks.createBook(HornEnchant.get(), "&6\u0420\u043E\u0433");
                case "SHELL" -> CustomEnchantBooks.createBook(ShellEnchant.get(), "&6\u041F\u0430\u043D\u0446\u0438\u0440\u044C");
                case "BOOM" -> CustomEnchantBooks.createBook(BoomLeggingsEnchant.get(), "&6\u041F\u043E\u0434\u0440\u044B\u0432");
                case "UNBREAKABLE" -> CustomEnchantBooks.createBook(me.dominus.faivents.enchant.UnbreakableEnchant.get(), "&6\u041D\u0435\u0440\u0430\u0437\u0440\u0443\u0448\u0438\u043C\u043E\u0441\u0442\u044C");
                case "PUMPKIN" -> CustomEnchantBooks.createBook(me.dominus.faivents.enchant.PumpkinEnchant.get(), "&6\u0422\u044B\u043A\u0432\u0430");
                default -> null;
            };
        }

        private String prettyMaterial(Material mat) {
            if (mat == null) {
                return "\u041F\u0440\u0435\u0434\u043C\u0435\u0442";
            }
            return mat.name().toLowerCase().replace('_', ' ');
        }
    }

    private enum RewardType {
        NOTHING,
        MATERIAL,
        BOOK,
        QUARRY
    }
}
