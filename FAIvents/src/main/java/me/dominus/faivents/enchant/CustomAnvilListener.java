package me.dominus.faivents.enchant;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomAnvilListener implements Listener {

    private static final int COST_LEVELS = 6;

    @EventHandler
    public void onPrepare(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || right == null) {
            return;
        }

        ItemStack book = null;
        ItemStack target = null;
        if (isBook(left)) {
            book = left;
            target = right;
        } else if (isBook(right)) {
            book = right;
            target = left;
        }
        if (book == null || target == null) {
            return;
        }

        List<Enchantment> custom = getCustomEnchantments(book);
        if (custom.isEmpty()) {
            return;
        }

        ItemStack result = target.clone();
        boolean changed = false;
        for (Enchantment ench : custom) {
            if (ench == null) {
                continue;
            }
            if (!canApply(ench, result)) {
                continue;
            }
            if (!result.containsEnchantment(ench)) {
                result.addUnsafeEnchantment(ench, 1);
                addLoreFor(ench, result);
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            if (result.containsEnchantment(BoomLeggingsEnchant.get())) {
                meta.setUnbreakable(true);
            }
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            result.setItemMeta(meta);
        }
        event.setResult(result);
        inv.setRepairCost(COST_LEVELS);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory inv)) {
            return;
        }
        if (event.getRawSlot() != 2) {
            return;
        }
        ItemStack result = inv.getItem(2);
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);
        if (left == null || right == null) {
            return;
        }

        ItemStack book = null;
        ItemStack target = null;
        if (isBook(left)) {
            book = left;
            target = right;
        } else if (isBook(right)) {
            book = right;
            target = left;
        }
        if (book == null || target == null) {
            return;
        }
        if (getCustomEnchantments(book).isEmpty()) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE && player.getLevel() < COST_LEVELS) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.setLevel(player.getLevel() - COST_LEVELS);
        }

        ItemStack give = result.clone();
        if (event.isShiftClick()) {
            Map<Integer, ItemStack> leftItems = player.getInventory().addItem(give);
            for (ItemStack l : leftItems.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), l);
            }
        } else {
            ItemStack cursor = event.getCursor();
            if (cursor == null || cursor.getType() == Material.AIR) {
                event.setCursor(give);
            } else {
                Map<Integer, ItemStack> leftItems = player.getInventory().addItem(give);
                for (ItemStack l : leftItems.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), l);
                }
            }
        }

        inv.setItem(0, null);
        inv.setItem(1, null);
        inv.setItem(2, null);
        inv.setRepairCost(0);
    }

    private boolean isBook(ItemStack item) {
        return item.getType() == Material.ENCHANTED_BOOK;
    }

    private List<Enchantment> getCustomEnchantments(ItemStack book) {
        List<Enchantment> out = new ArrayList<>();
        if (!(book.getItemMeta() instanceof EnchantmentStorageMeta meta)) {
            return out;
        }
        for (Map.Entry<Enchantment, Integer> e : meta.getStoredEnchants().entrySet()) {
            Enchantment ench = e.getKey();
            if (matches(ench)) {
                out.add(ench);
            }
        }
        return out;
    }

    private boolean matches(Enchantment ench) {
        if (ench == null) {
            return false;
        }
        return isCustomKey(ench, DrillEnchant.getKeyStatic())
                || isCustomKey(ench, MagnetEnchant.getKeyStatic())
                || isCustomKey(ench, AutoSmeltEnchant.getKeyStatic())
                || isCustomKey(ench, FarmerEnchant.getKeyStatic())
                || isCustomKey(ench, LumberjackEnchant.getKeyStatic())
                || isCustomKey(ench, SecondLifeEnchant.getKeyStatic())
                || isCustomKey(ench, AssassinEnchant.getKeyStatic())
                || isCustomKey(ench, HornEnchant.getKeyStatic())
                || isCustomKey(ench, ShellEnchant.getKeyStatic())
                || isCustomKey(ench, BoomLeggingsEnchant.getKeyStatic());
    }

    private boolean isCustomKey(Enchantment ench, org.bukkit.NamespacedKey key) {
        return key != null && key.equals(ench.getKey());
    }

    private boolean canApply(Enchantment ench, ItemStack target) {
        return ench.canEnchantItem(target);
    }

    private void addLoreFor(Enchantment ench, ItemStack item) {
        if (isCustomKey(ench, DrillEnchant.getKeyStatic())) {
            DrillEnchant.addLore(item);
        } else if (isCustomKey(ench, MagnetEnchant.getKeyStatic())) {
            MagnetEnchant.addLore(item);
        } else if (isCustomKey(ench, AutoSmeltEnchant.getKeyStatic())) {
            AutoSmeltEnchant.addLore(item);
        } else if (isCustomKey(ench, FarmerEnchant.getKeyStatic())) {
            FarmerEnchant.addLore(item);
        } else if (isCustomKey(ench, LumberjackEnchant.getKeyStatic())) {
            LumberjackEnchant.addLore(item);
        } else if (isCustomKey(ench, SecondLifeEnchant.getKeyStatic())) {
            SecondLifeEnchant.addLore(item);
        } else if (isCustomKey(ench, AssassinEnchant.getKeyStatic())) {
            AssassinEnchant.addLore(item);
        } else if (isCustomKey(ench, HornEnchant.getKeyStatic())) {
            HornEnchant.addLore(item);
        } else if (isCustomKey(ench, ShellEnchant.getKeyStatic())) {
            ShellEnchant.addLore(item);
        } else if (isCustomKey(ench, BoomLeggingsEnchant.getKeyStatic())) {
            BoomLeggingsEnchant.addLore(item);
        }
    }
}
