package me.dominus.faivents.enchant;

import me.dominus.faivents.FAIventsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class FarmerListener implements Listener {

    private final FAIventsPlugin plugin;

    public FarmerListener(FAIventsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!FarmerEnchant.has(tool)) {
            return;
        }
        Block base = player.getLocation().getBlock();
        int y = base.getY();
        boolean applied = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block b = base.getWorld().getBlockAt(base.getX() + dx, y, base.getZ() + dz);
                applied |= applyBonemeal(b);
                Block up = b.getRelative(0, 1, 0);
                applied |= applyBonemeal(up);
            }
        }
        if (applied) {
            damageHoe(player, tool, 2);
        }
    }

    private boolean applyBonemeal(Block block) {
        if (block.getBlockData() instanceof Ageable age) {
            if (age.getAge() < age.getMaximumAge()) {
                age.setAge(age.getMaximumAge());
                block.setBlockData(age, false);
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!FarmerEnchant.has(tool)) {
            return;
        }
        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() < ageable.getMaximumAge()) {
            return;
        }
        Material seed = seedFor(block.getType());
        if (seed == null) {
            return;
        }
        if (!removeOne(player, seed)) {
            return;
        }
        Material cropType = block.getType();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (block.getType() == Material.AIR) {
                block.setType(cropType, false);
                if (block.getBlockData() instanceof Ageable a) {
                    a.setAge(0);
                    block.setBlockData(a, false);
                }
            }
        }, 1L);
    }

    private Material seedFor(Material crop) {
        switch (crop) {
            case WHEAT:
                return Material.WHEAT_SEEDS;
            case CARROTS:
                return Material.CARROT;
            case POTATOES:
                return Material.POTATO;
            case BEETROOTS:
                return Material.BEETROOT_SEEDS;
            case NETHER_WART:
                return Material.NETHER_WART;
            default:
                return null;
        }
    }

    private boolean removeOne(Player player, Material seed) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && it.getType() == seed && it.getAmount() > 0) {
                it.setAmount(it.getAmount() - 1);
                if (it.getAmount() <= 0) {
                    contents[i] = null;
                }
                player.getInventory().setContents(contents);
                return true;
            }
        }
        return false;
    }

    private void damageHoe(Player player, ItemStack tool, int amount) {
        if (tool == null || amount <= 0) {
            return;
        }
        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        int max = tool.getType().getMaxDurability();
        if (max <= 0) {
            return;
        }
        int newDamage = damageable.getDamage() + amount;
        if (newDamage >= max) {
            tool.setAmount(0);
            player.getInventory().setItemInMainHand(null);
            return;
        }
        damageable.setDamage(newDamage);
        tool.setItemMeta(meta);
        player.getInventory().setItemInMainHand(tool);
    }
}
