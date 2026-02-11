package me.dominus.faivents.events.artifact;

import me.dominus.faivents.FAIventsPlugin;
import me.dominus.faivents.enchant.CustomEnchantBooks;
import me.dominus.faivents.events.EventManager;
import me.dominus.faivents.util.ConfigUtil;
import me.dominus.faivents.util.Msg;
import me.dominus.faivents.util.RandUtil;
import me.dominus.faivents.util.SafeWorldEdit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactHuntEvent implements EventManager.EventController, Listener {

    private final FAIventsPlugin plugin;
    private final NamespacedKey compassKey;
    private boolean running = false;
    private boolean rewarded = false;
    private Location artifactLoc;
    private Material artifactBlock;
    private BukkitTask task;
    private BukkitTask bossTask;
    private BossBar bossBar;
    private final Map<Location, Material> changedBlocks = new HashMap<>();

    public ArtifactHuntEvent(FAIventsPlugin plugin) {
        this.plugin = plugin;
        this.compassKey = new NamespacedKey(plugin, "artifact_compass");
    }

    @Override
    public String getId() {
        return "artifact";
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("artifact_hunt.enabled", true);
    }

    @Override
    public boolean start(CommandSender sender, String[] args) {
        if (args.length >= 3 && !"all".equalsIgnoreCase(args[2])) {
            Msg.send(sender, "&c\u0410\u0440\u0442\u0435\u0444\u0430\u043A\u0442 \u043D\u0435\u043B\u044C\u0437\u044F \u0441\u043F\u0430\u0432\u043D\u0438\u0442\u044C \u043D\u0430 \u0438\u0433\u0440\u043E\u043A\u0435.");
            return false;
        }

        World world = null;
        Location center = null;

        double sumX = 0;
        double sumZ = 0;
        int count = 0;

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getEnvironment() == World.Environment.NORMAL) {
                sumX += p.getLocation().getX();
                sumZ += p.getLocation().getZ();
                world = p.getWorld();
                count++;
            }
        }

        if (count == 0 || world == null) {
            Msg.send(sender, "&c\u041D\u0435\u0442 \u0438\u0433\u0440\u043E\u043A\u043E\u0432 \u0432 \u043E\u0431\u044B\u0447\u043D\u043E\u043C \u043C\u0438\u0440\u0435.");
            return false;
        }

        center = new Location(world, sumX / count, world.getSpawnLocation().getY(), sumZ / count);

        int min = plugin.getConfig().getInt("artifact_hunt.radius_min", 40);
        int max = plugin.getConfig().getInt("artifact_hunt.radius_max", 140);
        int radius = RandUtil.nextInt(min, max);
        double angle = Math.toRadians(RandUtil.nextInt(0, 360));
        int x = (int) (center.getX() + Math.cos(angle) * radius);
        int z = (int) (center.getZ() + Math.sin(angle) * radius);

        artifactBlock = ConfigUtil.getMaterial(plugin.getConfig().getString("artifact_hunt.artifact_block"), Material.BEACON);
        artifactLoc = SafeWorldEdit.getHighestSafe(world, x, z);
        if (artifactLoc == null) {
            Msg.send(sender, "&c\u041D\u0435 \u0443\u0434\u0430\u043B\u043E\u0441\u044C \u043D\u0430\u0439\u0442\u0438 \u043C\u0435\u0441\u0442\u043E \u0434\u043B\u044F \u0430\u0440\u0442\u0435\u0444\u0430\u043A\u0442\u0430.");
            return false;
        }

        rewarded = false;
        buildArtifactStructure(artifactLoc);
        clearInside(artifactLoc);
        placeArtifact(artifactLoc);
        giveCompasses();

        if (plugin.getConfig().getBoolean("artifact_hunt.announce_pvp_rule_only", true)) {
            Msg.send(sender, "&ePvP \u043D\u0430 \u0432\u0440\u0435\u043C\u044F \u0438\u0432\u0435\u043D\u0442\u0430 \u043D\u0435 \u043C\u0435\u043D\u044F\u0435\u0442\u0441\u044F. \u042D\u0442\u043E \u0442\u043E\u043B\u044C\u043A\u043E \u0438\u043D\u0444\u043E\u0440\u043C\u0430\u0446\u0438\u044F.");
        }

        int durationMin = plugin.getConfig().getInt("artifact_hunt.duration_minutes", 15);
        startBossBar(durationMin * 60);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                spawnParticles();
                updateCompasses();
            }
        }.runTaskTimer(plugin, 0L, 40L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (running && !rewarded) {
                stop(null);
            }
        }, durationMin * 60L * 20L);

        running = true;
        Msg.send(sender, "&a\u041E\u0445\u043E\u0442\u0430 \u0437\u0430 \u0430\u0440\u0442\u0435\u0444\u0430\u043A\u0442\u043E\u043C \u043D\u0430\u0447\u0430\u043B\u0430\u0441\u044C!");
        return true;
    }

    private void buildArtifactStructure(Location loc) {
        int radius = plugin.getConfig().getInt("artifact_hunt.structure.radius", 3);
        int height = plugin.getConfig().getInt("artifact_hunt.structure.height", 4);
        Material floor = ConfigUtil.getMaterial(plugin.getConfig().getString("artifact_hunt.structure.floor_material"), Material.POLISHED_BLACKSTONE_BRICKS);
        Material pillar = ConfigUtil.getMaterial(plugin.getConfig().getString("artifact_hunt.structure.pillar_material"), Material.DEEPSLATE_BRICKS);
        Material accent = ConfigUtil.getMaterial(plugin.getConfig().getString("artifact_hunt.structure.accent_material"), Material.AMETHYST_BLOCK);
        Material light = ConfigUtil.getMaterial(plugin.getConfig().getString("artifact_hunt.structure.light_material"), Material.SOUL_LANTERN);

        Location base = loc.clone().add(0, -1, 0);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Location p = base.clone().add(dx, 0, dz);
                setAndStore(p, floor);
            }
        }

        int corner = radius;
        for (int dy = 0; dy <= height; dy++) {
            setAndStore(base.clone().add(corner, dy, corner), pillar);
            setAndStore(base.clone().add(-corner, dy, corner), pillar);
            setAndStore(base.clone().add(corner, dy, -corner), pillar);
            setAndStore(base.clone().add(-corner, dy, -corner), pillar);
        }

        setAndStore(base.clone().add(0, height + 1, 0), accent);
        setAndStore(base.clone().add(1, height + 1, 0), accent);
        setAndStore(base.clone().add(-1, height + 1, 0), accent);
        setAndStore(base.clone().add(0, height + 1, 1), accent);
        setAndStore(base.clone().add(0, height + 1, -1), accent);
        setAndStore(base.clone().add(0, height + 2, 0), light);
    }

    private void clearInside(Location loc) {
        int radius = plugin.getConfig().getInt("artifact_hunt.structure.radius", 3);
        int height = plugin.getConfig().getInt("artifact_hunt.structure.height", 4);
        Location base = loc.clone().add(0, -1, 0);
        for (int dx = -(radius - 1); dx <= (radius - 1); dx++) {
            for (int dz = -(radius - 1); dz <= (radius - 1); dz++) {
                for (int dy = 1; dy <= height; dy++) {
                    Location p = base.clone().add(dx, dy, dz);
                    setAndStore(p, Material.AIR);
                }
            }
        }
    }

    private void setAndStore(Location loc, Material mat) {
        Block b = loc.getBlock();
        changedBlocks.putIfAbsent(b.getLocation(), b.getType());
        b.setType(mat, false);
    }

    private void placeArtifact(Location loc) {
        Block b = loc.getBlock();
        changedBlocks.putIfAbsent(b.getLocation(), b.getType());
        b.setType(artifactBlock, false);
    }

    private void giveCompasses() {
        String name = plugin.getConfig().getString("artifact_hunt.compass.name", "&e\u041A\u043E\u043C\u043F\u0430\u0441 \u0430\u0440\u0442\u0435\u0444\u0430\u043A\u0442\u0430");
        boolean offhand = plugin.getConfig().getBoolean("artifact_hunt.compass.offhand", true);
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack compass = new ItemStack(Material.COMPASS, 1);
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            if (meta != null) {
                meta.setLodestone(artifactLoc);
                meta.setLodestoneTracked(false);
                meta.setDisplayName(Msg.color(name));
                meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
                compass.setItemMeta(meta);
            }
            if (offhand) {
                ItemStack off = p.getInventory().getItemInOffHand();
                if (off != null && off.getType() != Material.AIR) {
                    Map<Integer, ItemStack> left = p.getInventory().addItem(off);
                    for (ItemStack l : left.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), l);
                    }
                }
                p.getInventory().setItemInOffHand(compass);
            } else {
                p.getInventory().addItem(compass);
            }
        }
    }

    private void updateCompasses() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : p.getInventory().getContents()) {
                updateCompass(item);
            }
            updateCompass(p.getInventory().getItemInOffHand());
        }
    }

    private void updateCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof CompassMeta)) {
            return;
        }
        if (!meta.getPersistentDataContainer().has(compassKey, PersistentDataType.BYTE)) {
            return;
        }
        CompassMeta c = (CompassMeta) meta;
        c.setLodestone(artifactLoc);
        c.setLodestoneTracked(false);
        item.setItemMeta(c);
    }

    private void removeCompasses() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack off = p.getInventory().getItemInOffHand();
            if (isArtifactCompass(off)) {
                p.getInventory().setItemInOffHand(null);
            }
            ItemStack[] contents = p.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                if (isArtifactCompass(contents[i])) {
                    contents[i] = null;
                }
            }
            p.getInventory().setContents(contents);
        }
    }

    private boolean isArtifactCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(compassKey, PersistentDataType.BYTE);
    }

    private void spawnParticles() {
        String pName = plugin.getConfig().getString("artifact_hunt.particle", "END_ROD");
        Particle particle = Particle.END_ROD;
        try {
            particle = Particle.valueOf(pName);
        } catch (IllegalArgumentException ignored) {
        }
        if (artifactLoc != null) {
            artifactLoc.getWorld().spawnParticle(particle, artifactLoc.clone().add(0.5, 1.0, 0.5), 20, 0.3, 0.6, 0.3, 0.01);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!running || artifactLoc == null) {
            return;
        }
        if (e.getBlock().getLocation().equals(artifactLoc.getBlock().getLocation())) {
            e.setDropItems(false);
            reward(e.getPlayer());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!running || artifactLoc == null || e.getClickedBlock() == null) {
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (e.getClickedBlock().getLocation().equals(artifactLoc.getBlock().getLocation())) {
            e.setCancelled(true);
            reward(e.getPlayer());
        }
    }

    private void reward(Player player) {
        if (!running || rewarded) {
            return;
        }
        rewarded = true;
        List<ItemStack> loot = ConfigUtil.rollLoot(plugin, "artifact_hunt.loot");
        if (loot.isEmpty()) {
            loot.add(new ItemStack(Material.GOLD_INGOT, 4));
        }
        CustomEnchantBooks.maybeAddCustomBook(plugin, loot, "artifact_hunt.custom_books");
        for (ItemStack item : loot) {
            player.getInventory().addItem(item);
        }
        Bukkit.broadcastMessage(Msg.color("&6[FAIvents]&r &a" + player.getName() + " \u043D\u0430\u0448\u0435\u043B \u0430\u0440\u0442\u0435\u0444\u0430\u043A\u0442!"));
        stop(null);
    }
    private void startBossBar(int totalSeconds) {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossBar = Bukkit.createBossBar(Msg.color("&6\u0410\u0440\u0442\u0435\u0444\u0430\u043A\u0442: " + formatTime(totalSeconds)), BarColor.YELLOW, BarStyle.SEGMENTED_10);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bossBar.addPlayer(p);
        }
        bossBar.setProgress(1.0);

        bossTask = new BukkitRunnable() {
            int left = totalSeconds;

            @Override
            public void run() {
                if (bossBar == null) {
                    cancel();
                    return;
                }
                if (left <= 0) {
                    bossBar.setProgress(0.0);
                    cancel();
                    return;
                }
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) left / (double) totalSeconds)));
                bossBar.setTitle(Msg.color("&6\u0410\u0440\u0442\u0435\u0444\u0430\u043A\u0442: " + formatTime(left)));
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void stopBossBar() {
        if (bossTask != null) {
            bossTask.cancel();
            bossTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private String formatTime(int seconds) {
        int m = Math.max(0, seconds) / 60;
        int s = Math.max(0, seconds) % 60;
        return String.format("%02d:%02d", m, s);
    }

    @Override
    public void stop(CommandSender sender) {
        if (!running) {
            return;
        }
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        stopBossBar();
        removeCompasses();
        restoreBlocks();
        if (sender != null) {
            Msg.send(sender, "&e\u041E\u0445\u043E\u0442\u0430 \u0437\u0430 \u0430\u0440\u0442\u0435\u0444\u0430\u043A\u0442\u043E\u043C \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043D\u0430.");
        }
        plugin.getEventManager().onEventStopped(getId());
    }

    private void restoreBlocks() {
        for (Map.Entry<Location, Material> entry : changedBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue(), false);
        }
        changedBlocks.clear();
    }

    @Override
    public void status(CommandSender sender) {
        Msg.send(sender, running ? "&a\u041E\u0445\u043E\u0442\u0430 \u0430\u043A\u0442\u0438\u0432\u043D\u0430." : "&e\u041E\u0445\u043E\u0442\u0430 \u043D\u0435 \u0430\u043A\u0442\u0438\u0432\u043D\u0430.");
    }

    @Override
    public void reload() {
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}