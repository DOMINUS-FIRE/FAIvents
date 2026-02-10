package me.dominus.faivents.enchant;

import me.dominus.faivents.FAIventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DrillListener implements Listener {

    private final FAIventsPlugin plugin;
    private final Set<UUID> breaking = new HashSet<>();

    public DrillListener(FAIventsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || !DrillEnchant.has(tool)) {
            return;
        }
        if (!DrillEnchant.isValidTool(tool)) {
            return;
        }
        if (player.isSneaking()) {
            return;
        }
        UUID id = player.getUniqueId();
        if (!breaking.add(id)) {
            return;
        }
        try {
            breakArea(event.getBlock(), player, tool);
        } finally {
            breaking.remove(id);
        }
    }

    private void breakArea(Block origin, Player player, ItemStack tool) {
        Vector dir = player.getLocation().getDirection();
        double ax = Math.abs(dir.getX());
        double ay = Math.abs(dir.getY());
        double az = Math.abs(dir.getZ());

        int[][] offsets;
        if (ay >= ax && ay >= az) {
            offsets = planeXZ();
        } else if (ax >= az) {
            offsets = planeYZ();
        } else {
            offsets = planeXY();
        }

        for (int[] o : offsets) {
            Block b = origin.getRelative(o[0], o[1], o[2]);
            if (b.equals(origin)) {
                continue;
            }
            if (!isBreakable(b)) {
                continue;
            }
            BlockBreakEvent ev = new BlockBreakEvent(b, player);
            Bukkit.getPluginManager().callEvent(ev);
            if (ev.isCancelled()) {
                continue;
            }
            if (CustomEnchantDrops.handleCustomDrops(b, player, tool, true)) {
                continue;
            }
            if (!ev.isDropItems()) {
                b.setType(Material.AIR, false);
            } else {
                b.breakNaturally(tool);
            }
        }
    }

    private boolean isBreakable(Block b) {
        Material type = b.getType();
        if (type.isAir() || type == Material.BEDROCK || type == Material.BARRIER) {
            return false;
        }
        if (type == Material.END_PORTAL || type == Material.END_PORTAL_FRAME || type == Material.NETHER_PORTAL) {
            return false;
        }
        return type.isBlock();
    }

    private int[][] planeXZ() {
        int[][] o = new int[9][3];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                o[i++] = new int[]{dx, 0, dz};
            }
        }
        return o;
    }

    private int[][] planeYZ() {
        int[][] o = new int[9][3];
        int i = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                o[i++] = new int[]{0, dy, dz};
            }
        }
        return o;
    }

    private int[][] planeXY() {
        int[][] o = new int[9][3];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                o[i++] = new int[]{dx, dy, 0};
            }
        }
        return o;
    }
}

