package me.dominus.faivents.events.glitch;

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
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RealityGlitchEvent implements EventManager.EventController {

    private final FAIventsPlugin plugin;
    private boolean running = false;
    private BukkitTask task;
    private BukkitTask visualTask;
    private BukkitTask bossTask;
    private BossBar bossBar;
    private final Map<UUID, Collection<PotionEffect>> stored = new HashMap<>();
    private final List<UUID> affected = new ArrayList<>();
    private final List<BlockDisplay> fragments = new ArrayList<>();

    public RealityGlitchEvent(FAIventsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "glitch";
    }

    @Override
    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("reality_glitch.enabled", true);
    }

    @Override
    public boolean start(CommandSender sender, String[] args) {
        int durationMin = plugin.getConfig().getInt("reality_glitch.duration_minutes", 10);
        int durationTicks = durationMin * 60 * 20;
        int durationSeconds = durationMin * 60;
        stored.clear();
        affected.clear();

        List<Player> targets;
        if (args.length >= 3) {
            if ("all".equalsIgnoreCase(args[2])) {
                targets = List.copyOf(Bukkit.getOnlinePlayers());
            } else {
                Player p = Bukkit.getPlayerExact(args[2]);
                if (p == null) {
                    Msg.send(sender, "&c\u0420\u0098\u0420\u0456\u0421\u0402\u0420\u0455\u0420\u0454 \u0420\u0405\u0420\u00B5 \u0420\u0405\u0420\u00B0\u0420\u2116\u0420\u0491\u0420\u00B5\u0420\u0405.");
                    return false;
                }
                targets = List.of(p);
            }
        } else {
            targets = List.copyOf(Bukkit.getOnlinePlayers());
        }

        for (Player p : targets) {
            stored.put(p.getUniqueId(), p.getActivePotionEffects());
            affected.add(p.getUniqueId());
            applyConfiguredEffects(p, durationTicks);
        }

        running = true;
        task = new BukkitRunnable() {
            @Override
            public void run() {
                stop(null);
            }
        }.runTaskLater(plugin, durationTicks);

        startBossBar(targets, durationSeconds);
        startVisuals(targets);

        Msg.send(sender, "&a\u0420\u0098\u0421\u0403\u0420\u0454\u0420\u00B0\u0420\u00B6\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5 \u0421\u0402\u0420\u00B5\u0420\u00B0\u0420\u00BB\u0421\u040A\u0420\u0405\u0420\u0455\u0421\u0403\u0421\u201A\u0420\u0451 \u0420\u0405\u0420\u00B0\u0421\u2021\u0420\u00B0\u0420\u00BB\u0420\u0455\u0421\u0403\u0421\u040A \u0420\u0405\u0420\u00B0 " + durationMin + " \u0420\u0458\u0420\u0451\u0420\u0405.");
        return true;
    }

    private void applyConfiguredEffects(Player p, int durationTicks) {
        List<String> list = plugin.getConfig().getStringList("reality_glitch.effects");
        for (String spec : list) {
            PotionEffect eff = ConfigUtil.parseEffect(spec, durationTicks / 20);
            if (eff != null) {
                p.addPotionEffect(new PotionEffect(eff.getType(), durationTicks, eff.getAmplifier()));
            }
        }
        double chance = plugin.getConfig().getDouble("reality_glitch.random_effect_chance", 0.15);
        if (RandUtil.chance(chance)) {
            List<String> random = plugin.getConfig().getStringList("reality_glitch.random_effects");
            String picked = RandUtil.pick(random);
            PotionEffect eff = ConfigUtil.parseEffect(picked, durationTicks / 20);
            if (eff != null) {
                p.addPotionEffect(new PotionEffect(eff.getType(), durationTicks, eff.getAmplifier()));
            }
        }
    }

    private void startVisuals(List<Player> targets) {
        boolean enabled = plugin.getConfig().getBoolean("reality_glitch.visuals.enabled", true);
        if (!enabled) {
            return;
        }
        String pName = plugin.getConfig().getString("reality_glitch.visuals.particle", "PORTAL");
        Particle particle = Particle.PORTAL;
        try {
            particle = Particle.valueOf(pName);
        } catch (IllegalArgumentException ignored) {
        }
        int count = plugin.getConfig().getInt("reality_glitch.visuals.particle_count", 30);
        double launchChance = plugin.getConfig().getDouble("reality_glitch.visuals.launch_chance", 0.2);
        double launchPower = plugin.getConfig().getDouble("reality_glitch.visuals.launch_power", 0.6);
        double flickerChance = plugin.getConfig().getDouble("reality_glitch.visuals.flicker_chance", 0.25);
        int flickerTicks = plugin.getConfig().getInt("reality_glitch.visuals.flicker_ticks", 10);
        String soundName = plugin.getConfig().getString("reality_glitch.visuals.sound", "ENTITY_ENDERMAN_SCREAM");
        Sound sound = Sound.ENTITY_ENDERMAN_SCREAM;
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException ignored) {
        }
        int interval = plugin.getConfig().getInt("reality_glitch.visuals.interval_ticks", 30);
        final Particle particleFinal = particle;
        final int countFinal = count;
        final double launchChanceFinal = launchChance;
        final double launchPowerFinal = launchPower;
        final double flickerChanceFinal = flickerChance;
        final int flickerTicksFinal = flickerTicks;
        final Sound soundFinal = sound;
        final List<Player> targetsFinal = List.copyOf(targets);

        visualTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : targetsFinal) {
                    if (!p.isOnline()) {
                        continue;
                    }
                    p.getWorld().spawnParticle(particleFinal, p.getLocation().add(0, 1, 0), countFinal, 0.8, 0.8, 0.8, 0.05);
                    if (RandUtil.chance(launchChanceFinal)) {
                        p.setVelocity(p.getVelocity().add(new Vector(0, launchPowerFinal, 0)));
                        p.getWorld().playSound(p.getLocation(), soundFinal, 0.8f, 1.2f);
                    }
                    if (RandUtil.chance(flickerChanceFinal)) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, flickerTicksFinal, 0));
                    }
                    spawnFragments(p);
                }
            }
        }.runTaskTimer(plugin, 0L, Math.max(10, interval));
    }

    private void spawnFragments(Player p) {
        boolean enabled = plugin.getConfig().getBoolean("reality_glitch.visuals.fragments.enabled", true);
        if (!enabled) {
            return;
        }
        int count = plugin.getConfig().getInt("reality_glitch.visuals.fragments.count", 4);
        int radius = plugin.getConfig().getInt("reality_glitch.visuals.fragments.radius", 6);
        int duration = plugin.getConfig().getInt("reality_glitch.visuals.fragments.duration_ticks", 40);
        List<String> mats = plugin.getConfig().getStringList("reality_glitch.visuals.fragments.materials");
        List<Material> palette = new ArrayList<>();
        for (String m : mats) {
            Material mat = ConfigUtil.getMaterial(m, null);
            if (mat != null) {
                palette.add(mat);
            }
        }
        if (palette.isEmpty()) {
            palette.add(Material.NETHERRACK);
            palette.add(Material.END_STONE);
            palette.add(Material.CRIMSON_NYLIUM);
            palette.add(Material.WARPED_NYLIUM);
        }

        for (int i = 0; i < count; i++) {
            int dx = RandUtil.nextInt(-radius, radius);
            int dz = RandUtil.nextInt(-radius, radius);
            int dy = RandUtil.nextInt(0, 3);
            Location loc = p.getLocation().clone().add(dx, dy, dz);
            Material mat = RandUtil.pick(palette);
            BlockDisplay bd = p.getWorld().spawn(loc, BlockDisplay.class, d -> d.setBlock(mat.createBlockData()));
            fragments.add(bd);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                bd.remove();
                fragments.remove(bd);
            }, Math.max(10, duration));
        }
    }

    private void startBossBar(List<Player> targets, int totalSeconds) {
        if (bossBar != null) {
            bossBar.removeAll();
        }
        bossBar = Bukkit.createBossBar(Msg.color("&b\u0420\u0098\u0421\u0403\u0420\u0454\u0420\u00B0\u0420\u00B6\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5: " + formatTime(totalSeconds)), BarColor.BLUE, BarStyle.SEGMENTED_10);
        for (Player p : targets) {
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
                bossBar.setTitle(Msg.color("&b\u0420\u0098\u0421\u0403\u0420\u0454\u0420\u00B0\u0420\u00B6\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5: " + formatTime(left)));
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
        if (visualTask != null) {
            visualTask.cancel();
            visualTask = null;
        }
        stopBossBar();
        for (BlockDisplay bd : new ArrayList<>(fragments)) {
            bd.remove();
        }
        fragments.clear();

        for (UUID id : affected) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) {
                continue;
            }
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            Collection<PotionEffect> original = stored.get(p.getUniqueId());
            if (original != null) {
                for (PotionEffect eff : original) {
                    p.addPotionEffect(new PotionEffect(eff.getType(), eff.getDuration(), eff.getAmplifier(), eff.isAmbient(), eff.hasParticles(), eff.hasIcon()));
                }
            }
        }
        stored.clear();
        affected.clear();
        if (sender != null) {
            Msg.send(sender, "&e\u0420\u0098\u0421\u0403\u0420\u0454\u0420\u00B0\u0420\u00B6\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5 \u0421\u0402\u0420\u00B5\u0420\u00B0\u0420\u00BB\u0421\u040A\u0420\u0405\u0420\u0455\u0421\u0403\u0421\u201A\u0420\u0451 \u0420\u00B7\u0420\u00B0\u0420\u0406\u0420\u00B5\u0421\u0402\u0421\u20AC\u0420\u00B5\u0420\u0405\u0420\u0455.");
        }
        plugin.getEventManager().onEventStopped(getId());
    }

    @Override
    public void status(CommandSender sender) {
        Msg.send(sender, running ? "&a\u0420\u0098\u0421\u0403\u0420\u0454\u0420\u00B0\u0420\u00B6\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5 \u0420\u00B0\u0420\u0454\u0421\u201A\u0420\u0451\u0420\u0406\u0420\u0405\u0420\u0455." : "&e\u0420\u0098\u0421\u0403\u0420\u0454\u0420\u00B0\u0420\u00B6\u0420\u00B5\u0420\u0405\u0420\u0451\u0420\u00B5 \u0420\u0405\u0420\u00B5 \u0420\u00B0\u0420\u0454\u0421\u201A\u0420\u0451\u0420\u0406\u0420\u0405\u0420\u0455.");
    }

    @Override
    public void reload() {
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}