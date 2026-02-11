package me.dominus.faivents.events.horror;

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
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HorrorNightEvent implements EventManager.EventController {

    private final FAIventsPlugin plugin;
    private boolean running = false;
    private BukkitTask task;
    private BukkitTask ambienceTask;
    private BukkitTask waveTask;
    private BukkitTask bossTask;
    private BossBar bossBar;
    private World world;
    private long oldTime = 0L;
    private final Map<UUID, MobStats> buffed = new HashMap<>();
    private final Map<UUID, Player> targets = new HashMap<>();

    public HorrorNightEvent(FAIventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "horror";
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("horror_night.enabled", true);
    }

    @Override
    public boolean start(CommandSender sender, String[] args) {
        Player target = null;
        if (args.length >= 3) {
            if ("all".equalsIgnoreCase(args[2])) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    targets.put(p.getUniqueId(), p);
                }
            } else {
                target = Bukkit.getPlayerExact(args[2]);
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        }

        if (targets.isEmpty() && target != null) {
            targets.put(target.getUniqueId(), target);
        }

        if (targets.isEmpty() && !Bukkit.getWorlds().isEmpty()) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                targets.put(p.getUniqueId(), p);
            }
        }

        if (!targets.isEmpty()) {
            world = targets.values().iterator().next().getWorld();
        } else if (!Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }

        if (world == null) {
            Msg.send(sender, "&c\u041C\u0438\u0440 \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D.");
            return false;
        }

        oldTime = world.getTime();
        world.setTime(18000L);

        int durationMin = plugin.getConfig().getInt("horror_night.duration_minutes", 8);
        int durationSeconds = durationMin * 60;
        int durationTicks = durationSeconds * 20;

        int waveInterval = plugin.getConfig().getInt("horror_night.wave_interval_seconds", 60);
        waveTask = new BukkitRunnable() {
            @Override
            public void run() {
                spawnExtraMobs();
            }
        }.runTaskTimer(plugin, 0L, Math.max(20, waveInterval * 20L));

        task = new BukkitRunnable() {
            int left = durationTicks;

            @Override
            public void run() {
                if (left <= 0) {
                    stop(null);
                    return;
                }
                world.setTime(18000L);
                buffNearbyMobs();
                left -= 40;
            }
        }.runTaskTimer(plugin, 0L, 40L);

        ambienceTask = new BukkitRunnable() {
            @Override
            public void run() {
                playAmbience();
            }
        }.runTaskTimer(plugin, 0L, 60L);

        startBossBar(durationSeconds);

        running = true;
        Msg.send(sender, "&a\u041D\u043E\u0447\u044C \u0443\u0436\u0430\u0441\u0430 \u043D\u0430\u0447\u0430\u043B\u0430\u0441\u044C \u043D\u0430 " + durationMin + " \u043C\u0438\u043D.");
        return true;
    }

    private void spawnExtraMobs() {
        List<Map<?, ?>> list = plugin.getConfig().getMapList("horror_night.extra_spawns");
        for (Player p : targets.values()) {
            for (Map<?, ?> raw : list) {
                String typeName = raw.get("type") != null ? raw.get("type").toString() : "ZOMBIE";
                int count = 1;
                if (raw.get("count") != null) {
                    try {
                        count = Integer.parseInt(raw.get("count").toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
                EntityType type = ConfigUtil.getEntityType(typeName, EntityType.ZOMBIE);
                for (int i = 0; i < count; i++) {
                    Location loc = p.getLocation().clone().add(RandUtil.nextInt(-6, 6), 0, RandUtil.nextInt(-6, 6));
                    if (loc.getWorld().spawnEntity(loc, type) instanceof LivingEntity le) {
                        Object effectsObj = raw.get("effects");
                        if (effectsObj instanceof List) {
                            for (Object e : (List<?>) effectsObj) {
                                PotionEffect effect = ConfigUtil.parseEffect(String.valueOf(e), 300);
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
                }
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
        Material mat = ConfigUtil.getMaterial(equipment.get(key).toString(), Material.AIR);
        if (mat == Material.AIR) {
            return null;
        }
        return new ItemStack(mat, 1);
    }

    private void buffNearbyMobs() {
        double radius = plugin.getConfig().getDouble("horror_night.mob_buff.radius", 24.0);
        double hpMul = plugin.getConfig().getDouble("horror_night.mob_buff.health_multiplier", 1.5);
        double dmgMul = plugin.getConfig().getDouble("horror_night.mob_buff.damage_multiplier", 1.4);

        for (Player p : targets.values()) {
            for (org.bukkit.entity.Entity e : p.getWorld().getNearbyEntities(p.getLocation(), radius, radius, radius)) {
                if (!(e instanceof LivingEntity)) {
                    continue;
                }
                LivingEntity le = (LivingEntity) e;
                if (le instanceof Player) {
                    continue;
                }
                if (buffed.containsKey(le.getUniqueId())) {
                    continue;
                }
                double baseHp = le.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                        ? le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue() : 0;
                double baseDmg = le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null
                        ? le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue() : 0;
                buffed.put(le.getUniqueId(), new MobStats(baseHp, baseDmg));
                if (le.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    le.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(baseHp * hpMul);
                    le.setHealth(Math.min(le.getHealth() * hpMul, baseHp * hpMul));
                }
                if (le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                    le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(baseDmg * dmgMul);
                }
            }
        }
    }

    private void playAmbience() {
        String particleName = plugin.getConfig().getString("horror_night.ambience.particle", "SMOKE_LARGE");
        String soundName = plugin.getConfig().getString("horror_night.ambience.sound", "AMBIENT_CAVE");
        Particle particle = Particle.SMOKE_LARGE;
        Sound sound = Sound.AMBIENT_CAVE;
        try {
            particle = Particle.valueOf(particleName);
        } catch (IllegalArgumentException ignored) {
        }
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException ignored) {
        }
        for (Player p : targets.values()) {
            Location loc = p.getLocation();
            p.getWorld().spawnParticle(particle, loc, 20, 1.5, 1.0, 1.5, 0.01);
            p.getWorld().playSound(loc, sound, 0.6f, 0.9f);
        }
    }

    private void startBossBar(int totalSeconds) {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossBar = Bukkit.createBossBar(Msg.color("&4\u041D\u043E\u0447\u044C \u0443\u0436\u0430\u0441\u0430: " + formatTime(totalSeconds)), BarColor.RED, BarStyle.SEGMENTED_10);
        for (Player p : targets.values()) {
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
                bossBar.setTitle(Msg.color("&4\u041D\u043E\u0447\u044C \u0443\u0436\u0430\u0441\u0430: " + formatTime(left)));
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
        if (ambienceTask != null) {
            ambienceTask.cancel();
            ambienceTask = null;
        }
        if (waveTask != null) {
            waveTask.cancel();
            waveTask = null;
        }
        stopBossBar();
        if (world != null) {
            world.setTime(oldTime);
        }
        restoreMobs();
        targets.clear();
        if (sender != null) {
            Msg.send(sender, "&e\u041D\u043E\u0447\u044C \u0443\u0436\u0430\u0441\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043D\u0430.");
        }
        plugin.getEventManager().onEventStopped(getId());
    }

    private void restoreMobs() {
        for (UUID id : buffed.keySet()) {
            if (!(Bukkit.getEntity(id) instanceof LivingEntity)) {
                continue;
            }
            LivingEntity le = (LivingEntity) Bukkit.getEntity(id);
            MobStats stats = buffed.get(id);
            if (le != null && stats != null) {
                if (le.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    le.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(stats.maxHealth);
                    le.setHealth(Math.min(le.getHealth(), stats.maxHealth));
                }
                if (le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                    le.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(stats.attackDamage);
                }
            }
        }
        buffed.clear();
    }

    @Override
    public void status(CommandSender sender) {
        Msg.send(sender, running ? "&a\u041D\u043E\u0447\u044C \u0443\u0436\u0430\u0441\u0430 \u0430\u043A\u0442\u0438\u0432\u043D\u0430." : "&e\u041D\u043E\u0447\u044C \u0443\u0436\u0430\u0441\u0430 \u043D\u0435 \u0430\u043A\u0442\u0438\u0432\u043D\u0430.");
    }

    @Override
    public void reload() {
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private static class MobStats {
        final double maxHealth;
        final double attackDamage;

        MobStats(double maxHealth, double attackDamage) {
            this.maxHealth = maxHealth;
            this.attackDamage = attackDamage;
        }
    }
}