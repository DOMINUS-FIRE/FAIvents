package me.dominus.faivents.enchant;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CustomEnchantDrops {

    private CustomEnchantDrops() {
    }

    public static boolean handleCustomDrops(Block block, Player player, ItemStack tool, boolean allowBreak) {
        boolean magnet = MagnetEnchant.has(tool);
        boolean smelt = AutoSmeltEnchant.has(tool);
        if (!magnet && !smelt) {
            return false;
        }

        if (smelt && !isSmeltTool(tool)) {
            smelt = false;
        }
        if (magnet && !isMagnetTool(tool)) {
            magnet = false;
        }
        if (!magnet && !smelt) {
            return false;
        }
        if (!isBreakable(block.getType())) {
            return false;
        }

        List<ItemStack> drops = new ArrayList<>(block.getDrops(tool, player));
        if (drops.isEmpty()) {
            return true;
        }
        if (smelt) {
            List<ItemStack> cooked = new ArrayList<>();
            for (ItemStack drop : drops) {
                cooked.add(smeltResult(drop));
            }
            drops = cooked;
        }

        if (magnet) {
            for (ItemStack item : drops) {
                Map<Integer, ItemStack> left = player.getInventory().addItem(item);
                for (ItemStack l : left.values()) {
                    player.getWorld().dropItemNaturally(block.getLocation(), l);
                }
            }
        } else {
            for (ItemStack item : drops) {
                player.getWorld().dropItemNaturally(block.getLocation(), item);
            }
        }

        if (allowBreak) {
            block.setType(Material.AIR, false);
            damageTool(tool, player, drops.size());
        }
        return true;
    }

    private static boolean isMagnetTool(ItemStack tool) {
        if (tool == null) {
            return false;
        }
        String name = tool.getType().name();
        return name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_AXE");
    }

    private static boolean isSmeltTool(ItemStack tool) {
        if (tool == null) {
            return false;
        }
        String name = tool.getType().name();
        return name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL");
    }

    private static boolean isBreakable(Material type) {
        if (type.isAir() || type == Material.BEDROCK || type == Material.BARRIER) {
            return false;
        }
        if (type == Material.END_PORTAL || type == Material.END_PORTAL_FRAME || type == Material.NETHER_PORTAL) {
            return false;
        }
        return type.isBlock();
    }

    private static ItemStack smeltResult(ItemStack in) {
        if (in == null) {
            return in;
        }
        ItemStack direct = directSmelt(in);
        if (direct != null) {
            return direct;
        }
        for (Recipe r : Bukkit.getRecipesFor(in)) {
            if (r instanceof CookingRecipe<?> recipe) {
                ItemStack result = recipe.getResult().clone();
                result.setAmount(in.getAmount());
                return result;
            }
        }
        return in;
    }

    private static ItemStack directSmelt(ItemStack in) {
        Material type = in.getType();
        Material result = null;
        if (type == Material.COBBLESTONE) {
            result = Material.STONE;
        } else if ("DEEPSLATE_COBBLESTONE".equals(type.name())) {
            result = Material.DEEPSLATE;
        } else if (type == Material.SAND || type == Material.RED_SAND) {
            result = Material.GLASS;
        } else if (type == Material.CLAY_BALL) {
            result = Material.TERRACOTTA;
        } else if (type == Material.NETHERRACK) {
            result = Material.NETHER_BRICK;
        } else if (type == Material.RAW_IRON) {
            result = Material.IRON_INGOT;
        } else if (type == Material.RAW_GOLD) {
            result = Material.GOLD_INGOT;
        } else if (type == Material.RAW_COPPER) {
            result = Material.COPPER_INGOT;
        } else if (type == Material.ANCIENT_DEBRIS) {
            result = Material.NETHERITE_SCRAP;
        }
        if (result == null) {
            return null;
        }
        ItemStack out = new ItemStack(result, in.getAmount());
        return out;
    }

    private static void damageTool(ItemStack tool, Player player, int count) {
        if (!(tool.getItemMeta() instanceof Damageable meta)) {
            return;
        }
        int max = tool.getType().getMaxDurability();
        if (max <= 0) {
            return;
        }
        meta.setDamage(meta.getDamage() + Math.max(1, count));
        tool.setItemMeta(meta);
        if (meta.getDamage() >= max) {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
