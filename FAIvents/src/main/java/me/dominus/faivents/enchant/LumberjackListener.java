package me.dominus.faivents.enchant;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class LumberjackListener implements Listener {

    private static final int MAX_BLOCKS = 256;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!LumberjackEnchant.has(tool)) {
            return;
        }
        if (!tool.getType().name().endsWith("_AXE")) {
            return;
        }
        Block start = event.getBlock();
        if (!Tag.LOGS.isTagged(start.getType())) {
            return;
        }

        event.setCancelled(true);
        Set<Block> visited = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        int count = 0;
        while (!queue.isEmpty() && count < MAX_BLOCKS) {
            Block b = queue.poll();
            if (!Tag.LOGS.isTagged(b.getType())) {
                continue;
            }
            if (!CustomEnchantDrops.handleCustomDrops(b, player, tool, true)) {
                b.breakNaturally(tool);
            }
            count++;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block nb = b.getRelative(dx, dy, dz);
                        if (visited.add(nb) && Tag.LOGS.isTagged(nb.getType())) {
                            queue.add(nb);
                        }
                    }
                }
            }
        }
    }
}
