package me.dominus.faivents.enchant;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

public class UnbreakableListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(PlayerItemDamageEvent event) {
        ItemStack item = event.getItem();
        if (UnbreakableEnchant.has(item)) {
            event.setCancelled(true);
        }
    }
}
