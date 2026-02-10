package me.dominus.faivents.enchant;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HornListener implements Listener {

    private static final long COOLDOWN_MS = 3 * 60 * 1000L;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getItem() == null) {
            return;
        }
        ItemStack item = event.getItem();
        if (item.getType() != Material.GOAT_HORN) {
            return;
        }
        if (!HornEnchant.has(item)) {
            return;
        }
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now + COOLDOWN_MS);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 400, 1, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 3, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 6000, 0, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 6000, 0, true, false, false));
        player.setCooldown(Material.GOAT_HORN, 3600);
    }
}

