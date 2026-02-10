package me.dominus.faivents.enchant;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ShellListener implements Listener {

    private static final long COOLDOWN_MS = 2 * 60 * 1000L;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> active = new HashSet<>();

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack helmet = player.getInventory().getHelmet();
        if (!ShellEnchant.has(helmet)) {
            return;
        }
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (event.isSneaking()) {
            long readyAt = cooldowns.getOrDefault(id, 0L);
            if (now < readyAt) {
                return;
            }
            active.add(id);
            player.setInvulnerable(true);
        } else {
            if (active.remove(id)) {
                player.setInvulnerable(false);
                cooldowns.put(id, now + COOLDOWN_MS);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (active.contains(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (active.remove(id)) {
            event.getPlayer().setInvulnerable(false);
        }
    }
}
