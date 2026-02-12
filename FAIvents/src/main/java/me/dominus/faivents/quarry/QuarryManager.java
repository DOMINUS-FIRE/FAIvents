package me.dominus.faivents.quarry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class QuarryManager {

    private static final int[] BLOCKS_PER_TICK = new int[] { 1, 1, 2, 3, 4 };
    private static final long TASK_INTERVAL = 4L;

    public enum FilterMode {
        KEEP,
        DELETE
    }

    private final JavaPlugin plugin;
    private final NamespacedKey quarryKey;
    private final NamespacedKey levelKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey fuelKey;
    private final NamespacedKey runningKey;
    private final NamespacedKey silkKey;
    private final NamespacedKey filterModeKey;
    private final NamespacedKey multiChunkKey;
    private final NamespacedKey solarKey;

    private final Map<LocationKey, QuarryTask> tasks = new HashMap<>();
    private final Map<LocationKey, QuarryData> data = new HashMap<>();
    private final Map<LocationKey, Entity> holograms = new HashMap<>();

    public QuarryManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.quarryKey = new NamespacedKey(plugin, "quarry");
        this.levelKey = new NamespacedKey(plugin, "quarry_level");
        this.ownerKey = new NamespacedKey(plugin, "quarry_owner");
        this.fuelKey = new NamespacedKey(plugin, "quarry_fuel");
        this.runningKey = new NamespacedKey(plugin, "quarry_running");
        this.silkKey = new NamespacedKey(plugin, "quarry_silk");
        this.filterModeKey = new NamespacedKey(plugin, "quarry_filter_mode");
        this.multiChunkKey = new NamespacedKey(plugin, "quarry_multi_chunk");
        this.solarKey = new NamespacedKey(plugin, "quarry_solar");
    }

    public ItemStack createQuarryItem(int level) {
        ItemStack item = new ItemStack(Material.DISPENSER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00A7a\u041A\u0430\u0440\u044C\u0435\u0440 \u0423\u0440\u043E\u0432\u0435\u043D\u044C " + level);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(quarryKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(levelKey, PersistentDataType.INTEGER, level);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isQuarryItem(ItemStack item) {
        if (item == null || item.getType() != Material.DISPENSER) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(quarryKey, PersistentDataType.BYTE);
    }

    public int getItemLevel(ItemStack item) {
        if (!isQuarryItem(item)) {
            return 1;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 1;
        }
        Integer lvl = meta.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
        if (lvl == null) {
            return 1;
        }
        return Math.max(1, Math.min(5, lvl));
    }

    public boolean isQuarryBlock(Block block) {
        if (block == null || block.getType() != Material.DISPENSER) {
            return false;
        }
        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) {
            return false;
        }
        return tile.getPersistentDataContainer().has(quarryKey, PersistentDataType.BYTE);
    }

    public QuarryData getData(Block block) {
        if (block == null) {
            return null;
        }
        return data.get(LocationKey.from(block.getLocation()));
    }

    public void registerQuarry(Block block, Player owner, int level) {
        if (block == null || owner == null) {
            return;
        }
        level = Math.max(1, Math.min(5, level));
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            PersistentDataContainer pdc = tile.getPersistentDataContainer();
            pdc.set(quarryKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(levelKey, PersistentDataType.INTEGER, level);
            pdc.set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
            pdc.set(fuelKey, PersistentDataType.INTEGER, 0);
            pdc.set(runningKey, PersistentDataType.BYTE, (byte) 1);
            pdc.set(silkKey, PersistentDataType.BYTE, (byte) 0);
            pdc.set(filterModeKey, PersistentDataType.STRING, FilterMode.KEEP.name());
            pdc.set(multiChunkKey, PersistentDataType.BYTE, (byte) 0);
            pdc.set(solarKey, PersistentDataType.BYTE, (byte) 1);
            tile.update(true, false);
        }
        QuarryData qd = new QuarryData(owner.getUniqueId(), level);
        data.put(LocationKey.from(block.getLocation()), qd);
        startQuarry(block);
        updateHologram(block);
    }

    public void clearQuarry(Block block) {
        if (block == null) {
            return;
        }
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            PersistentDataContainer pdc = tile.getPersistentDataContainer();
            pdc.remove(quarryKey);
            pdc.remove(levelKey);
            pdc.remove(ownerKey);
            pdc.remove(fuelKey);
            pdc.remove(runningKey);
            pdc.remove(silkKey);
            pdc.remove(filterModeKey);
            pdc.remove(multiChunkKey);
            pdc.remove(solarKey);
            tile.update(true, false);
        }
        LocationKey key = LocationKey.from(block.getLocation());
        data.remove(key);
        stopQuarry(block);
        removeHologram(block);
        removeNearbyHolograms(block);
    }

    public UUID getOwner(Block block) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            String id = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (id != null) {
                return UUID.fromString(id);
            }
        }
        return null;
    }

    public int getLevel(Block block) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            Integer lvl = tile.getPersistentDataContainer().get(levelKey, PersistentDataType.INTEGER);
            if (lvl != null) {
                return Math.max(1, Math.min(5, lvl));
            }
        }
        return 1;
    }

    public boolean isRunning(Block block) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            Byte b = tile.getPersistentDataContainer().get(runningKey, PersistentDataType.BYTE);
            return b != null && b == 1;
        }
        return false;
    }

    public void setRunning(Block block, boolean running) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            tile.getPersistentDataContainer().set(runningKey, PersistentDataType.BYTE, (byte) (running ? 1 : 0));
            tile.update(true, false);
        }
        updateHologram(block);
    }

    public boolean isSilk(Block block) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            Byte b = tile.getPersistentDataContainer().get(silkKey, PersistentDataType.BYTE);
            return b != null && b == 1;
        }
        return false;
    }

    public void setSilk(Block block, boolean value) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            tile.getPersistentDataContainer().set(silkKey, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
            tile.update(true, false);
        }
    }

    public FilterMode getFilterMode(Block block) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            String mode = tile.getPersistentDataContainer().get(filterModeKey, PersistentDataType.STRING);
            if (mode != null) {
                try {
                    return FilterMode.valueOf(mode);
                } catch (IllegalArgumentException ignored) {
                    return FilterMode.KEEP;
                }
            }
        }
        return FilterMode.KEEP;
    }

    public void setFilterMode(Block block, FilterMode mode) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            tile.getPersistentDataContainer().set(filterModeKey, PersistentDataType.STRING, mode.name());
            tile.update(true, false);
        }
    }

    public boolean isMultiChunk(Block block) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            Byte b = tile.getPersistentDataContainer().get(multiChunkKey, PersistentDataType.BYTE);
            return b != null && b == 1;
        }
        return false;
    }

    public void setMultiChunk(Block block, boolean value) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            tile.getPersistentDataContainer().set(multiChunkKey, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
            tile.update(true, false);
        }
    }

    public boolean isSolarEnabled(Block block) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            Byte b = tile.getPersistentDataContainer().get(solarKey, PersistentDataType.BYTE);
            return b == null || b == 1;
        }
        return true;
    }

    public void setSolarEnabled(Block block, boolean value) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            tile.getPersistentDataContainer().set(solarKey, PersistentDataType.BYTE, (byte) (value ? 1 : 0));
            tile.update(true, false);
        }
    }

    public int getFuel(Block block) {
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            Integer value = tile.getPersistentDataContainer().get(fuelKey, PersistentDataType.INTEGER);
            return value != null ? value : 0;
        }
        return 0;
    }

    public void addFuel(Block block, int amount) {
        if (amount <= 0) {
            return;
        }
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            int fuel = getFuel(block);
            tile.getPersistentDataContainer().set(fuelKey, PersistentDataType.INTEGER, fuel + amount);
            tile.update(true, false);
        }
        updateHologram(block);
    }

    public int getStorageSize(int level) {
        if (level == 1) {
            return 0;
        }
        if (level == 2) {
            return 54;
        }
        if (level == 3) {
            return 162;
        }
        if (level == 4) {
            return 180;
        }
        return 270;
    }

    public void startQuarry(Block block) {
        if (block == null || block.getType() != Material.DISPENSER) {
            return;
        }
        LocationKey key = LocationKey.from(block.getLocation());
        if (tasks.containsKey(key)) {
            return;
        }
        QuarryTask task = new QuarryTask(block);
        tasks.put(key, task);
        task.runTaskTimer(plugin, 1L, TASK_INTERVAL);
    }

    public void stopQuarry(Block block) {
        if (block == null) {
            return;
        }
        LocationKey key = LocationKey.from(block.getLocation());
        QuarryTask task = tasks.remove(key);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        for (QuarryTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        for (Entity e : holograms.values()) {
            if (e != null && !e.isDead()) {
                e.remove();
            }
        }
        holograms.clear();
        data.clear();
    }

    public void updateHologram(Block block) {
        if (block == null) {
            return;
        }
        LocationKey key = LocationKey.from(block.getLocation());
        Entity existing = holograms.get(key);
        if (!isQuarryBlock(block)) {
            if (existing != null && !existing.isDead()) {
                existing.remove();
            }
            holograms.remove(key);
            return;
        }
        String ownerName = "Unknown";
        UUID owner = getOwner(block);
        if (owner != null) {
            Player p = Bukkit.getPlayer(owner);
            if (p != null) {
                ownerName = p.getName();
            }
        }
        String status = isRunning(block) ? "\u00A7a\u0420\u0430\u0431\u043E\u0442\u0430\u0435\u0442" : "\u00A7c\u041E\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D";
        String text = "\u00A7f\u0412\u043B\u0430\u0434\u0435\u043B\u0435\u0446: \u00A7e" + ownerName + "\n" + status;
        if (existing instanceof TextDisplay td) {
            td.setText(text);
            return;
        }
        try {
            Location loc = block.getLocation().clone().add(0.5, 1.6, 0.5);
            TextDisplay td = block.getWorld().spawn(loc, TextDisplay.class);
            td.setText(text);
            td.setBillboard(TextDisplay.Billboard.CENTER);
            td.setSeeThrough(true);
            td.setShadowed(false);
            holograms.put(key, td);
        } catch (Throwable ignored) {
            // TextDisplay not available
        }
    }

    public int cleanupOrphanedHolograms() {
        int removed = 0;
        for (World w : Bukkit.getWorlds()) {
            for (TextDisplay td : w.getEntitiesByClass(TextDisplay.class)) {
                if (!isQuarryHologram(td)) {
                    continue;
                }
                Location loc = td.getLocation();
                Block base = loc.clone().subtract(0.5, 1.6, 0.5).getBlock();
                if (!isQuarryBlock(base)) {
                    td.remove();
                    removed++;
                    continue;
                }
                td.remove();
                removed++;
                updateHologram(base);
            }
        }
        return removed;
    }

    private void removeHologram(Block block) {
        if (block == null) {
            return;
        }
        LocationKey key = LocationKey.from(block.getLocation());
        Entity e = holograms.remove(key);
        if (e != null && !e.isDead()) {
            e.remove();
        }
    }

    private void removeNearbyHolograms(Block block) {
        if (block == null) {
            return;
        }
        for (TextDisplay td : block.getWorld().getEntitiesByClass(TextDisplay.class)) {
            if (!isQuarryHologram(td)) {
                continue;
            }
            if (td.getLocation().distanceSquared(block.getLocation()) <= 9.0) {
                td.remove();
            }
        }
    }

    private final class QuarryTask extends BukkitRunnable {

        private final Location origin;
        private final World world;
        private List<BlockPos> queue;
        private int queueIndex = 0;
        private int chunkX;
        private int chunkZ;
        private int spiralStep = 0;
        private int spiralLeg = 1;
        private int spiralDir = 0;
        private int spiralProgress = 0;
        private int solarTick = 0;

        QuarryTask(Block block) {
            this.origin = block.getLocation().clone();
            this.world = block.getWorld();
            this.chunkX = block.getChunk().getX();
            this.chunkZ = block.getChunk().getZ();
            this.queue = buildQueue(block.getChunk());
        }

        @Override
        public void run() {
            Block quarryBlock = origin.getBlock();
            if (!isQuarryBlock(quarryBlock)) {
                stopQuarry(quarryBlock);
                cancel();
                return;
            }
            if (!quarryBlock.getChunk().isLoaded()) {
                return;
            }
            if (!isRunning(quarryBlock)) {
                return;
            }

            QuarryData qd = data.get(LocationKey.from(origin));
            if (qd == null) {
                qd = new QuarryData(getOwner(quarryBlock), getLevel(quarryBlock));
                data.put(LocationKey.from(origin), qd);
            }

            int level = qd.level;
            if (level >= 3 && isSolarEnabled(quarryBlock)) {
                solarTick++;
                if (solarTick >= 20) {
                    solarTick = 0;
                    if (isDay(world) && quarryBlock.getLightFromSky() >= 14) {
                        addFuel(quarryBlock, 1);
                    }
                }
            }

            if (getFuel(quarryBlock) <= 0) {
                if (!isSolarEnabled(quarryBlock)) {
                    setRunning(quarryBlock, false);
                }
                return;
            }

            int limit = BLOCKS_PER_TICK[Math.max(0, Math.min(4, level - 1))];
            int processed = 0;
            while (processed < limit && queueIndex < queue.size()) {
                if (!consumeFuel(quarryBlock, 1)) {
                    break;
                }
                BlockPos pos = queue.get(queueIndex++);
                Block block = world.getBlockAt(pos.x, pos.y, pos.z);
                if (!shouldMine(block, quarryBlock)) {
                    processed++;
                    continue;
                }
                mineBlock(block, quarryBlock, qd);
                processed++;
            }

            if (queueIndex >= queue.size()) {
                if (level >= 5 && isMultiChunk(quarryBlock)) {
                    moveToNextChunk();
                    queue = buildQueue(world.getChunkAt(chunkX, chunkZ));
                    queueIndex = 0;
                } else {
                    stopQuarry(quarryBlock);
                    cancel();
                }
            }
        }

        private void moveToNextChunk() {
            if (spiralProgress >= spiralLeg) {
                spiralProgress = 0;
                spiralDir = (spiralDir + 1) % 4;
                spiralStep++;
                if (spiralStep % 2 == 0) {
                    spiralLeg++;
                }
            }
            switch (spiralDir) {
                case 0 -> chunkX++;
                case 1 -> chunkZ++;
                case 2 -> chunkX--;
                case 3 -> chunkZ--;
                default -> chunkX++;
            }
            spiralProgress++;
        }

        private List<BlockPos> buildQueue(Chunk chunk) {
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight() - 1;
            int startX = chunk.getX() << 4;
            int startZ = chunk.getZ() << 4;
            List<BlockPos> list = new ArrayList<>(16 * 16 * (maxY - minY + 1));
            for (int y = maxY; y >= minY; y--) {
                for (int x = startX; x < startX + 16; x++) {
                    for (int z = startZ; z < startZ + 16; z++) {
                        list.add(new BlockPos(x, y, z));
                    }
                }
            }
            return list;
        }
    }

    private boolean isQuarryHologram(TextDisplay td) {
        if (td == null) {
            return false;
        }
        String text = td.getText();
        if (text == null) {
            return false;
        }
        if (!text.contains("\u0412\u043B\u0430\u0434\u0435\u043B\u0435\u0446:")) {
            return false;
        }
        return text.contains("\u0420\u0430\u0431\u043E\u0442\u0430\u0435\u0442") || text.contains("\u041E\u0441\u0442\u0430\u043D\u043E\u0432\u043B\u0435\u043D");
    }

    private boolean consumeFuel(Block block, int amount) {
        if (amount <= 0) {
            return true;
        }
        BlockState state = block.getState();
        if (state instanceof TileState tile) {
            int fuel = getFuel(block);
            if (fuel < amount) {
                return false;
            }
            tile.getPersistentDataContainer().set(fuelKey, PersistentDataType.INTEGER, fuel - amount);
            tile.update(true, false);
            return true;
        }
        return false;
    }

    private boolean shouldMine(Block block, Block quarryBlock) {
        if (block == null) {
            return false;
        }
        if (block.equals(quarryBlock)) {
            return false;
        }
        BlockState state = block.getState();
        if (state instanceof Container) {
            return false;
        }
        Material type = block.getType();
        if (type.isAir()) {
            return false;
        }
        if (type == Material.WATER || type == Material.LAVA) {
            return false;
        }
        if (type == Material.BEDROCK || type == Material.BARRIER) {
            return false;
        }
        if (type == Material.COMMAND_BLOCK || type == Material.CHAIN_COMMAND_BLOCK || type == Material.REPEATING_COMMAND_BLOCK) {
            return false;
        }
        if (type == Material.STRUCTURE_BLOCK || type == Material.JIGSAW) {
            return false;
        }
        if (type == Material.END_PORTAL || type == Material.NETHER_PORTAL || type == Material.END_PORTAL_FRAME) {
            return false;
        }
        return true;
    }

    private boolean isDay(World world) {
        long time = world.getTime() % 24000L;
        return time < 12300 || time > 23850;
    }

    private void mineBlock(Block block, Block quarryBlock, QuarryData qd) {
        ItemStack tool = new ItemStack(Material.DIAMOND_PICKAXE);
        if (qd.level >= 4 && isSilk(quarryBlock)) {
            tool.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH, 1);
        }
        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool));
        if (!drops.isEmpty()) {
            handleDrops(quarryBlock, qd, drops);
        }
        block.setType(Material.AIR, false);
    }

    private void handleDrops(Block quarryBlock, QuarryData qd, List<ItemStack> drops) {
        List<ItemStack> filtered = new ArrayList<>();
        FilterMode mode = getFilterMode(quarryBlock);
        EnumSet<Material> filter = qd.filter;
        for (ItemStack item : drops) {
            Material mat = item.getType();
            boolean inFilter = filter.contains(mat);
            if (mode == FilterMode.KEEP && filter.size() > 0 && !inFilter) {
                continue;
            }
            if (mode == FilterMode.DELETE && inFilter) {
                continue;
            }
            filtered.add(item);
        }
        if (filtered.isEmpty()) {
            return;
        }
        if (qd.level >= 2) {
            for (ItemStack item : filtered) {
                ItemStack remaining = addToStorage(qd, item);
                if (remaining != null) {
                    insertDrops(quarryBlock, remaining);
                }
            }
        } else {
            for (ItemStack item : filtered) {
                insertDrops(quarryBlock, item);
            }
        }
    }

    private ItemStack addToStorage(QuarryData qd, ItemStack item) {
        if (qd.storage.isEmpty()) {
            return item;
        }
        ItemStack remaining = item.clone();
        for (int i = 0; i < qd.storage.size(); i++) {
            ItemStack slot = qd.storage.get(i);
            if (slot == null) {
                int max = remaining.getMaxStackSize();
                int toMove = Math.min(max, remaining.getAmount());
                ItemStack put = remaining.clone();
                put.setAmount(toMove);
                qd.storage.set(i, put);
                remaining.setAmount(remaining.getAmount() - toMove);
                if (remaining.getAmount() <= 0) {
                    return null;
                }
            } else if (slot.isSimilar(remaining) && slot.getAmount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getAmount();
                int toMove = Math.min(space, remaining.getAmount());
                slot.setAmount(slot.getAmount() + toMove);
                remaining.setAmount(remaining.getAmount() - toMove);
                if (remaining.getAmount() <= 0) {
                    return null;
                }
            }
        }
        return remaining;
    }

    private void insertDrops(Block quarryBlock, ItemStack item) {
        List<Inventory> outputs = findOutputs(quarryBlock);
        if (outputs.isEmpty()) {
            quarryBlock.getWorld().dropItemNaturally(quarryBlock.getLocation(), item);
            return;
        }
        ItemStack remaining = item;
        for (Inventory inv : outputs) {
            Map<Integer, ItemStack> left = inv.addItem(remaining);
            if (left.isEmpty()) {
                remaining = null;
                break;
            }
            remaining = left.values().iterator().next();
        }
        if (remaining != null) {
            quarryBlock.getWorld().dropItemNaturally(quarryBlock.getLocation(), remaining);
        }
    }

    private List<Inventory> findOutputs(Block quarryBlock) {
        List<Inventory> list = new ArrayList<>();
        Block[] neighbors = new Block[] {
                quarryBlock.getRelative(1, 0, 0),
                quarryBlock.getRelative(-1, 0, 0),
                quarryBlock.getRelative(0, 1, 0),
                quarryBlock.getRelative(0, -1, 0),
                quarryBlock.getRelative(0, 0, 1),
                quarryBlock.getRelative(0, 0, -1)
        };
        for (Block b : neighbors) {
            BlockState st = b.getState();
            if (st instanceof Container container) {
                list.add(container.getInventory());
            }
        }
        return list;
    }

    public static final class QuarryData {
        public final UUID owner;
        public final int level;
        public int page = 0;
        public boolean running = true;
        public EnumSet<Material> filter = EnumSet.noneOf(Material.class);
        public final List<ItemStack> storage;

        QuarryData(UUID owner, int level) {
            this.owner = owner;
            this.level = level;
            int size = 0;
            if (level == 2) {
                size = 54;
            } else if (level == 3) {
                size = 162;
            } else if (level == 4) {
                size = 180;
            } else if (level >= 5) {
                size = 270;
            }
            storage = new ArrayList<>(Collections.nCopies(size, null));
        }
    }

    private static final class BlockPos {
        final int x;
        final int y;
        final int z;

        BlockPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

    }

    private static final class LocationKey {
        final UUID world;
        final int x;
        final int y;
        final int z;

        private LocationKey(UUID world, int x, int y, int z) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        static LocationKey from(Location loc) {
            return new LocationKey(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LocationKey other)) {
                return false;
            }
            return x == other.x && y == other.y && z == other.z && world.equals(other.world);
        }

        @Override
        public int hashCode() {
            int result = world.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }
}
