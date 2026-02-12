package me.dominus.faivents.enchant;

import org.bukkit.entity.Enderman;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;

public class PumpkinListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Enderman)) {
            return;
        }
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        if (PumpkinEnchant.has(helmet)) {
            event.setCancelled(true);
        }
    }
}
