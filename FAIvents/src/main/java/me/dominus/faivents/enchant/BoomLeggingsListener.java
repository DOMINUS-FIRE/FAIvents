package me.dominus.faivents.enchant;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class BoomLeggingsListener implements Listener {

    private final NamespacedKey arrowKey;

    public BoomLeggingsListener(JavaPlugin plugin) {
        this.arrowKey = new NamespacedKey(plugin, "boom_arrow");
    }

    @EventHandler
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        ItemStack bow = event.getBow();
        if (!BoomLeggingsEnchant.has(bow)) {
            return;
        }
        if (!(event.getProjectile() instanceof Projectile proj)) {
            return;
        }
        proj.getPersistentDataContainer().set(arrowKey, PersistentDataType.BYTE, (byte) 1);
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!arrow.getPersistentDataContainer().has(arrowKey, PersistentDataType.BYTE)) {
            return;
        }
        arrow.getWorld().createExplosion(arrow.getLocation(), 4.0f, false, false);
        arrow.remove();
    }
}
