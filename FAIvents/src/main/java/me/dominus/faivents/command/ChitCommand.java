package me.dominus.faivents.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.dominus.faivents.util.Msg;

public class ChitCommand implements CommandExecutor, Listener {

    private static final String TITLE = "\u041F\u0440\u043E\u0432\u0435\u0440\u043A\u0430: \u0447\u0438\u0442\u044B";
    private static final String TITLE_SELECT = "\u0412\u044B\u0431\u0435\u0440\u0438 \u0438\u0433\u0440\u043E\u043A\u0430";
    private static final int GUI_SIZE = 27;
    private static final int SLOT_DIAMOND = 11;
    private static final int SLOT_ANCIENT = 13;
    private static final int SLOT_FREEZE = 15;
    private static final int SLOT_UNFREEZE = 22;

    private final JavaPlugin plugin;
    private final Map<UUID, UUID> openTarget = new HashMap<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Map<UUID, Float> prevWalk = new HashMap<>();
    private final Map<UUID, Float> prevFly = new HashMap<>();
    private final Map<UUID, Map<Location, BlockData>> fakeBlocks = new HashMap<>();
    private final Random random = new Random();

    public ChitCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Msg.send(sender, "&c\u041A\u043E\u043C\u0430\u043D\u0434\u0430 \u0442\u043E\u043B\u044C\u043A\u043E \u0434\u043B\u044F \u0438\u0433\u0440\u043E\u043A\u043E\u0432.");
            return true;
        }
        Player admin = (Player) sender;
        if (args.length < 1) {
            admin.openInventory(buildSelectGui());
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Msg.send(admin, "&c\u0418\u0433\u0440\u043E\u043A \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D.");
            return true;
        }
        admin.openInventory(buildGui(target));
        openTarget.put(admin.getUniqueId(), target.getUniqueId());
        return true;
    }

    private Inventory buildGui(Player target) {
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, TITLE + " - " + target.getName());
        inv.setItem(SLOT_DIAMOND, button(Material.DIAMOND_ORE, "&b\u041F\u043E\u043A\u0430\u0437\u0430\u0442\u044C \u0430\u043B\u043C\u0430\u0437\u044B"));
        inv.setItem(SLOT_ANCIENT, button(Material.ANCIENT_DEBRIS, "&6\u041F\u043E\u043A\u0430\u0437\u0430\u0442\u044C \u0434\u0440\u0435\u0432\u043D\u0438\u0439 \u043E\u0431\u043B\u043E\u043C\u043E\u043A"));
        inv.setItem(SLOT_FREEZE, button(Material.REDSTONE_BLOCK, "&c\u0412\u044B\u0437\u0432\u0430\u0442\u044C \u043D\u0430 \u043F\u0440\u043E\u0432\u0435\u0440\u043A\u0443"));
        inv.setItem(SLOT_UNFREEZE, button(Material.LIME_DYE, "&a\u0421\u043D\u044F\u0442\u044C \u043F\u0440\u043E\u0432\u0435\u0440\u043A\u0443"));
        return inv;
    }

    private Inventory buildSelectGui() {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SELECT);
        int slot = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (slot >= 54) {
                break;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            ItemMeta meta = head.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(Msg.color("&b" + p.getName()));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }
        return inv;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) {
            return;
        }
        String title = event.getView().getTitle();
        if (title.startsWith(TITLE_SELECT)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) {
                return;
            }
            String name = null;
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                name = ChatColor.stripColor(meta.getDisplayName());
            }
            if (name == null || name.isEmpty()) {
                return;
            }
            Player target = Bukkit.getPlayerExact(name);
            if (target == null) {
                Msg.send(admin, "&c\u0418\u0433\u0440\u043E\u043A \u043D\u0435 \u043E\u043D\u043B\u0430\u0439\u043D.");
                return;
            }
            admin.openInventory(buildGui(target));
            openTarget.put(admin.getUniqueId(), target.getUniqueId());
            return;
        }
        if (!title.startsWith(TITLE)) {
            return;
        }
        event.setCancelled(true);
        UUID targetId = openTarget.get(admin.getUniqueId());
        if (targetId == null) {
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            Msg.send(admin, "&c\u0418\u0433\u0440\u043E\u043A \u043D\u0435 \u043E\u043D\u043B\u0430\u0439\u043D.");
            return;
        }
        int slot = event.getRawSlot();
        if (slot == SLOT_DIAMOND) {
            showFakeOre(target, Material.DIAMOND_ORE);
            Msg.send(admin, "&a\u041F\u043E\u043A\u0430\u0437\u0430\u043B \u0430\u043B\u043C\u0430\u0437\u044B \u0438\u0433\u0440\u043E\u043A\u0443.");
        } else if (slot == SLOT_ANCIENT) {
            showFakeOre(target, Material.ANCIENT_DEBRIS);
            Msg.send(admin, "&a\u041F\u043E\u043A\u0430\u0437\u0430\u043B \u0434\u0440\u0435\u0432\u043D\u0438\u0435 \u043E\u0431\u043B\u043E\u043C\u043A\u0438 \u0438\u0433\u0440\u043E\u043A\u0443.");
        } else if (slot == SLOT_FREEZE) {
            freeze(target);
            Msg.send(admin, "&e\u0418\u0433\u0440\u043E\u043A \u0437\u0430\u043C\u043E\u0440\u043E\u0436\u0435\u043D.");
        } else if (slot == SLOT_UNFREEZE) {
            unfreeze(target);
            Msg.send(admin, "&a\u041F\u0440\u043E\u0432\u0435\u0440\u043A\u0430 \u0441\u043D\u044F\u0442\u0430.");
        }
    }

    private void freeze(Player player) {
        if (frozen.contains(player.getUniqueId())) {
            return;
        }
        frozen.add(player.getUniqueId());
        prevWalk.put(player.getUniqueId(), player.getWalkSpeed());
        prevFly.put(player.getUniqueId(), player.getFlySpeed());
        player.setWalkSpeed(0f);
        player.setFlySpeed(0f);
        player.sendTitle("\u00A7c\u041F\u0420\u041E\u0412\u0415\u0420\u041A\u0410 \u041D\u0410 \u0427\u0418\u0422\u042B!", "\u00A7f\u041D\u0435 \u0434\u0432\u0438\u0433\u0430\u0439\u0441\u044F \u0438 \u043D\u0435 \u043F\u0438\u0448\u0438 \u043A\u043E\u043C\u0430\u043D\u0434\u044B", 10, 80, 10);
    }

    private void unfreeze(Player player) {
        frozen.remove(player.getUniqueId());
        Float ws = prevWalk.remove(player.getUniqueId());
        Float fs = prevFly.remove(player.getUniqueId());
        if (ws != null) {
            player.setWalkSpeed(ws);
        } else {
            player.setWalkSpeed(0.2f);
        }
        if (fs != null) {
            player.setFlySpeed(fs);
        } else {
            player.setFlySpeed(0.1f);
        }
    }

    private void showFakeOre(Player player, Material ore) {
        Location origin = player.getLocation();
        List<Block> candidates = new ArrayList<>();
        int radius = 6;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block b = origin.getWorld().getBlockAt(origin.getBlockX() + dx, origin.getBlockY() + dy, origin.getBlockZ() + dz);
                    if (!b.getType().isSolid()) {
                        continue;
                    }
                    if (b.getType() == Material.BEDROCK || b.getType() == Material.BARRIER) {
                        continue;
                    }
                    candidates.add(b);
                }
            }
        }
        Collections.shuffle(candidates, random);
        int count = Math.min(10, candidates.size());
        Map<Location, BlockData> changed = fakeBlocks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        for (int i = 0; i < count; i++) {
            Block b = candidates.get(i);
            Location loc = b.getLocation();
            if (!changed.containsKey(loc)) {
                changed.put(loc, b.getBlockData());
            }
            player.sendBlockChange(loc, ore.createBlockData());
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                restoreFake(player);
            }
        }.runTaskLater(plugin, 20L * 15);
    }

    private void restoreFake(Player player) {
        Map<Location, BlockData> map = fakeBlocks.remove(player.getUniqueId());
        if (map == null) {
            return;
        }
        for (Map.Entry<Location, BlockData> e : map.entrySet()) {
            player.sendBlockChange(e.getKey(), e.getValue());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!frozen.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p && frozen.contains(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInv(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p && frozen.contains(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (frozen.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && frozen.contains(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        frozen.remove(event.getPlayer().getUniqueId());
        prevWalk.remove(event.getPlayer().getUniqueId());
        prevFly.remove(event.getPlayer().getUniqueId());
        fakeBlocks.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack button(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.color(name));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            it.setItemMeta(meta);
        }
        return it;
    }
}
