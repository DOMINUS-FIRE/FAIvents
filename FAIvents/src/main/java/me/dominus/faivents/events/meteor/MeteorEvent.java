package me.dominus.faivents.events.meteor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import me.dominus.faivents.FAIventsPlugin;
import me.dominus.faivents.enchant.CustomEnchantBooks;
import me.dominus.faivents.events.EventManager;
import me.dominus.faivents.util.ConfigUtil;
import me.dominus.faivents.util.Msg;
import me.dominus.faivents.util.RandUtil;
import me.dominus.faivents.util.SafeWorldEdit;

public class MeteorEvent implements EventManager.EventController {

    private static class MeteorPart {
        final BlockDisplay display;
        final Vector offset;

        MeteorPart(BlockDisplay display, Vector offset) {
            this.display = display;
            this.offset = offset;
        }
    }

    private final FAIventsPlugin plugin;
    private boolean running = false;
    private BukkitTask task;
    private final List<MeteorPart> parts = new ArrayList<>();
    private Location target;
    private Location center;
    private final List<UUID> pendingTargets = new ArrayList<>();
    private CommandSender lastSender;
    private String currentRarity = "common";

    public MeteorEvent(FAIventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "meteor";
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("meteor.enabled", true);
    }

    @Override
    public boolean start(CommandSender sender, String[] args) {
        if (running) {
            Msg.send(sender, "&e\u0418\u0432\u0435\u043D\u0442 \u0443\u0436\u0435 \u0437\u0430\u043F\u0443\u0449\u0435\u043D.");
            return false;
        }
        pendingTargets.clear();
        lastSender = sender;

        if (args.length >= 3) {
            if ("all".equalsIgnoreCase(args[2])) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    pendingTargets.add(p.getUniqueId());
                }
            } else {
                Player p = Bukkit.getPlayerExact(args[2]);
                if (p == null) {
                    Msg.send(sender, "&c\u0418\u0433\u0440\u043E\u043A \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D.");
                    return false;
                }
                pendingTargets.add(p.getUniqueId());
            }
        } else if (sender instanceof Player) {
            pendingTargets.add(((Player) sender).getUniqueId());
        } else {
            Msg.send(sender, "&c\u0423\u043A\u0430\u0436\u0438\u0442\u0435 \u0438\u0433\u0440\u043E\u043A\u0430 \u0438\u043B\u0438 all.");
            return false;
        }

        launchNext();
        return true;
    }

    private void launchNext() {
        if (pendingTargets.isEmpty()) {
            running = false;
            plugin.getEventManager().onEventStopped(getId());
            return;
        }
        Player player = Bukkit.getPlayer(pendingTargets.remove(0));
        if (player == null) {
            launchNext();
            return;
        }

        currentRarity = chooseRarity();

        int max = plugin.getConfig().getInt("meteor.raytrace_max_distance", 120);
        World world = player.getWorld();
        Vector look = player.getEyeLocation().getDirection().normalize();
        RayTraceResult hit = world.rayTraceBlocks(player.getEyeLocation(), look, max);
        Location hitLoc;
        if (hit != null && hit.getHitPosition() != null) {
            hitLoc = hit.getHitPosition().toLocation(world);
        } else {
            hitLoc = player.getEyeLocation().add(look.clone().multiply(max));
        }
        target = new Location(world, hitLoc.getBlockX() + 0.5, hitLoc.getBlockY(), hitLoc.getBlockZ() + 0.5);

        Location rod = findNearestLightningRod(target);
        if (rod != null && plugin.getConfig().getBoolean("meteor.lightning_rod.force_if_found", true)) {
            target = rod.clone();
        }

        startFlight(world, target.clone());
        Msg.send(lastSender != null ? lastSender : player, "&a\u041C\u0435\u0442\u0435\u043E\u0440 \u043F\u0430\u0434\u0430\u0435\u0442 \u043D\u0430: " + player.getName());
        running = true;
    }

    private String chooseRarity() {
        double common = plugin.getConfig().getDouble("meteor.rarity.common", 0.6);
        double rare = plugin.getConfig().getDouble("meteor.rarity.rare", 0.25);
        double epic = plugin.getConfig().getDouble("meteor.rarity.epic", 0.12);
        double legendary = plugin.getConfig().getDouble("meteor.rarity.legendary", 0.03);

        double roll = Math.random();
        double sum = common;
        if (roll <= sum) {
            return "common";
        }
        sum += rare;
        if (roll <= sum) {
            return "rare";
        }
        sum += epic;
        if (roll <= sum) {
            return "epic";
        }
        return "legendary";
    }

    private Location findNearestLightningRod(Location center) {
        int radius = plugin.getConfig().getInt("meteor.lightning_rod.search_radius", 25);
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        Location best = null;
        double bestDist = Double.MAX_VALUE;
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -4; y <= 4; y++) {
                    Block b = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (b.getType() == Material.LIGHTNING_ROD) {
                        double d = b.getLocation().distanceSquared(center);
                        if (d < bestDist) {
                            bestDist = d;
                            best = b.getLocation().add(0.5, 0, 0.5);
                        }
                    }
                }
            }
        }
        return best;
    }

    private void startFlight(World world, Location target) {
        int height = plugin.getConfig().getInt("meteor.flight.height", 50);
        double speed = plugin.getConfig().getDouble("meteor.flight.speed_blocks_per_tick", 1.6);
        String particleName = plugin.getConfig().getString("meteor.flight.trail_particle", "FLAME");
        Particle particle = Particle.FLAME;
        try {
            particle = Particle.valueOf(particleName);
        } catch (IllegalArgumentException ignored) {
        }
        int particleCount = plugin.getConfig().getInt("meteor.flight.trail_particle_count", 14);
        final Particle trail = particle;
        final int trailCount = Math.max(1, particleCount);

        final Location targetFinal = target.clone();
        final double speedFinal = speed;
        center = targetFinal.clone().add(0, height, 0);
        final Vector dir = targetFinal.clone().subtract(center).toVector().normalize().multiply(speedFinal);
        spawnMeteorModel(world, center);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (parts.isEmpty()) {
                    cancel();
                    return;
                }
                RayTraceResult hit = world.rayTraceBlocks(center, dir.clone().normalize(), speedFinal);
                if (hit != null && hit.getHitPosition() != null) {
                    Location impact = hit.getHitPosition().toLocation(world);
                    removeMeteorModel();
                    impact(impact);
                    cancel();
                    return;
                }
                center = center.clone().add(dir);
                moveMeteorModel(center);
                world.spawnParticle(trail, center, trailCount, 0.6, 0.6, 0.6, 0.02);
                if (center.distanceSquared(targetFinal) <= (speedFinal * speedFinal)) {
                    removeMeteorModel();
                    impact(targetFinal);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void impact(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        String soundName = plugin.getConfig().getString("meteor.flight.impact_sound", "ENTITY_GENERIC_EXPLODE");
        Sound sound = Sound.ENTITY_GENERIC_EXPLODE;
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException ignored) {
        }
        world.playSound(loc, sound, 2.0f, 0.8f);
        world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 2);

        createCrater(loc);
        spawnLootChest(loc);
        finishCurrent();
    }

    private void finishCurrent() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeMeteorModel();
        Bukkit.getScheduler().runTask(plugin, this::launchNext);
    }

    private void createCrater(Location center) {
        int radius = plugin.getConfig().getInt("meteor.crater.radius", 7);
        int depth = plugin.getConfig().getInt("meteor.crater.depth", 6);
        List<Material> palette = buildPalette();
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius + RandUtil.nextInt(-1, 1) * 0.3) {
                    continue;
                }
                double noise = (Math.random() * 0.8) - 0.4;
                int localDepth = (int) Math.max(1, Math.round(depth * (1.0 - (dist / radius)) + noise * depth));
                for (int dy = 0; dy <= localDepth; dy++) {
                    int y = cy - dy;
                    Block b = world.getBlockAt(cx + dx, y, cz + dz);
                    if (dy == localDepth && !palette.isEmpty()) {
                        b.setType(RandUtil.pick(palette), false);
                    } else {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private List<Material> buildPalette() {
        List<Material> list = new ArrayList<>();
        List<Map<?, ?>> palette = plugin.getConfig().getMapList("meteor.crater.core_palette");
        for (Map<?, ?> raw : palette) {
            String matName = raw.get("material") != null ? raw.get("material").toString() : "MAGMA_BLOCK";
            int weight = 1;
            if (raw.get("weight") != null) {
                try {
                    weight = Integer.parseInt(raw.get("weight").toString());
                } catch (NumberFormatException ignored) {
                }
            }
            Material mat = ConfigUtil.getMaterial(matName, Material.MAGMA_BLOCK);
            for (int i = 0; i < weight; i++) {
                list.add(mat);
            }
        }
        return list;
    }

        private void spawnLootChest(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        int maxDepth = plugin.getConfig().getInt("meteor.crater.depth", 6);
        int x = center.getBlockX();
        int z = center.getBlockZ();
        int y = center.getBlockY();
        int minY = y - maxDepth - 3;
        while (y > minY && world.getBlockAt(x, y, z).getType() == Material.AIR) {
            y--;
        }
        Location place = new Location(world, x, y + 1, z);
        Location safe = SafeWorldEdit.findNearestAir(place, 3, 2);
        if (safe == null) {
            safe = place;
        }
        Block block = safe.getBlock();
        block.setType(ConfigUtil.getMaterial(plugin.getConfig().getString("meteor.loot.chest_material"), Material.CHEST), false);
        Bukkit.getScheduler().runTask(plugin, () -> fillChest(block));
    }

        private void fillChest(Block block) {
        if (block == null) {
            return;
        }
        if (block.getType() != Material.CHEST) {
            block.setType(Material.CHEST, false);
        }
        if (!(block.getState() instanceof Chest chest)) {
            return;
        }
        Inventory inv = chest.getBlockInventory();
        List<ItemStack> loot = ConfigUtil.rollLoot(plugin, "meteor.loot.tiers." + currentRarity);
        if (loot.isEmpty()) {
            loot = ConfigUtil.rollLoot(plugin, "meteor.loot.loot");
        }
        if (loot.isEmpty()) {
            loot.add(new ItemStack(Material.IRON_INGOT, 6));
        }
        CustomEnchantBooks.maybeAddCustomBook(plugin, loot, "meteor.loot.custom_books");
        for (ItemStack item : loot) {
            inv.addItem(item);
        }
        chest.update();
        if (inv.isEmpty()) {
            for (ItemStack item : loot) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), item);
            }
        }
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
        removeMeteorModel();
        pendingTargets.clear();
        if (sender != null) {
            Msg.send(sender, "&e\u0418\u0432\u0435\u043D\u0442 \u0443\u0436\u0435 \u0437\u0430\u043F\u0443\u0449\u0435\u043D.");
        }
        plugin.getEventManager().onEventStopped(getId());
    }

    @Override
    public void status(CommandSender sender) {
        Msg.send(sender, running ? "&a???????? ???????." : "&e???????? ?? ???????.");
    }

    @Override
    public void reload() {
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void spawnMeteorModel(World world, Location center) {
        removeMeteorModel();
        int radius = plugin.getConfig().getInt("meteor.model.radius", 2);
        List<Material> palette = buildMeteorPalette();
        if (palette.isEmpty()) {
            palette.add(Material.MAGMA_BLOCK);
            palette.add(Material.OBSIDIAN);
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > radius + 0.2) {
                        continue;
                    }
                    Location loc = center.clone().add(dx, dy, dz);
                    Material mat = RandUtil.pick(palette);
                    BlockDisplay bd = world.spawn(loc, BlockDisplay.class, d -> d.setBlock(mat.createBlockData()));
                    parts.add(new MeteorPart(bd, new Vector(dx, dy, dz)));
                }
            }
        }
    }

    private void moveMeteorModel(Location center) {
        for (MeteorPart part : parts) {
            if (part.display == null || part.display.isDead()) {
                continue;
            }
            Location loc = center.clone().add(part.offset);
            part.display.teleport(loc);
        }
    }

    private void removeMeteorModel() {
        for (MeteorPart part : parts) {
            if (part.display != null) {
                part.display.remove();
            }
        }
        parts.clear();
    }

    private List<Material> buildMeteorPalette() {
        List<Material> list = new ArrayList<>();
        List<Map<?, ?>> palette = plugin.getConfig().getMapList("meteor.model.palette");
        for (Map<?, ?> raw : palette) {
            String matName = raw.get("material") != null ? raw.get("material").toString() : "MAGMA_BLOCK";
            int weight = 1;
            if (raw.get("weight") != null) {
                try {
                    weight = Integer.parseInt(raw.get("weight").toString());
                } catch (NumberFormatException ignored) {
                }
            }
            Material mat = ConfigUtil.getMaterial(matName, Material.MAGMA_BLOCK);
            for (int i = 0; i < weight; i++) {
                list.add(mat);
            }
        }
        return list;
    }
}




