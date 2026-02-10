package me.dominus.faivents.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeathDropRuleListener implements Listener {

    private static final double DROP_FRACTION = 0.30;
    private final Map<UUID, Integer> deaths = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        int count = deaths.getOrDefault(player.getUniqueId(), 0) + 1;
        deaths.put(player.getUniqueId(), count);

        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();
        event.setDroppedExp(0);

        if (count % 3 != 0) {
            Msg.send(player, "&a\u0420\u040E\u0420\u0458\u0420\u00B5\u0421\u0402\u0421\u201A\u0421\u040A \u0420\u00B1\u0420\u00B5\u0420\u00B7 \u0420\u0457\u0420\u0455\u0421\u201A\u0420\u00B5\u0421\u0402\u0420\u0451 \u0420\u0406\u0420\u00B5\u0421\u2030\u0420\u00B5\u0420\u2116. \u0420\u045B\u0421\u0403\u0421\u201A\u0420\u00B0\u0420\u00BB\u0420\u0455\u0421\u0403\u0421\u040A \u0420\u0491\u0420\u0455 \u0420\u0457\u0420\u0455\u0421\u201A\u0420\u00B5\u0421\u0402\u0421\u040A: " + (3 - (count % 3)));
            return;
        }

        ItemStack[] contents = player.getInventory().getStorageContents();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir()) {
                indices.add(i);
            }
        }
        if (indices.isEmpty()) {
            Msg.send(player, "&e\u0420\u045E\u0421\u0402\u0420\u00B5\u0421\u201A\u0421\u040A\u0421\u040F \u0421\u0403\u0420\u0458\u0420\u00B5\u0421\u0402\u0421\u201A\u0421\u040A, \u0420\u0405\u0420\u0455 \u0420\u0451\u0420\u0405\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A\u0420\u00B0\u0421\u0402\u0421\u040A \u0420\u0457\u0421\u0453\u0421\u0403\u0421\u201A.");
            return;
        }

        int dropCount = (int) Math.ceil(indices.size() * DROP_FRACTION);
        Collections.shuffle(indices);
        Location loc = player.getLocation();
        for (int i = 0; i < dropCount; i++) {
            int slot = indices.get(i);
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            player.getWorld().dropItemNaturally(loc, item.clone());
            contents[slot] = null;
        }
        player.getInventory().setStorageContents(contents);
        Msg.send(player, "&c\u0420\u045E\u0421\u0402\u0420\u00B5\u0421\u201A\u0421\u040A\u0421\u040F \u0421\u0403\u0420\u0458\u0420\u00B5\u0421\u0402\u0421\u201A\u0421\u040A: \u0420\u0406\u0421\u2039\u0420\u0457\u0420\u00B0\u0420\u00BB\u0420\u0455 30% \u0420\u0451\u0420\u0405\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A\u0420\u00B0\u0421\u0402\u0421\u040F.");
    }
}
