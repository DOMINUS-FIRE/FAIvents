package me.dominus.faivents.enchant;

import me.dominus.faivents.util.Msg;
import me.dominus.faivents.util.SafeWorldEdit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SecondLifeListener implements Listener {

    private static final long COOLDOWN_MS = 5 * 60 * 1000L;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        double finalDamage = event.getFinalDamage();
        if (player.getHealth() - finalDamage > 0) {
            return;
        }
        ItemStack chest = player.getInventory().getChestplate();
        if (!SecondLifeEnchant.has(chest)) {
            return;
        }
        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now + COOLDOWN_MS);
        event.setCancelled(true);

        double newHealth = Math.min(player.getMaxHealth(), 6.0);
        player.setHealth(newHealth);
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 3.0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, true, true), true);
        player.getWorld().spawnParticle(Particle.TOTEM, player.getLocation().add(0, 1.0, 0), 80, 0.6, 0.8, 0.6, 0.1);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

        Location loc = player.getLocation();
        Location safe = null;
        for (int i = 0; i < 10; i++) {
            double dx = (Math.random() * 20) - 10;
            double dz = (Math.random() * 20) - 10;
            Location candidate = loc.clone().add(dx, 0, dz);
            safe = SafeWorldEdit.findNearestAir(candidate, 2, 2);
            if (safe != null) {
                break;
            }
        }
        if (safe == null) {
            safe = loc;
        }
        player.teleport(safe);
        Msg.send(player, "&a\u0412\u0442\u043E\u0440\u0430\u044F \u0436\u0438\u0437\u043D\u044C \u0441\u0440\u0430\u0431\u043E\u0442\u0430\u043B\u0430!");
    }
}
