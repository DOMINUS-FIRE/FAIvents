package me.dominus.faivents.enchant;

import me.dominus.faivents.FAIventsPlugin;
import me.dominus.faivents.util.InvisManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AssassinListener implements Listener {

    private static final long COOLDOWN_MS = 60 * 1000L;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final InvisManager invisManager;

    public AssassinListener(FAIventsPlugin plugin, InvisManager invisManager) {
        this.invisManager = invisManager;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    ItemStack helmet = p.getInventory().getHelmet();
                    if (AssassinEnchant.has(helmet)) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0, true, false, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 100L);
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack helmet = player.getInventory().getHelmet();
        if (!AssassinEnchant.has(helmet)) {
            return;
        }
        long now = System.currentTimeMillis();
        long readyAt = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now < readyAt) {
            return;
        }
        cooldowns.put(player.getUniqueId(), now + COOLDOWN_MS);
        invisManager.addSource(player, InvisManager.Source.ASSASSIN, 200L);
    }
}
