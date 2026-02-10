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

public final class QuarryListener implements Listener {

    private static final String GUI_TITLE = "\u041A\u0430\u0440\u044C\u0435\u0440";
    private static final String FILTER_TITLE = "\u0424\u0438\u043B\u044C\u0442\u0440";
    private static final int GUI_SIZE = 54;
    private static final int STORAGE_START = 9;
    private static final int STORAGE_END = 53;
    private static final int FUEL_SLOT = 2;

    private final QuarryManager quarryManager;
    private final Map<UUID, Location> openGui = new HashMap<>();
    private final Map<UUID, Location> openFilter = new HashMap<>();

    public QuarryListener(QuarryManager quarryManager) {
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
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        openMainGui(player, block, qd.page);
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
        if (!player.getUniqueId().equals(quarryManager.getOwner(block))) {
            Msg.send(player, "&c\u042D\u0442\u043E \u043D\u0435 \u0442\u0432\u043E\u0439 \u043A\u0430\u0440\u044C\u0435\u0440.");
            return;
        }
        openMainGui(player, block, 0);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!title.startsWith(GUI_TITLE) && !title.startsWith(FILTER_TITLE)) {
            return;
        }
        event.setCancelled(true);
        int raw = event.getRawSlot();
        if (title.startsWith(FILTER_TITLE)) {
            handleFilterClick(player, raw, event);
            return;
        }
        Location loc = openGui.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        Block block = loc.getBlock();
        if (!quarryManager.isQuarryBlock(block)) {
            return;
        }
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        int level = qd.level;
        switch (raw) {
            case 0:
                quarryManager.setRunning(block, true);
                openMainGui(player, block, qd.page);
                return;
            case 1:
                quarryManager.setRunning(block, false);
                openMainGui(player, block, qd.page);
                return;
            case FUEL_SLOT:
                handleFuelClick(event, player, block, qd);
                return;
            case 3:
                if (qd.page > 0) {
                    openMainGui(player, block, qd.page - 1);
                }
                return;
            case 5:
                if (hasNextPage(qd)) {
                    openMainGui(player, block, qd.page + 1);
                }
                return;
            case 6:
                if (level >= 4) {
                    quarryManager.setSilk(block, !quarryManager.isSilk(block));
                    openMainGui(player, block, qd.page);
                }
                return;
            case 7:
                if (level >= 5) {
                    quarryManager.setMultiChunk(block, !quarryManager.isMultiChunk(block));
                    openMainGui(player, block, qd.page);
                }
                return;
            case 8:
                if (level >= 4) {
                    openFilterGui(player, block);
                }
                return;
            default:
                break;
        }
        if (raw >= STORAGE_START && raw <= STORAGE_END) {
            handleStorageClick(event, player, block, qd, raw);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (title.startsWith(GUI_TITLE)) {
            saveStorage(player, event.getInventory());
            handleFuelClose(player, event.getInventory());
            openGui.remove(player.getUniqueId());
        }
        if (title.startsWith(FILTER_TITLE)) {
            saveFilter(player, event.getInventory());
            openFilter.remove(player.getUniqueId());
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

    private void openMainGui(Player player, Block block, int page) {
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        qd.page = Math.max(0, page);
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, GUI_TITLE + " \u0423\u0440\u043E\u0432\u0435\u043D\u044C " + qd.level);
        fillBorder(inv);

        inv.setItem(0, button(Material.LIME_DYE, "&a\u0417\u0430\u043F\u0443\u0441\u0442\u0438\u0442\u044C"));
        inv.setItem(1, button(Material.RED_DYE, "&c\u041E\u0441\u0442\u0430\u043D\u043E\u0432\u0438\u0442\u044C"));
        inv.setItem(FUEL_SLOT, button(Material.COAL, "&6\u0422\u043E\u043F\u043B\u0438\u0432\u043E"));
        inv.setItem(3, button(Material.ARROW, "&e\u041F\u0440\u0435\u0434\u044B\u0434\u0443\u0449\u0430\u044F"));
        inv.setItem(5, button(Material.ARROW, "&e\u0421\u043B\u0435\u0434\u0443\u044E\u0449\u0430\u044F"));
        inv.setItem(4, info(block, qd));

        if (qd.level >= 4) {
            inv.setItem(6, button(Material.ENCHANTED_BOOK, "&6\u0428\u0451\u043B\u043A\u043E\u0432\u043E\u0435: " + (quarryManager.isSilk(block) ? "&a\u0412\u043A\u043B" : "&c\u0412\u044B\u043A\u043B")));
            inv.setItem(8, button(Material.HOPPER, "&e\u0424\u0438\u043B\u044C\u0442\u0440"));
        }
        if (qd.level >= 5) {
            inv.setItem(7, button(Material.MAP, "&b\u0427\u0430\u043D\u043A\u0438 \u0432\u043E\u043A\u0440\u0443\u0433: " + (quarryManager.isMultiChunk(block) ? "&a\u0412\u043A\u043B" : "&c\u0412\u044B\u043A\u043B")));
        }

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
        openGui.put(player.getUniqueId(), block.getLocation());
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

    private void openFilterGui(Player player, Block block) {
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, FILTER_TITLE);
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
        Location loc = openGui.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        QuarryData qd = quarryManager.getData(loc.getBlock());
        if (qd == null || qd.level == 1) {
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
        Location loc = openGui.get(player.getUniqueId());
        if (loc == null) {
            return;
        }
        Block block = loc.getBlock();
        QuarryData qd = quarryManager.getData(block);
        if (qd == null) {
            return;
        }
        ItemStack fuel = inv.getItem(FUEL_SLOT);
        if (fuel == null || fuel.getType() == Material.AIR) {
            return;
        }
        int per = fuelValue(fuel.getType(), qd.level);
        if (per <= 0) {
            return;
        }
        int amount = fuel.getAmount();
        quarryManager.addFuel(block, per * amount);
        inv.setItem(FUEL_SLOT, null);
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

    private void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, pane);
        }
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
