package me.dominus.faivents.events.ufo;

import me.dominus.faivents.FAIventsPlugin;
import me.dominus.faivents.events.EventManager;
import me.dominus.faivents.util.ConfigUtil;
import me.dominus.faivents.util.Msg;
import me.dominus.faivents.util.RandUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UfoAbductionEvent implements EventManager.EventController {

    private final FAIventsPlugin plugin;
    private boolean running = false;
    private UUID targetId;
    private Location returnLocation;
    private Location arenaCenter;
    private Location saucerCenter;
    private BukkitTask task;
    private BossBar bossBar;
    private final List<UUID> spawned = new ArrayList<>();
    private final Map<Location, Material> saucerBlocks = new HashMap<>();
    private Boolean oldKeepInventory;
    private World keepInventoryWorld;
    private final List<UUID> pendingTargets = new ArrayList<>();
    private CommandSender lastSender;

    public UfoAbductionEvent(FAIventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "ufo";
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("ufo.enabled", true);
    }

    @Override
    public boolean start(CommandSender sender, String[] args) {
        if (running) {
            Msg.send(sender, "&e\u0420\u045F\u0420\u0455\u0421\u2026\u0420\u0451\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5 \u0421\u0453\u0420\u00B6\u0420\u00B5 \u0420\u00B7\u0420\u00B0\u0420\u0457\u0421\u0453\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0455.");
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
                    Msg.send(sender, "&c\u0420\u0098\u0420\u0456\u0421\u0402\u0420\u0455\u0420\u0454 \u0420\u0405\u0420\u00B5 \u0420\u0405\u0420\u00B0\u0420\u2116\u0420\u0491\u0420\u00B5\u0420\u0405.");
                    return false;
                }
                pendingTargets.add(p.getUniqueId());
            }
        } else if (sender instanceof Player) {
            pendingTargets.add(((Player) sender).getUniqueId());
        } else {
            Msg.send(sender, "&c\u0420\u0408\u0420\u0454\u0420\u00B0\u0420\u00B6\u0420\u0451\u0421\u201A\u0420\u00B5 \u0420\u0451\u0420\u0456\u0421\u0402\u0420\u0455\u0420\u0454\u0420\u00B0 \u0420\u0451\u0420\u00BB\u0420\u0451 \u0420\u0406\u0421\u2039\u0420\u0457\u0420\u0455\u0420\u00BB\u0420\u0405\u0420\u0451\u0421\u201A\u0420\u00B5 \u0420\u0454\u0420\u0455\u0420\u0458\u0420\u00B0\u0420\u0405\u0420\u0491\u0421\u0453 \u0420\u0455\u0421\u201A \u0420\u0451\u0420\u0456\u0421\u0402\u0420\u0455\u0420\u0454\u0420\u00B0.");
            return false;
        }

        startNext();
        return true;
    }

    private void startNext() {
        if (pendingTargets.isEmpty()) {
            running = false;
            plugin.getEventManager().onEventStopped(getId());
            return;
        }

        FileConfiguration cfg = plugin.getConfig();
        Player target = Bukkit.getPlayer(pendingTargets.remove(0));
        if (target == null) {
            startNext();
            return;
        }

        World arenaWorld = ConfigUtil.getWorld(cfg, "ufo.arena.world", target.getWorld());
        arenaCenter = ConfigUtil.getLocation(cfg, "ufo.arena", arenaWorld);
        if (arenaWorld == null) {
            Msg.send(lastSender != null ? lastSender : target, "&c\u0420\u045A\u0420\u0451\u0421\u0402 \u0420\u00B0\u0421\u0402\u0420\u00B5\u0420\u0405\u0421\u2039 \u0420\u0405\u0420\u00B5 \u0420\u0405\u0420\u00B0\u0420\u2116\u0420\u0491\u0420\u00B5\u0420\u0405.");
            return;
        }

        running = true;
        targetId = target.getUniqueId();
        returnLocation = target.getLocation().clone();

        int radius = cfg.getInt("ufo.arena.radius", 8);
        double rx = RandUtil.nextInt(-radius, radius) + 0.5;
        double rz = RandUtil.nextInt(-radius, radius) + 0.5;
        Location tp = arenaCenter.clone().add(rx, 0, rz);

        playAbductionEffects(target);

        spawnSaucer(tp);
        Location inside = saucerCenter != null ? saucerCenter.clone().add(0, 1, 0) : tp.clone();
        target.teleport(inside);

        handleTempKeepInventory(target);
        int alienRadius = cfg.getInt("ufo.saucer.alien_radius", 4);
        Location alienBase = saucerCenter != null ? saucerCenter.clone().add(0, 1, 0) : tp.clone();
        spawnAliens(alienBase, alienRadius);

        int duration = cfg.getInt("ufo.duration_seconds", 60);
        startBossBar(target, duration);
        task = new BukkitRunnable() {
            int left = duration;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(targetId);
                if (p == null) {
                    stop(null);
                    return;
                }
                if (p.isDead()) {
                    Msg.send(p, "&c\u0420\u045F\u0420\u0455\u0421\u2026\u0420\u0451\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5 \u0420\u0457\u0421\u0402\u0420\u0455\u0420\u0406\u0420\u00B0\u0420\u00BB\u0420\u00B5\u0420\u0405\u0420\u0455.");
                    stop(null);
                    return;
                }
                if (left <= 0) {
                    rewardAndReturn(p);
                    stop(null);
                    return;
                }
                updateBossBar(left, duration);
                left--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        Msg.send(lastSender != null ? lastSender : target, "&a\u0420\u045F\u0420\u0455\u0421\u2026\u0420\u0451\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5 \u0420\u0405\u0420\u00B0\u0421\u2021\u0420\u00B0\u0420\u00BB\u0420\u0455\u0421\u0403\u0421\u040A: " + target.getName());
    }

    private void playAbductionEffects(Player target) {
        FileConfiguration cfg = plugin.getConfig();
        int blindSec = cfg.getInt("ufo.abduction_effects.blindness_seconds", 3);
        int levSec = cfg.getInt("ufo.abduction_effects.levitation_seconds", 2);
        target.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, blindSec * 20, 0));
        target.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION, levSec * 20, 0));

        String partName = cfg.getString("ufo.beam.particles", "END_ROD");
        Particle particle = Particle.END_ROD;
        try {
            particle = Particle.valueOf(partName);
        } catch (IllegalArgumentException ignored) {
        }
        String soundName = cfg.getString("ufo.beam.sound", "ENTITY_ENDERMAN_TELEPORT");
        Sound sound = Sound.ENTITY_ENDERMAN_TELEPORT;
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException ignored) {
        }
        Location loc = target.getLocation();
        target.getWorld().spawnParticle(particle, loc, 60, 0.5, 2.0, 0.5, 0.01);
        target.getWorld().playSound(loc, sound, 1.2f, 1.0f);
        target.sendTitle(Msg.color("&5\u0420\u045F\u0420\u0455\u0421\u2026\u0420\u0451\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5"), Msg.color("&d\u0420\u201D\u0420\u00B5\u0421\u0402\u0420\u00B6\u0420\u0451\u0421\u0403\u0421\u040A!"), 10, 40, 10);
    }

    private void handleTempKeepInventory(Player target) {
        FileConfiguration cfg = plugin.getConfig();
        boolean enabled = cfg.getBoolean("ufo.temp_keep_inventory.enabled", false);
        if (!enabled) {
            return;
        }
        boolean value = cfg.getBoolean("ufo.temp_keep_inventory.value", true);
        keepInventoryWorld = target.getWorld();
        oldKeepInventory = keepInventoryWorld.getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY);
        keepInventoryWorld.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, value);
    }

    private void restoreKeepInventory() {
        if (keepInventoryWorld != null && oldKeepInventory != null) {
            keepInventoryWorld.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, oldKeepInventory);
        }
        keepInventoryWorld = null;
        oldKeepInventory = null;
    }

    private void spawnSaucer(Location base) {
        removeSaucer();
        FileConfiguration cfg = plugin.getConfig();
        boolean enabled = cfg.getBoolean("ufo.saucer.enabled", true);
        if (!enabled) {
            return;
        }
        int height = cfg.getInt("ufo.saucer.height", 6);
        int radius = cfg.getInt("ufo.saucer.radius", 7);
        int interior = cfg.getInt("ufo.saucer.interior_height", 3);
        Material body = ConfigUtil.getMaterial(cfg.getString("ufo.saucer.material"), Material.IRON_BLOCK);
        Material light = ConfigUtil.getMaterial(cfg.getString("ufo.saucer.light_material"), Material.SEA_LANTERN);
        Material floor = ConfigUtil.getMaterial(cfg.getString("ufo.saucer.floor_material"), Material.SMOOTH_STONE);
        Material wall = ConfigUtil.getMaterial(cfg.getString("ufo.saucer.wall_material"), Material.TINTED_GLASS);

        saucerCenter = base.clone().add(0, height, 0);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius + 0.2) {
                    continue;
                }
                Material mat = (dist > radius - 0.6 && (dx + dz) % 2 == 0) ? light : body;
                setSaucerBlock(saucerCenter.clone().add(dx, 0, dz), mat);
            }
        }

        for (int dx = -(radius - 1); dx <= (radius - 1); dx++) {
            for (int dz = -(radius - 1); dz <= (radius - 1); dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > (radius - 1) + 0.2) {
                    continue;
                }
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    setSaucerBlock(saucerCenter.clone().add(dx, -1, dz), light);
                } else {
                    setSaucerBlock(saucerCenter.clone().add(dx, -1, dz), floor);
                }
            }
        }

        int wallRadius = radius - 1;
        for (int y = 0; y <= interior; y++) {
            for (int dx = -wallRadius; dx <= wallRadius; dx++) {
                for (int dz = -wallRadius; dz <= wallRadius; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > wallRadius + 0.2) {
                        continue;
                    }
                    if (Math.abs(dist - wallRadius) < 0.8) {
                        setSaucerBlock(saucerCenter.clone().add(dx, y, dz), wall);
                    }
                }
            }
        }

        for (int dx = -(radius - 1); dx <= (radius - 1); dx++) {
            for (int dz = -(radius - 1); dz <= (radius - 1); dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > (radius - 1) + 0.2) {
                    continue;
                }
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    setSaucerBlock(saucerCenter.clone().add(dx, interior + 1, dz), light);
                } else {
                    setSaucerBlock(saucerCenter.clone().add(dx, interior + 1, dz), body);
                }
            }
        }

        clearInterior(radius - 2, interior);
    }

    private void clearInterior(int radius, int height) {
        for (int y = 0; y <= height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > radius - 0.2) {
                        continue;
                    }
                    setSaucerBlock(saucerCenter.clone().add(dx, y, dz), Material.AIR);
                }
            }
        }
    }

    private void setSaucerBlock(Location loc, Material mat) {
        if (!saucerBlocks.containsKey(loc)) {
            saucerBlocks.put(loc, loc.getBlock().getType());
        }
        loc.getBlock().setType(mat, false);
    }

    private void removeSaucer() {
        for (Map.Entry<Location, Material> entry : saucerBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue(), false);
        }
        saucerBlocks.clear();
        saucerCenter = null;
    }

    private void spawnAliens(Location center, int radius) {
        FileConfiguration cfg = plugin.getConfig();
        List<Map<?, ?>> list = cfg.getMapList("ufo.aliens");
        for (Map<?, ?> raw : list) {
            String typeName = raw.get("type") != null ? raw.get("type").toString() : "ZOMBIE";
            EntityType type = ConfigUtil.getEntityType(typeName, EntityType.ZOMBIE);
            int count = 1;
            if (raw.get("count") != null) {
                try {
                    count = Integer.parseInt(raw.get("count").toString());
                } catch (NumberFormatException ignored) {
                }
            }
            for (int i = 0; i < count; i++) {
                double rx = RandUtil.nextInt(-radius, radius) + 0.5;
                double rz = RandUtil.nextInt(-radius, radius) + 0.5;
                Location loc = center.clone().add(rx, 0, rz);
                Entity entity = center.getWorld().spawnEntity(loc, type);
                if (entity instanceof LivingEntity le) {
                    String name = raw.get("name") != null ? raw.get("name").toString() : null;
                    if (name != null) {
                        le.setCustomName(Msg.color(name));
                        le.setCustomNameVisible(true);
                    }
                    Object effectsObj = raw.get("effects");
                    if (effectsObj instanceof List) {
                        for (Object e : (List<?>) effectsObj) {
                            PotionEffect effect = ConfigUtil.parseEffect(String.valueOf(e), 60);
                            if (effect != null) {
                                le.addPotionEffect(effect);
                            }
                        }
                    }
                    Object equipmentObj = raw.get("equipment");
                    if (equipmentObj instanceof Map) {
                        applyEquipment(le, (Map<?, ?>) equipmentObj);
                    }
                }
                spawned.add(entity.getUniqueId());
            }
        }
    }

    private void applyEquipment(LivingEntity le, Map<?, ?> equipment) {
        EntityEquipment eq = le.getEquipment();
        if (eq == null) {
            return;
        }
        eq.setItemInMainHand(itemFromMap(equipment, "hand"));
        eq.setHelmet(itemFromMap(equipment, "head"));
        eq.setChestplate(itemFromMap(equipment, "chest"));
        eq.setLeggings(itemFromMap(equipment, "legs"));
        eq.setBoots(itemFromMap(equipment, "feet"));
    }

    private ItemStack itemFromMap(Map<?, ?> equipment, String key) {
        if (!equipment.containsKey(key)) {
            return null;
        }
        Material mat = ConfigUtil.getMaterial(equipment.get(key).toString(), null);
        if (mat == null) {
            return null;
        }
        return new ItemStack(mat, 1);
    }

    private void rewardAndReturn(Player p) {
        List<ItemStack> loot = ConfigUtil.rollLoot(plugin, "ufo.reward.loot");
        if (loot.isEmpty()) {
            loot.add(new ItemStack(Material.IRON_INGOT, 3));
        }
        for (ItemStack item : loot) {
            p.getInventory().addItem(item);
        }
        Msg.send(p, "&a\u0420\u045E\u0421\u2039 \u0420\u0406\u0421\u2039\u0420\u00B6\u0420\u0451\u0420\u00BB \u0420\u0451 \u0420\u0457\u0420\u0455\u0420\u00BB\u0421\u0453\u0421\u2021\u0420\u0451\u0420\u00BB \u0420\u0405\u0420\u00B0\u0420\u0456\u0421\u0402\u0420\u00B0\u0420\u0491\u0421\u0453!");
    }

    private void startBossBar(Player p, int totalSeconds) {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossBar = Bukkit.createBossBar(Msg.color("&d\u0420\u045F\u0420\u0455\u0421\u2026\u0420\u0451\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5: " + formatTime(totalSeconds)), BarColor.PURPLE, BarStyle.SEGMENTED_10);
        bossBar.addPlayer(p);
        bossBar.setProgress(1.0);
    }

    private void updateBossBar(int left, int total) {
        if (bossBar == null) {
            return;
        }
        double progress = Math.max(0.0, Math.min(1.0, (double) left / (double) total));
        bossBar.setProgress(progress);
        bossBar.setTitle(Msg.color("&d\u0420\u045F\u0420\u0455\u0421\u2026\u0420\u0451\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5: " + formatTime(left)));
    }

    private void stopBossBar() {
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
        Player p = targetId != null ? Bukkit.getPlayer(targetId) : null;
        if (p != null && returnLocation != null && !p.isDead()) {
            p.teleport(returnLocation);
        }
        for (UUID id : spawned) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) {
                e.remove();
            }
        }
        spawned.clear();
        removeSaucer();
        restoreKeepInventory();
        stopBossBar();
        if (sender != null) {
            Msg.send(sender, "&e\u0420\u045F\u0420\u0455\u0421\u2026\u0420\u0451\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5 \u0420\u00B7\u0420\u00B0\u0420\u0406\u0420\u00B5\u0421\u0402\u0421\u20AC\u0420\u00B5\u0420\u0405\u0420\u0455.");
            pendingTargets.clear();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, this::startNext);
    }

    @Override
    public void status(CommandSender sender) {
        Msg.send(sender, running ? "&a\u0420\u0098\u0420\u0491\u0420\u00B5\u0421\u201A \u0420\u0457\u0420\u0455\u0421\u2026\u0420\u0451\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5." : "&e\u0420\u045F\u0420\u0455\u0421\u2026\u0420\u0451\u0421\u2030\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5 \u0420\u0405\u0420\u00B5 \u0420\u00B0\u0420\u0454\u0421\u201A\u0420\u0451\u0420\u0406\u0420\u0405\u0420\u0455.");
    }

    @Override
    public void reload() {
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
