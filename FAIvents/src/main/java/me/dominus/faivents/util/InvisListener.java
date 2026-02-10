package me.dominus.faivents.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class InvisListener implements Listener {

    private final JavaPlugin plugin;
    private final InvisManager invisManager;

    public InvisListener(JavaPlugin plugin, InvisManager invisManager) {
        this.plugin = plugin;
        this.invisManager = invisManager;
    }

    private void refreshSoon(Player player) {
        invisManager.refresh(player);
        Bukkit.getScheduler().runTask(plugin, () -> invisManager.refresh(player));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player joined = e.getPlayer();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (invisManager.isActive(p)) {
                invisManager.hideFor(p, joined);
                refreshSoon(p);
            }
        }
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        if (invisManager.isActive(p)) {
            refreshSoon(p);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (invisManager.isActive(p)) {
            refreshSoon(p);
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        if (!invisManager.isActive(p)) {
            return;
        }
        refreshSoon(p);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }
        if (invisManager.isActive(p)) {
            refreshSoon(p);
        }
    }

    @EventHandler
    public void onBreak(PlayerItemBreakEvent e) {
        Player p = e.getPlayer();
        if (invisManager.isActive(p)) {
            refreshSoon(p);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (invisManager.isActive(p)) {
            refreshSoon(p);
        }
    }
}
