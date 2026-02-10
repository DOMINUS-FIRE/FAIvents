package me.dominus.faivents.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class InvisManager {

    public enum Source {
        COMMAND,
        ASSASSIN
    }

    private final JavaPlugin plugin;
    private final Map<UUID, EnumSet<Source>> sources = new HashMap<>();
    private final Map<UUID, Map<Source, BukkitTask>> timers = new HashMap<>();
    private final Map<UUID, Boolean> prevInvisible = new HashMap<>();
    private BukkitTask refreshTask;

    public InvisManager(JavaPlugin plugin) {
        this.plugin = plugin;
        startRefreshTask();
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        sources.clear();
        timers.clear();
        prevInvisible.clear();
    }

    public boolean isActive(Player player) {
        EnumSet<Source> set = sources.get(player.getUniqueId());
        return set != null && !set.isEmpty();
    }

    public boolean hasSource(Player player, Source source) {
        EnumSet<Source> set = sources.get(player.getUniqueId());
        return set != null && set.contains(source);
    }


    public void addSource(Player player, Source source, long durationTicks) {
        EnumSet<Source> set = sources.computeIfAbsent(player.getUniqueId(), k -> EnumSet.noneOf(Source.class));
        if (set.add(source)) {
            apply(player);
        }
        if (durationTicks > 0) {
            timers.computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(Source.class));
            Map<Source, BukkitTask> map = timers.get(player.getUniqueId());
            BukkitTask old = map.get(source);
            if (old != null) {
                old.cancel();
            }
            map.put(source, Bukkit.getScheduler().runTaskLater(plugin, () -> removeSource(player, source), durationTicks));
        }
    }

    public void removeSource(Player player, Source source) {
        EnumSet<Source> set = sources.get(player.getUniqueId());
        if (set == null) {
            return;
        }
        set.remove(source);
        Map<Source, BukkitTask> map = timers.get(player.getUniqueId());
        if (map != null) {
            BukkitTask t = map.remove(source);
            if (t != null) {
                t.cancel();
            }
        }
        if (set.isEmpty()) {
            sources.remove(player.getUniqueId());
            clear(player);
        }
    }

    private void startRefreshTask() {
        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (isActive(p)) {
                        hideEquipment(p);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void apply(Player player) {
        prevInvisible.putIfAbsent(player.getUniqueId(), player.isInvisible());
        player.setInvisible(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false), true);
        hidePlayer(player, true);
        hideEquipment(player);
    }

    public void refresh(Player player) {
        if (player != null) {
            hideEquipment(player);
        }
    }


    private void clear(Player player) {
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        Boolean prev = prevInvisible.remove(player.getUniqueId());
        if (prev != null) {
            player.setInvisible(prev);
        } else {
            player.setInvisible(false);
        }
        hidePlayer(player, false);
        showEquipment(player);
    }

    public void hideFor(Player target, Player viewer) {
        if (target == null || viewer == null) {
            return;
        }
        if (isActive(target)) {
            viewer.hidePlayer(plugin, target);
        }
    }

    private void hidePlayer(Player target, boolean hide) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            if (hide) {
                viewer.hidePlayer(plugin, target);
            } else {
                viewer.showPlayer(plugin, target);
            }
        }
    }

    private void hideEquipment(Player target) {
        ItemStack air = new ItemStack(Material.AIR);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            viewer.sendEquipmentChange(target, EquipmentSlot.HEAD, air);
            viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, air);
            viewer.sendEquipmentChange(target, EquipmentSlot.LEGS, air);
            viewer.sendEquipmentChange(target, EquipmentSlot.FEET, air);
            viewer.sendEquipmentChange(target, EquipmentSlot.HAND, air);
            viewer.sendEquipmentChange(target, EquipmentSlot.OFF_HAND, air);
        }
    }

    private void showEquipment(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) {
                continue;
            }
            viewer.sendEquipmentChange(target, EquipmentSlot.HEAD, target.getInventory().getHelmet());
            viewer.sendEquipmentChange(target, EquipmentSlot.CHEST, target.getInventory().getChestplate());
            viewer.sendEquipmentChange(target, EquipmentSlot.LEGS, target.getInventory().getLeggings());
            viewer.sendEquipmentChange(target, EquipmentSlot.FEET, target.getInventory().getBoots());
            viewer.sendEquipmentChange(target, EquipmentSlot.HAND, target.getInventory().getItemInMainHand());
            viewer.sendEquipmentChange(target, EquipmentSlot.OFF_HAND, target.getInventory().getItemInOffHand());
        }
    }
}




