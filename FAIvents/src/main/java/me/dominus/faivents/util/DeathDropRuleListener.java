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
            Msg.send(player, "&a\u0421\u043C\u0435\u0440\u0442\u044C \u0431\u0435\u0437 \u043F\u043E\u0442\u0435\u0440\u0438 \u0432\u0435\u0449\u0435\u0439. \u041E\u0441\u0442\u0430\u043B\u043E\u0441\u044C \u0434\u043E \u043F\u043E\u0442\u0435\u0440\u044C: " + (3 - (count % 3)));
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
            Msg.send(player, "&e\u0422\u0440\u0435\u0442\u044C\u044F \u0441\u043C\u0435\u0440\u0442\u044C, \u043D\u043E \u0438\u043D\u0432\u0435\u043D\u0442\u0430\u0440\u044C \u043F\u0443\u0441\u0442.");
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
        Msg.send(player, "&c\u0422\u0440\u0435\u0442\u044C\u044F \u0441\u043C\u0435\u0440\u0442\u044C: \u0432\u044B\u043F\u0430\u043B\u043E 30% \u0438\u043D\u0432\u0435\u043D\u0442\u0430\u0440\u044F.");
    }
}
