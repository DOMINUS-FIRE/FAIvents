package me.dominus.faivents.quarry;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.dominus.faivents.quarry.QuarryManager.FilterMode;
import me.dominus.faivents.quarry.QuarryManager.QuarryData;
import me.dominus.faivents.util.Msg;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class QuarryListener implements Listener {

    private static final String GUI_TITLE = "\u041A\u0430\u0440\u044C\u0435\u0440";
    private static final String MAIN_TITLE = "\u041A\u0430\u0440\u044C\u0435\u0440: \u041C\u0435\u043D\u044E";
    private static final String FUEL_TITLE = "\u041A\u0430\u0440\u044C\u0435\u0440: \u0422\u043E\u043F\u043B\u0438\u0432\u043E";
    private static final String SETTINGS_TITLE = "\u041A\u0430\u0440\u044C\u0435\u0440: \u041D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438";
    private static final String STORAGE_TITLE = "\u041A\u0430\u0440\u044C\u0435\u0440: \u0425\u0440\u0430\u043D\u0438\u043B\u0438\u0449\u0435";
    private static final String FILTER_TITLE = "\u0424\u0438\u043B\u044C\u0442\u0440";
    private static final int MAIN_SIZE = 27;
    private static final int STORAGE_GUI_SIZE = 54;
    private static final int STORAGE_START = 0;
    private static final int STORAGE_END = 44;
    private static final int STORAGE_PREV = 45;
    private static final int STORAGE_INFO = 47;
    private static final int STORAGE_BACK = 49;
    private static final int STORAGE_NEXT = 53;
    private static final int FUEL_INPUT_SLOT = 13;
    private static final int FUEL_SOLAR_SLOT = 11;
    private static final int FUEL_BACK_SLOT = 18;
    private static final int MAIN_FUEL_SLOT = 11;
    private static final int MAIN_TOGGLE_SLOT = 13;
    private static final int MAIN_SETTINGS_SLOT = 15;
    private static final int MAIN_STORAGE_SLOT = 22;
    private static final int SETTINGS_SILK_SLOT = 11;
    private static final int SETTINGS_FILTER_SLOT = 13;
    private static final int SETTINGS_CHUNKS_SLOT = 15;
    private static final int SETTINGS_BACK_SLOT = 18;

    private final QuarryManager quarryManager;
    private final JavaPlugin plugin;
    private final Map<UUID, Location> openMain = new HashMap<>();
    private final Map<UUID, Location> openStorage = new HashMap<>();
    private final Map<UUID, Location> openFuel = new HashMap<>();
    private final Map<UUID, Location> openSettings = new HashMap<>();
    private final Map<UUID, Location> openFilter = new HashMap<>();

    public QuarryListener(JavaPlugin plugin, QuarryManager quarryManager) {
        this.plugin = plugin;
        this.quarryManager = quarryManager;
    }

    public Location findQuarryByOwner(UUID owner, int level) {
        for (WorldChunk wc : WorldChunk.getLoaded()) {
            for (BlockState st : wc.states) {
                Location loc = st.getLocation();
                Block block = loc.getBlock();
                if (!quarryManager.isQuarryBlock(block)) {
                    continue;
                }
                UUID o = quarryManager.getOwner(block);
                if (o != null && o.equals(owner) && quarryManager.getLevel(block) == level) {
                    return loc;
                }
            }
        }
        return null;
    }

    public void openRemoteStorage(Player player, Block block) {
        if (!canAccess(player, block)) {
            Msg.send(player, "&c\u042D\u0442\u043E \u043D\u0435 \u0442\u0432\u043E\u0439 \u043A\u0430\u0440\u044C\u0435\u0440.");
            return;
        }
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        openStorageGui(player, block, qd.page);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.DISPENSER) {
            return;
        }
        ItemStack inHand = event.getItemInHand();
        if (!quarryManager.isQuarryItem(inHand)) {
            return;
        }
        Player player = event.getPlayer();
        int level = quarryManager.getItemLevel(inHand);
        if (hasLevel(player.getUniqueId(), level)) {
            Msg.send(player, "&c\u0423 \u0442\u0435\u0431\u044F \u0443\u0436\u0435 \u0435\u0441\u0442\u044C \u043A\u0430\u0440\u044C\u0435\u0440 \u044D\u0442\u043E\u0433\u043E \u0443\u0440\u043E\u0432\u043D\u044F.");
            event.setCancelled(true);
            return;
        }
        quarryManager.registerQuarry(block, player, level);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!quarryManager.isQuarryBlock(block)) {
            return;
        }
        quarryManager.clearQuarry(block);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !quarryManager.isQuarryBlock(block)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!canAccess(player, block)) {
            Msg.send(player, "&c\u042D\u0442\u043E \u043D\u0435 \u0442\u0432\u043E\u0439 \u043A\u0430\u0440\u044C\u0435\u0440.");
            return;
        }
        openMainGui(player, block);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.startsWith(MAIN_TITLE) && !title.startsWith(FUEL_TITLE)
                && !title.startsWith(SETTINGS_TITLE) && !title.startsWith(STORAGE_TITLE)
                && !title.startsWith(FILTER_TITLE)) {
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        int raw = event.getRawSlot();
        if (raw >= topSize) {
            if (!event.isShiftClick()) {
                event.setCancelled(false);
            } else {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        if (title.startsWith(FILTER_TITLE)) {
            handleFilterClick(player, raw, event);
            return;
        }
        if (title.startsWith(MAIN_TITLE)) {
            handleMainClick(player, raw);
            return;
        }
        if (title.startsWith(FUEL_TITLE)) {
            handleFuelGuiClick(player, raw, event);
            return;
        }
        if (title.startsWith(SETTINGS_TITLE)) {
            handleSettingsClick(player, raw);
            return;
        }
        if (title.startsWith(STORAGE_TITLE)) {
            handleStorageGuiClick(player, raw, event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (title.startsWith(MAIN_TITLE)) {
            scheduleMapCleanup(player, openMain, MAIN_TITLE);
        }
        if (title.startsWith(STORAGE_TITLE)) {
            saveStorage(player, event.getInventory());
            scheduleMapCleanup(player, openStorage, STORAGE_TITLE);
        }
        if (title.startsWith(FUEL_TITLE)) {
            handleFuelClose(player, event.getInventory());
            scheduleMapCleanup(player, openFuel, FUEL_TITLE);
        }
        if (title.startsWith(FILTER_TITLE)) {
            saveFilter(player, event.getInventory());
            scheduleMapCleanup(player, openFilter, FILTER_TITLE);
        }
        if (title.startsWith(SETTINGS_TITLE)) {
            scheduleMapCleanup(player, openSettings, SETTINGS_TITLE);
        }
    }

    private void handleStorageClick(InventoryClickEvent event, Player player, Block block, QuarryData qd, int raw) {
        int index = (raw - STORAGE_START) + qd.page * storagePageSize();
        if (index < 0 || index >= qd.storage.size()) {
            return;
        }
        if (qd.level == 1) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            qd.storage.set(index, cursor.clone());
            event.getView().setItem(raw, cursor.clone());
            event.getWhoClicked().setItemOnCursor(null);
        } else if (current != null && current.getType() != Material.AIR) {
            qd.storage.set(index, null);
            event.getView().setItem(raw, null);
            event.getWhoClicked().setItemOnCursor(current.clone());
        }
    }

    private void handleFilterClick(Player player, int raw, InventoryClickEvent event) {
        Location loc = openFilter.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        Block block = loc.getBlock();
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        if (!canAccess(player, block)) {
            Msg.send(player, "&c\u042D\u0442\u043E \u043D\u0435 \u0442\u0432\u043E\u0439 \u043A\u0430\u0440\u044C\u0435\u0440.");
            player.closeInventory();
            return;
        }
        if (raw == 45) {
            FilterMode mode = quarryManager.getFilterMode(block);
            quarryManager.setFilterMode(block, mode == FilterMode.KEEP ? FilterMode.DELETE : FilterMode.KEEP);
            openFilterGui(player, block);
            return;
        }
        if (raw >= 0 && raw < 45) {
            event.setCancelled(false);
        }
    }

    private void openMainGui(Player player, Block block) {
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        Inventory inv = Bukkit.createInventory(null, MAIN_SIZE, MAIN_TITLE + " " + qd.level);
        inv.setItem(4, info(block, qd));
        inv.setItem(MAIN_FUEL_SLOT, button(Material.COAL, "&6\u0422\u043E\u043F\u043B\u0438\u0432\u043E"));
        inv.setItem(MAIN_TOGGLE_SLOT, toggleButton(block));
        inv.setItem(MAIN_SETTINGS_SLOT, button(Material.COMPARATOR, "&e\u041D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438"));
        inv.setItem(MAIN_STORAGE_SLOT, button(Material.CHEST, "&b\u0425\u0440\u0430\u043D\u0438\u043B\u0438\u0449\u0435"));
        openMain.put(player.getUniqueId(), block.getLocation());
        player.openInventory(inv);
    }

    private void handleFuelClick(InventoryClickEvent event, Player player, Block block, QuarryData qd) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (cursor != null && cursor.getType() != Material.AIR) {
            if (!isFuel(cursor, qd.level)) {
                return;
            }
            event.setCancelled(false);
            return;
        }
        if (current != null && current.getType() != Material.AIR) {
            event.setCancelled(false);
        }
    }

    private void openFuelGui(Player player, Block block) {
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        Inventory inv = Bukkit.createInventory(null, MAIN_SIZE, FUEL_TITLE);
        inv.setItem(4, info(block, qd));
        inv.setItem(FUEL_SOLAR_SLOT, solarButton(block, qd.level));
        inv.setItem(FUEL_BACK_SLOT, button(Material.BARRIER, "&c\u041D\u0430\u0437\u0430\u0434"));
        openFuel.put(player.getUniqueId(), block.getLocation());
        player.openInventory(inv);
    }

    private void openSettingsGui(Player player, Block block) {
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        Inventory inv = Bukkit.createInventory(null, MAIN_SIZE, SETTINGS_TITLE);
        inv.setItem(4, info(block, qd));
        if (qd.level >= 4) {
            inv.setItem(SETTINGS_SILK_SLOT, button(Material.ENCHANTED_BOOK,
                    "&6\u0428\u0451\u043B\u043A\u043E\u0432\u043E\u0435: " + (quarryManager.isSilk(block) ? "&a\u0412\u043A\u043B" : "&c\u0412\u044B\u043A\u043B")));
            inv.setItem(SETTINGS_FILTER_SLOT, button(Material.HOPPER, "&e\u0424\u0438\u043B\u044C\u0442\u0440"));
        } else {
            inv.setItem(SETTINGS_SILK_SLOT, button(Material.BARRIER, "&c\u041D\u0435\u0434\u043E\u0441\u0442\u0443\u043F\u043D\u043E"));
            inv.setItem(SETTINGS_FILTER_SLOT, button(Material.BARRIER, "&c\u041D\u0435\u0434\u043E\u0441\u0442\u0443\u043F\u043D\u043E"));
        }
        if (qd.level >= 5) {
            inv.setItem(SETTINGS_CHUNKS_SLOT, button(Material.MAP,
                    "&b\u0427\u0430\u043D\u043A\u0438 \u0432\u043E\u043A\u0440\u0443\u0433: " + (quarryManager.isMultiChunk(block) ? "&a\u0412\u043A\u043B" : "&c\u0412\u044B\u043A\u043B")));
        } else {
            inv.setItem(SETTINGS_CHUNKS_SLOT, button(Material.BARRIER, "&c\u041D\u0435\u0434\u043E\u0441\u0442\u0443\u043F\u043D\u043E"));
        }
        inv.setItem(SETTINGS_BACK_SLOT, button(Material.BARRIER, "&c\u041D\u0430\u0437\u0430\u0434"));
        openSettings.put(player.getUniqueId(), block.getLocation());
        player.openInventory(inv);
    }

    private void openStorageGui(Player player, Block block, int page) {
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        qd.page = Math.max(0, page);
        Inventory inv = Bukkit.createInventory(null, STORAGE_GUI_SIZE, STORAGE_TITLE + " " + qd.level);
        if (qd.level == 1) {
            for (int i = STORAGE_START; i <= STORAGE_END; i++) {
                inv.setItem(i, placeholder());
            }
        } else {
            int start = qd.page * storagePageSize();
            for (int i = 0; i < storagePageSize(); i++) {
                int idx = start + i;
                int slot = STORAGE_START + i;
                if (slot > STORAGE_END) {
                    break;
                }
                ItemStack it = idx < qd.storage.size() ? qd.storage.get(idx) : null;
                if (it != null) {
                    inv.setItem(slot, it);
                }
            }
        }
        inv.setItem(STORAGE_PREV, button(Material.ARROW, "&e\u041F\u0440\u0435\u0434\u044B\u0434\u0443\u0449\u0430\u044F"));
        inv.setItem(STORAGE_NEXT, button(Material.ARROW, "&e\u0421\u043B\u0435\u0434\u0443\u044E\u0449\u0430\u044F"));
        inv.setItem(STORAGE_BACK, button(Material.BARRIER, "&c\u041D\u0430\u0437\u0430\u0434"));
        inv.setItem(STORAGE_INFO, info(block, qd));
        openStorage.put(player.getUniqueId(), block.getLocation());
        player.openInventory(inv);
    }

    private void handleMainClick(Player player, int raw) {
        Location loc = openMain.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        Block block = loc.getBlock();
        if (!quarryManager.isQuarryBlock(block)) {
            return;
        }
        if (!canAccess(player, block)) {
            Msg.send(player, "&c\u042D\u0442\u043E \u043D\u0435 \u0442\u0432\u043E\u0439 \u043A\u0430\u0440\u044C\u0435\u0440.");
            player.closeInventory();
            return;
        }
        if (raw == MAIN_FUEL_SLOT) {
            openFuelGui(player, block);
            return;
        }
        if (raw == MAIN_TOGGLE_SLOT) {
            boolean running = quarryManager.isRunning(block);
            if (!running) {
                if (quarryManager.getFuel(block) <= 0 && !quarryManager.isSolarEnabled(block)) {
                    Msg.send(player, "&c\u041D\u0435\u0442 \u0442\u043E\u043F\u043B\u0438\u0432\u0430.");
                    return;
                }
                quarryManager.setRunning(block, true);
                quarryManager.startQuarry(block);
            } else {
                quarryManager.setRunning(block, false);
            }
            openMainGui(player, block);
            return;
        }
        if (raw == MAIN_SETTINGS_SLOT) {
            openSettingsGui(player, block);
            return;
        }
        if (raw == MAIN_STORAGE_SLOT) {
            QuarryData qd = quarryManager.getData(block);
            if (qd != null) {
                openStorageGui(player, block, qd.page);
            }
        }
    }

    private void handleFuelGuiClick(Player player, int raw, InventoryClickEvent event) {
        Location loc = openFuel.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        Block block = loc.getBlock();
        if (!quarryManager.isQuarryBlock(block)) {
            return;
        }
        if (!canAccess(player, block)) {
            Msg.send(player, "&c\u042D\u0442\u043E \u043D\u0435 \u0442\u0432\u043E\u0439 \u043A\u0430\u0440\u044C\u0435\u0440.");
            player.closeInventory();
            return;
        }
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        if (raw == FUEL_BACK_SLOT) {
            openMainGui(player, block);
            return;
        }
        if (raw == FUEL_SOLAR_SLOT) {
            if (qd.level >= 3) {
                quarryManager.setSolarEnabled(block, !quarryManager.isSolarEnabled(block));
            }
            openFuelGui(player, block);
            return;
        }
        if (raw == FUEL_INPUT_SLOT) {
            handleFuelClick(event, player, block, qd);
        }
    }

    private void handleSettingsClick(Player player, int raw) {
        Location loc = openSettings.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        Block block = loc.getBlock();
        if (!quarryManager.isQuarryBlock(block)) {
            return;
        }
        if (!canAccess(player, block)) {
            Msg.send(player, "&c\u042D\u0442\u043E \u043D\u0435 \u0442\u0432\u043E\u0439 \u043A\u0430\u0440\u044C\u0435\u0440.");
            player.closeInventory();
            return;
        }
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        if (raw == SETTINGS_BACK_SLOT) {
            openMainGui(player, block);
            return;
        }
        if (raw == SETTINGS_SILK_SLOT && qd.level >= 4) {
            quarryManager.setSilk(block, !quarryManager.isSilk(block));
            openSettingsGui(player, block);
            return;
        }
        if (raw == SETTINGS_CHUNKS_SLOT && qd.level >= 5) {
            quarryManager.setMultiChunk(block, !quarryManager.isMultiChunk(block));
            openSettingsGui(player, block);
            return;
        }
        if (raw == SETTINGS_FILTER_SLOT && qd.level >= 4) {
            openFilterGui(player, block);
        }
    }

    private void handleStorageGuiClick(Player player, int raw, InventoryClickEvent event) {
        Location loc = openStorage.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        Block block = loc.getBlock();
        if (!quarryManager.isQuarryBlock(block)) {
            return;
        }
        if (!canAccess(player, block)) {
            Msg.send(player, "&c\u042D\u0442\u043E \u043D\u0435 \u0442\u0432\u043E\u0439 \u043A\u0430\u0440\u044C\u0435\u0440.");
            player.closeInventory();
            return;
        }
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        if (raw == STORAGE_BACK) {
            openMainGui(player, block);
            return;
        }
        if (raw == STORAGE_PREV) {
            if (qd.page > 0) {
                openStorageGui(player, block, qd.page - 1);
            }
            return;
        }
        if (raw == STORAGE_NEXT) {
            if (hasNextPage(qd)) {
                openStorageGui(player, block, qd.page + 1);
            }
            return;
        }
        if (raw >= STORAGE_START && raw <= STORAGE_END) {
            handleStorageClick(event, player, block, qd, raw);
        }
    }

    private void openFilterGui(Player player, Block block) {
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        Inventory inv = Bukkit.createInventory(null, STORAGE_GUI_SIZE, FILTER_TITLE);
        int i = 0;
        for (Material mat : qd.filter) {
            if (i >= 45) {
                break;
            }
            inv.setItem(i++, new ItemStack(mat));
        }
        FilterMode mode = quarryManager.getFilterMode(block);
        inv.setItem(45, button(Material.PAPER, "&e\u0420\u0435\u0436\u0438\u043C: " + (mode == FilterMode.KEEP ? "&a\u041E\u0441\u0442\u0430\u0432\u043B\u044F\u0442\u044C" : "&c\u0423\u0434\u0430\u043B\u044F\u0442\u044C")));
        openFilter.put(player.getUniqueId(), block.getLocation());
        player.openInventory(inv);
    }

    private void saveStorage(Player player, Inventory inv) {
        Location loc = openStorage.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        QuarryData qd = quarryManager.getData(loc.getBlock());
        if (qd == null || qd.level == 1) {
            return;
        }
        if (!canAccess(player, loc.getBlock())) {
            return;
        }
        int start = qd.page * storagePageSize();
        for (int i = 0; i < storagePageSize(); i++) {
            int idx = start + i;
            int slot = STORAGE_START + i;
            if (slot > STORAGE_END || idx >= qd.storage.size()) {
                break;
            }
            ItemStack it = inv.getItem(slot);
            qd.storage.set(idx, it);
        }
    }

    private void handleFuelClose(Player player, Inventory inv) {
        Location loc = openFuel.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        Block block = loc.getBlock();
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        if (!canAccess(player, block)) {
            return;
        }
        ItemStack fuel = inv.getItem(FUEL_INPUT_SLOT);
        if (fuel == null || fuel.getType() == Material.AIR) {
            return;
        }
        int per = fuelValue(fuel.getType(), qd.level);
        if (per <= 0) {
            return;
        }
        int amount = fuel.getAmount();
        quarryManager.addFuel(block, per * amount);
        inv.setItem(FUEL_INPUT_SLOT, null);
        if (fuel.getType() == Material.LAVA_BUCKET) {
            ItemStack buckets = new ItemStack(Material.BUCKET, amount);
            Map<Integer, ItemStack> left = player.getInventory().addItem(buckets);
            if (!left.isEmpty()) {
                for (ItemStack it : left.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), it);
                }
            }
        }
    }

    private void saveFilter(Player player, Inventory inv) {
        Location loc = openFilter.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        QuarryData qd = quarryManager.getData(loc.getBlock());
        if (qd == null) {
            return;
        }
        if (!canAccess(player, loc.getBlock())) {
            return;
        }
        EnumSet<Material> set = EnumSet.noneOf(Material.class);
        for (int i = 0; i < 45; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() != Material.AIR) {
                set.add(it.getType());
            }
        }
        qd.filter = set;
    }

    private boolean hasNextPage(QuarryData qd) {
        int start = (qd.page + 1) * storagePageSize();
        return start < qd.storage.size();
    }

    private int storagePageSize() {
        return STORAGE_END - STORAGE_START + 1;
    }

    private ItemStack info(Block block, QuarryData qd) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(Msg.color("&7\u0422\u043E\u043F\u043B\u0438\u0432\u043E: &f" + quarryManager.getFuel(block)));
            lore.add(Msg.color("&7\u0421\u0442\u0430\u0442\u0443\u0441: " + (quarryManager.isRunning(block) ? "&a\u0420\u0430\u0431\u043E\u0442\u0430\u0435\u0442" : "&c\u041E\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D")));
            meta.setDisplayName(Msg.color("&b\u0418\u043D\u0444\u043E"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
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

    private ItemStack placeholder() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack toggleButton(Block block) {
        boolean running = quarryManager.isRunning(block);
        if (running) {
            return button(Material.RED_DYE, "&c\u041E\u0441\u0442\u0430\u043D\u043E\u0432\u0438\u0442\u044C");
        }
        return button(Material.LIME_DYE, "&a\u0417\u0430\u043F\u0443\u0441\u0442\u0438\u0442\u044C");
    }

    private ItemStack solarButton(Block block, int level) {
        if (level < 3) {
            return button(Material.BARRIER, "&c\u0421\u043E\u043B\u043D\u0446\u0435: \u043D\u0435\u0434\u043E\u0441\u0442\u0443\u043F\u043D\u043E");
        }
        return button(Material.DAYLIGHT_DETECTOR,
                "&e\u0421\u043E\u043B\u043D\u0446\u0435: " + (quarryManager.isSolarEnabled(block) ? "&a\u0412\u043A\u043B" : "&c\u0412\u044B\u043A\u043B"));
    }

    private void scheduleMapCleanup(Player player, Map<UUID, Location> map, String prefix) {
        if (player == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            String current = player.getOpenInventory().getTitle();
            if (current == null || !current.startsWith(prefix)) {
                map.remove(player.getUniqueId());
            }
        });
    }

    private boolean isFuel(ItemStack item, int level) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        if (item.getType() == Material.WATER_BUCKET) {
            return level >= 2;
        }
        return item.getType().isFuel();
    }

    private int fuelValue(Material type, int level) {
        if (type == Material.WATER_BUCKET) {
            return level >= 2 ? 800 : 0;
        }
        if (type == Material.LAVA_BUCKET) {
            return 20000;
        }
        if (type == Material.COAL_BLOCK) {
            return 16000;
        }
        if (type == Material.BLAZE_ROD) {
            return 2400;
        }
        if (type == Material.COAL || type == Material.CHARCOAL) {
            return 1600;
        }
        if (type == Material.STICK) {
            return 100;
        }
        if (type == Material.BAMBOO) {
            return 50;
        }
        if (type.name().endsWith("_LOG") || type.name().endsWith("_PLANKS")) {
            return 300;
        }
        if (type.isFuel()) {
            return 200;
        }
        return 0;
    }

    private boolean hasLevel(UUID playerId, int level) {
        for (QuarryData qd : quarryManagerAll()) {
            if (qd.owner != null && qd.owner.equals(playerId) && qd.level == level) {
                return true;
            }
        }
        return false;
    }

    private boolean canAccess(Player player, Block block) {
        if (player == null || block == null) {
            return false;
        }
        if (player.isOp()) {
            return true;
        }
        UUID owner = quarryManager.getOwner(block);
        if (owner != null && owner.equals(player.getUniqueId())) {
            return true;
        }
        if (owner == null) {
            return false;
        }
        String ownerName = Bukkit.getOfflinePlayer(owner).getName();
        if (ownerName == null) {
            return false;
        }
        Scoreboard main = Bukkit.getScoreboardManager() != null
                ? Bukkit.getScoreboardManager().getMainScoreboard()
                : null;
        if (main == null) {
            return false;
        }
        Team playerTeam = main.getEntryTeam(player.getName());
        Team ownerTeam = main.getEntryTeam(ownerName);
        return playerTeam != null && playerTeam.equals(ownerTeam);
    }

    private List<QuarryData> quarryManagerAll() {
        List<QuarryData> list = new ArrayList<>();
        for (BlockState state : getLoadedQuarryStates()) {
            if (state instanceof Container) {
                Location loc = state.getLocation();
                QuarryData qd = quarryManager.getData(loc.getBlock());
                if (qd != null) {
                    list.add(qd);
                }
            }
        }
        return list;
    }

    private List<BlockState> getLoadedQuarryStates() {
        List<BlockState> list = new ArrayList<>();
        for (WorldChunk wc : WorldChunk.getLoaded()) {
            for (BlockState st : wc.states) {
                list.add(st);
            }
        }
        return list;
    }

    private static final class WorldChunk {
        final List<BlockState> states;

        private WorldChunk(List<BlockState> states) {
            this.states = states;
        }

        static List<WorldChunk> getLoaded() {
            List<WorldChunk> list = new ArrayList<>();
            for (org.bukkit.World w : Bukkit.getWorlds()) {
                for (org.bukkit.Chunk c : w.getLoadedChunks()) {
                    List<BlockState> states = new ArrayList<>();
                    for (BlockState st : c.getTileEntities()) {
                        states.add(st);
                    }
                    list.add(new WorldChunk(states));
                }
            }
            return list;
        }
    }
}
