package me.dominus.faivents.command;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.dominus.faivents.enchant.AssassinEnchant;
import me.dominus.faivents.enchant.AutoSmeltEnchant;
import me.dominus.faivents.enchant.BoomLeggingsEnchant;
import me.dominus.faivents.enchant.DrillEnchant;
import me.dominus.faivents.enchant.FarmerEnchant;
import me.dominus.faivents.enchant.HornEnchant;
import me.dominus.faivents.enchant.LumberjackEnchant;
import me.dominus.faivents.enchant.MagnetEnchant;
import me.dominus.faivents.enchant.SecondLifeEnchant;
import me.dominus.faivents.enchant.ShellEnchant;
import me.dominus.faivents.util.Msg;

public class SecretCommand implements CommandExecutor, Listener {

    private static final String TITLE_MAIN = ChatColor.DARK_GRAY + "\u0421\u0435\u043A\u0440\u0435\u0442\u043D\u043E\u0435 \u043C\u0435\u043D\u044E";
    private static final String TITLE_MODES = ChatColor.DARK_AQUA + "\u0420\u0435\u0436\u0438\u043C\u044B";
    private static final String TITLE_COMMANDS = ChatColor.DARK_AQUA + "\u041A\u043E\u043C\u0430\u043D\u0434\u044B";
    private static final String TITLE_ENCHANTS = ChatColor.DARK_AQUA + "\u0417\u0430\u0447\u0430\u0440\u043E\u0432\u0430\u043D\u0438\u044F";
    private static final String TITLE_TRACK = ChatColor.DARK_AQUA + "\u041D\u0430\u0431\u043B\u044E\u0434\u0435\u043D\u0438\u0435";
    private static final String TITLE_MOBS_BASE = ChatColor.DARK_AQUA + "\u041C\u043E\u0431\u044B";

    private static final int MOBS_PAGE_SIZE = 45;
    private static final List<MobEntry> MOB_ENTRIES = buildMobEntries();

    private final JavaPlugin plugin;
    private final Map<Integer, Action> mainActions = new HashMap<>();
    private final Map<Integer, ModeAction> modeActions = new HashMap<>();
    private final Map<Integer, CommandAction> commandActions = new HashMap<>();
    private final Map<Integer, EnchantAction> enchantActions = new HashMap<>();

    private final Map<UUID, FollowState> follow = new HashMap<>();
    private final Map<UUID, DisguiseState> disguises = new HashMap<>();

    public SecretCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private enum Action {
        OPEN_MODES,
        OPEN_COMMANDS,
        OPEN_ENCHANTS,
        OPEN_TRACK,
        OPEN_MOBS
    }

    private enum ModeAction {
        CREATIVE,
        SURVIVAL,
        SPECTATOR,
        BACK
    }

    private enum CommandAction {
        DISABLE_FEEDBACK,
        ENABLE_FEEDBACK,
        OP_SELF,
        BACK
    }

    private enum EnchantAction {
        DRILL,
        MAGNET,
        SMELT,
        FARMER,
        LUMBERJACK,
        SECOND_LIFE,
        ASSASSIN,
        HORN,
        SHELL,
        BOOM_LEGS,
        BACK
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Msg.send(sender, "&c\u041A\u043E\u043C\u0430\u043D\u0434\u0430 \u0442\u043E\u043B\u044C\u043A\u043E \u0434\u043B\u044F \u0438\u0433\u0440\u043E\u043A\u043E\u0432.");
            return true;
        }
        Player player = (Player) sender;
        player.openInventory(buildMainMenu());
        return true;
    }

    private Inventory buildMainMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MAIN);
        inv.setItem(10, item(Material.DIAMOND_SWORD, "&b\u0420\u0435\u0436\u0438\u043C\u044B"));
        inv.setItem(12, item(Material.COMMAND_BLOCK, "&e\u041A\u043E\u043C\u0430\u043D\u0434\u044B"));
        inv.setItem(14, item(Material.ENCHANTED_BOOK, "&6\u0417\u0430\u0447\u0430\u0440\u043E\u0432\u0430\u043D\u0438\u044F"));
        inv.setItem(16, item(Material.ENDER_EYE, "&d\u041D\u0430\u0431\u043B\u044E\u0434\u0435\u043D\u0438\u0435"));
        inv.setItem(22, item(Material.ZOMBIE_HEAD, "&5\u041C\u043E\u0431\u044B"));

        mainActions.clear();
        mainActions.put(10, Action.OPEN_MODES);
        mainActions.put(12, Action.OPEN_COMMANDS);
        mainActions.put(14, Action.OPEN_ENCHANTS);
        mainActions.put(16, Action.OPEN_TRACK);
        mainActions.put(22, Action.OPEN_MOBS);
        return inv;
    }
    private Inventory buildModesMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_MODES);
        inv.setItem(10, item(Material.DIAMOND_SWORD, "&b\u041A\u0440\u0435\u0430\u0442\u0438\u0432"));
        inv.setItem(12, item(Material.IRON_SWORD, "&a\u0412\u044B\u0436\u0438\u0432\u0430\u043D\u0438\u0435"));
        inv.setItem(14, item(Material.ENDER_EYE, "&d\u041D\u0430\u0431\u043B\u044E\u0434\u0430\u0442\u0435\u043B\u044C"));
        inv.setItem(25, item(Material.ARROW, "&e\u041D\u0430\u0437\u0430\u0434"));

        modeActions.clear();
        modeActions.put(10, ModeAction.CREATIVE);
        modeActions.put(12, ModeAction.SURVIVAL);
        modeActions.put(14, ModeAction.SPECTATOR);
        modeActions.put(25, ModeAction.BACK);
        return inv;
    }

    private Inventory buildCommandsMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_COMMANDS);
        inv.setItem(10, item(Material.REDSTONE_TORCH, "&c\u041E\u0442\u043A\u043B\u044E\u0447\u0438\u0442\u044C \u0444\u0438\u0434\u0431\u0435\u043A \u043A\u043E\u043C\u0430\u043D\u0434"));
        inv.setItem(12, item(Material.COMMAND_BLOCK, "&e\u0412\u043A\u043B\u044E\u0447\u0438\u0442\u044C \u0444\u0438\u0434\u0431\u0435\u043A \u043A\u043E\u043C\u0430\u043D\u0434"));
        inv.setItem(14, item(Material.NETHER_STAR, "&6\u0412\u044B\u0434\u0430\u0442\u044C OP"));
        inv.setItem(25, item(Material.ARROW, "&e\u041D\u0430\u0437\u0430\u0434"));

        commandActions.clear();
        commandActions.put(10, CommandAction.DISABLE_FEEDBACK);
        commandActions.put(12, CommandAction.ENABLE_FEEDBACK);
        commandActions.put(14, CommandAction.OP_SELF);
        commandActions.put(25, CommandAction.BACK);
        return inv;
    }

    private Inventory buildEnchantMenu() {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_ENCHANTS);
        inv.setItem(10, item(Material.ENCHANTED_BOOK, "&6\u0411\u0443\u0440"));
        inv.setItem(11, item(Material.ENCHANTED_BOOK, "&6\u041C\u0430\u0433\u043D\u0438\u0442"));
        inv.setItem(12, item(Material.ENCHANTED_BOOK, "&6\u0410\u0432\u0442\u043E\u043F\u043B\u0430\u0432\u043A\u0430"));
        inv.setItem(14, item(Material.ENCHANTED_BOOK, "&6\u0424\u0435\u0440\u043C\u0435\u0440"));
        inv.setItem(15, item(Material.ENCHANTED_BOOK, "&6\u041B\u0435\u0441\u043E\u0440\u0443\u0431"));
        inv.setItem(16, item(Material.ENCHANTED_BOOK, "&6\u0412\u0442\u043E\u0440\u0430\u044F \u0436\u0438\u0437\u043D\u044C"));
        inv.setItem(19, item(Material.ENCHANTED_BOOK, "&6\u0423\u0431\u0438\u0439\u0446\u0430"));
        inv.setItem(20, item(Material.ENCHANTED_BOOK, "&6\u0420\u043E\u0433"));
        inv.setItem(21, item(Material.ENCHANTED_BOOK, "&6\u041F\u0430\u043D\u0446\u0438\u0440\u044C"));
        inv.setItem(22, item(Material.ENCHANTED_BOOK, "&6\u041F\u043E\u0434\u0440\u044B\u0432"));
        inv.setItem(25, item(Material.ARROW, "&e\u041D\u0430\u0437\u0430\u0434"));

        enchantActions.clear();
        enchantActions.put(10, EnchantAction.DRILL);
        enchantActions.put(11, EnchantAction.MAGNET);
        enchantActions.put(12, EnchantAction.SMELT);
        enchantActions.put(14, EnchantAction.FARMER);
        enchantActions.put(15, EnchantAction.LUMBERJACK);
        enchantActions.put(16, EnchantAction.SECOND_LIFE);
        enchantActions.put(19, EnchantAction.ASSASSIN);
        enchantActions.put(20, EnchantAction.HORN);
        enchantActions.put(21, EnchantAction.SHELL);
        enchantActions.put(22, EnchantAction.BOOM_LEGS);
        enchantActions.put(25, EnchantAction.BACK);
        return inv;
    }

    private Inventory buildTrackMenu() {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_TRACK);
        int slot = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) {
                break;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta skull) {
                skull.setOwningPlayer(p);
                skull.setDisplayName(Msg.color("&b" + p.getName()));
                head.setItemMeta(skull);
            }
            inv.setItem(slot, head);
            slot++;
        }
        inv.setItem(49, item(Material.BARRIER, "&c\u041E\u0441\u0442\u0430\u043D\u043E\u0432\u0438\u0442\u044C \u0441\u043B\u0435\u0436\u043A\u0443"));
        inv.setItem(53, item(Material.ARROW, "&e\u041D\u0430\u0437\u0430\u0434"));
        return inv;
    }

    private Inventory buildMobsMenu(int page) {
        int totalPages = Math.max(1, (MOB_ENTRIES.size() + MOBS_PAGE_SIZE - 1) / MOBS_PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        String title = TITLE_MOBS_BASE + " " + (safePage + 1) + "/" + totalPages;

        Inventory inv = Bukkit.createInventory(null, 54, title);
        int start = safePage * MOBS_PAGE_SIZE;
        int end = Math.min(MOB_ENTRIES.size(), start + MOBS_PAGE_SIZE);
        for (int i = start; i < end; i++) {
            MobEntry entry = MOB_ENTRIES.get(i);
            inv.setItem(i - start, item(entry.icon, "&a" + entry.display));
        }

        if (safePage > 0) {
            inv.setItem(45, item(Material.ARROW, "&e\u041D\u0430\u0437\u0430\u0434"));
        }
        inv.setItem(48, item(Material.OAK_DOOR, "&7\u0412 \u0433\u043B\u0430\u0432\u043D\u043E\u0435 \u043C\u0435\u043D\u044E"));
        inv.setItem(49, item(Material.BARRIER, "&c\u0421\u043D\u044F\u0442\u044C \u043F\u0440\u0435\u0432\u0440\u0430\u0449\u0435\u043D\u0438\u0435"));
        if (safePage < totalPages - 1) {
            inv.setItem(53, item(Material.ARROW, "&e\u0412\u043F\u0435\u0440\u0451\u0434"));
        }
        return inv;
    }
    private static List<MobEntry> buildMobEntries() {
        List<MobEntry> list = new ArrayList<>();
        Set<String> vanillaIds = new HashSet<>();

        for (EntityType type : EntityType.values()) {
            if (type == EntityType.UNKNOWN || type == EntityType.PLAYER || type == EntityType.ARMOR_STAND) {
                continue;
            }
            if (!type.isAlive() || !type.isSpawnable()) {
                continue;
            }
            if (type == EntityType.ENDER_DRAGON) {
                continue;
            }
            NamespacedKey key = type.getKey();
            String id = key != null ? key.toString() : "minecraft:" + type.name().toLowerCase(Locale.ROOT);
            vanillaIds.add(id);
            list.add(new MobEntry(id, prettyName(type.name()), type, mobIcon(type)));
        }

        list.addAll(loadForgeEntities(vanillaIds));

        list.sort(Comparator.comparing(a -> a.id));
        return list;
    }

    private static List<MobEntry> loadForgeEntities(Set<String> vanillaIds) {
        List<MobEntry> list = new ArrayList<>();
        try {
            Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
            Object registry = forgeRegistries.getField("ENTITY_TYPES").get(null);
            Method getKeys = registry.getClass().getMethod("getKeys");
            Object keysObj = getKeys.invoke(registry);
            if (keysObj instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<Object> keys = (Set<Object>) keysObj;
                for (Object key : keys) {
                    String id = String.valueOf(key);
                    if (id.startsWith("minecraft:")) {
                        continue;
                    }
                    if (id.endsWith(":ender_dragon")) {
                        continue;
                    }
                    if (vanillaIds.contains(id)) {
                        continue;
                    }
                    String display = prettyName(id.contains(":") ? id.split(":", 2)[1] : id);
                    list.add(new MobEntry(id, display, null, Material.ZOMBIE_HEAD));
                }
            }
        } catch (Throwable ignored) {
            // Forge may be unavailable in dev environment
        }
        return list;
    }

    private static Material mobIcon(EntityType type) {
        String eggName = type.name() + "_SPAWN_EGG";
        Material egg = Material.matchMaterial(eggName);
        if (egg != null) {
            return egg;
        }
        return Material.ZOMBIE_SPAWN_EGG;
    }

    private static String prettyName(String rawName) {
        String key = rawName.toUpperCase(Locale.ROOT);
        String ru = RUS_MOB_NAMES.get(key);
        if (ru != null) {
            return ru;
        }
        String raw = rawName.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = raw.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    private static final Map<String, String> RUS_MOB_NAMES = new HashMap<>();
    static {
        RUS_MOB_NAMES.put("ALLAY", "\u0410\u043B\u043B\u0430\u0439");
        RUS_MOB_NAMES.put("ARMADILLO", "\u0411\u0440\u043E\u043D\u0435\u043D\u043E\u0441\u0435\u0446");
        RUS_MOB_NAMES.put("AXOLOTL", "\u0410\u043A\u0441\u043E\u043B\u043E\u0442\u043B\u044C");
        RUS_MOB_NAMES.put("BAT", "\u041B\u0435\u0442\u0443\u0447\u0430\u044F \u043C\u044B\u0448\u044C");
        RUS_MOB_NAMES.put("BEE", "\u041F\u0447\u0435\u043B\u0430");
        RUS_MOB_NAMES.put("BLAZE", "\u0418\u0444\u0440\u0438\u0442");
        RUS_MOB_NAMES.put("BOGGED", "\u0411\u043E\u043B\u043E\u0442\u043D\u0438\u043A");
        RUS_MOB_NAMES.put("BREEZE", "\u0411\u0440\u0438\u0437");
        RUS_MOB_NAMES.put("CAT", "\u041A\u043E\u0448\u043A\u0430");
        RUS_MOB_NAMES.put("CAMEL", "\u0412\u0435\u0440\u0431\u043B\u044E\u0434");
        RUS_MOB_NAMES.put("CAVE_SPIDER", "\u041F\u0435\u0449\u0435\u0440\u043D\u044B\u0439 \u043F\u0430\u0443\u043A");
        RUS_MOB_NAMES.put("CHICKEN", "\u041A\u0443\u0440\u0438\u0446\u0430");
        RUS_MOB_NAMES.put("COD", "\u0422\u0440\u0435\u0441\u043A\u0430");
        RUS_MOB_NAMES.put("COW", "\u041A\u043E\u0440\u043E\u0432\u0430");
        RUS_MOB_NAMES.put("CREEPER", "\u041A\u0440\u0438\u043F\u0435\u0440");
        RUS_MOB_NAMES.put("DOLPHIN", "\u0414\u0435\u043B\u044C\u0444\u0438\u043D");
        RUS_MOB_NAMES.put("DONKEY", "\u041E\u0441\u0451\u043B");
        RUS_MOB_NAMES.put("DROWNED", "\u0423\u0442\u043E\u043F\u043B\u0435\u043D\u043D\u0438\u043A");
        RUS_MOB_NAMES.put("ELDER_GUARDIAN", "\u0414\u0440\u0435\u0432\u043D\u0438\u0439 \u0441\u0442\u0440\u0430\u0436");
        RUS_MOB_NAMES.put("ENDERMAN", "\u042D\u043D\u0434\u0435\u0440\u043C\u0435\u043D");
        RUS_MOB_NAMES.put("ENDERMITE", "\u042D\u043D\u0434\u0435\u0440\u043C\u0438\u0442");
        RUS_MOB_NAMES.put("EVOKER", "\u0412\u044B\u0437\u044B\u0432\u0430\u0442\u0435\u043B\u044C");
        RUS_MOB_NAMES.put("FOX", "\u041B\u0438\u0441\u0430");
        RUS_MOB_NAMES.put("FROG", "\u041B\u044F\u0433\u0443\u0448\u043A\u0430");
        RUS_MOB_NAMES.put("GHAST", "\u0413\u0430\u0441\u0442");
        RUS_MOB_NAMES.put("GLOW_SQUID", "\u0421\u0432\u0435\u0442\u044F\u0449\u0438\u0439\u0441\u044F \u0441\u043F\u0440\u0443\u0442");
        RUS_MOB_NAMES.put("GOAT", "\u041A\u043E\u0437\u0430");
        RUS_MOB_NAMES.put("GUARDIAN", "\u0421\u0442\u0440\u0430\u0436");
        RUS_MOB_NAMES.put("HOGLIN", "\u0425\u043E\u0433\u043B\u0438\u043D");
        RUS_MOB_NAMES.put("HORSE", "\u041B\u043E\u0448\u0430\u0434\u044C");
        RUS_MOB_NAMES.put("HUSK", "\u041A\u0430\u0434\u0430\u0432\u0440");
        RUS_MOB_NAMES.put("ILLUSIONER", "\u0418\u043B\u043B\u044E\u0437\u0438\u043E\u043D\u0438\u0441\u0442");
        RUS_MOB_NAMES.put("IRON_GOLEM", "\u0416\u0435\u043B\u0435\u0437\u043D\u044B\u0439 \u0433\u043E\u043B\u0435\u043C");
        RUS_MOB_NAMES.put("LLAMA", "\u041B\u0430\u043C\u0430");
        RUS_MOB_NAMES.put("MAGMA_CUBE", "\u041C\u0430\u0433\u043C\u043E\u0432\u044B\u0439 \u043A\u0443\u0431");
        RUS_MOB_NAMES.put("MOOSHROOM", "\u041C\u0443\u0445\u043E\u043C\u043E\u0440\u043A\u0430");
        RUS_MOB_NAMES.put("MULE", "\u041C\u0443\u043B");
        RUS_MOB_NAMES.put("OCELOT", "\u041E\u0446\u0435\u043B\u043E\u0442");
        RUS_MOB_NAMES.put("PANDA", "\u041F\u0430\u043D\u0434\u0430");
        RUS_MOB_NAMES.put("PARROT", "\u041F\u043E\u043F\u0443\u0433\u0430\u0439");
        RUS_MOB_NAMES.put("PHANTOM", "\u0424\u0430\u043D\u0442\u043E\u043C");
        RUS_MOB_NAMES.put("PIG", "\u0421\u0432\u0438\u043D\u044C\u044F");
        RUS_MOB_NAMES.put("PIGLIN", "\u041F\u0438\u0433\u043B\u0438\u043D");
        RUS_MOB_NAMES.put("PIGLIN_BRUTE", "\u041F\u0438\u0433\u043B\u0438\u043D-\u0437\u0432\u0435\u0440\u044C");
        RUS_MOB_NAMES.put("PILLAGER", "\u0420\u0430\u0437\u0431\u043E\u0439\u043D\u0438\u043A");
        RUS_MOB_NAMES.put("POLAR_BEAR", "\u0411\u0435\u043B\u044B\u0439 \u043C\u0435\u0434\u0432\u0435\u0434\u044C");
        RUS_MOB_NAMES.put("PUFFERFISH", "\u0420\u044B\u0431\u0430-\u0451\u0436");
        RUS_MOB_NAMES.put("RABBIT", "\u041A\u0440\u043E\u043B\u0438\u043A");
        RUS_MOB_NAMES.put("RAVAGER", "\u0420\u0430\u0437\u043E\u0440\u0438\u0442\u0435\u043B\u044C");
        RUS_MOB_NAMES.put("SALMON", "\u041B\u043E\u0441\u043E\u0441\u044C");
        RUS_MOB_NAMES.put("SHEEP", "\u041E\u0432\u0446\u0430");
        RUS_MOB_NAMES.put("SHULKER", "\u0428\u0430\u043B\u043A\u0435\u0440");
        RUS_MOB_NAMES.put("SILVERFISH", "\u0427\u0435\u0448\u0443\u0439\u043D\u0438\u0446\u0430");
        RUS_MOB_NAMES.put("SKELETON", "\u0421\u043A\u0435\u043B\u0435\u0442");
        RUS_MOB_NAMES.put("SKELETON_HORSE", "\u041B\u043E\u0448\u0430\u0434\u044C-\u0441\u043A\u0435\u043B\u0435\u0442");
        RUS_MOB_NAMES.put("SLIME", "\u0421\u043B\u0438\u0437\u0435\u043D\u044C");
        RUS_MOB_NAMES.put("SNIFFER", "\u041D\u044E\u0445\u0430\u0447");
        RUS_MOB_NAMES.put("SNOW_GOLEM", "\u0421\u043D\u0435\u0436\u043D\u044B\u0439 \u0433\u043E\u043B\u0435\u043C");
        RUS_MOB_NAMES.put("SPIDER", "\u041F\u0430\u0443\u043A");
        RUS_MOB_NAMES.put("SQUID", "\u0421\u043F\u0440\u0443\u0442");
        RUS_MOB_NAMES.put("STRAY", "\u0417\u0438\u043C\u043E\u0433\u043E\u0440");
        RUS_MOB_NAMES.put("STRIDER", "\u041B\u0430\u0432\u043E\u043C\u0435\u0440");
        RUS_MOB_NAMES.put("TADPOLE", "\u0413\u043E\u043B\u043E\u0432\u0430\u0441\u0442\u0438\u043A");
        RUS_MOB_NAMES.put("TRADER_LLAMA", "\u041B\u0430\u043C\u0430 \u0442\u043E\u0440\u0433\u043E\u0432\u0446\u0430");
        RUS_MOB_NAMES.put("TROPICAL_FISH", "\u0422\u0440\u043E\u043F\u0438\u0447\u0435\u0441\u043A\u0430\u044F \u0440\u044B\u0431\u0430");
        RUS_MOB_NAMES.put("TURTLE", "\u0427\u0435\u0440\u0435\u043F\u0430\u0445\u0430");
        RUS_MOB_NAMES.put("VEX", "\u0412\u0440\u0435\u0434\u0438\u043D\u0430");
        RUS_MOB_NAMES.put("VILLAGER", "\u0416\u0438\u0442\u0435\u043B\u044C");
        RUS_MOB_NAMES.put("VINDICATOR", "\u041F\u043E\u0431\u043E\u0440\u043D\u0438\u043A");
        RUS_MOB_NAMES.put("WANDERING_TRADER", "\u0421\u0442\u0440\u0430\u043D\u0441\u0442\u0432\u0443\u044E\u0449\u0438\u0439 \u0442\u043E\u0440\u0433\u043E\u0432\u0435\u0446");
        RUS_MOB_NAMES.put("WARDEN", "\u0412\u0430\u0440\u0434\u0435\u043D");
        RUS_MOB_NAMES.put("WITCH", "\u0412\u0435\u0434\u044C\u043C\u0430");
        RUS_MOB_NAMES.put("WITHER", "\u0418\u0441\u0441\u0443\u0448\u0438\u0442\u0435\u043B\u044C");
        RUS_MOB_NAMES.put("WITHER_SKELETON", "\u0421\u043A\u0435\u043B\u0435\u0442-\u0438\u0441\u0441\u0443\u0448\u0438\u0442\u0435\u043B\u044C");
        RUS_MOB_NAMES.put("WOLF", "\u0412\u043E\u043B\u043A");
        RUS_MOB_NAMES.put("ZOGLIN", "\u0417\u043E\u0433\u043B\u0438\u043D");
        RUS_MOB_NAMES.put("ZOMBIE", "\u0417\u043E\u043C\u0431\u0438");
        RUS_MOB_NAMES.put("ZOMBIE_HORSE", "\u041B\u043E\u0448\u0430\u0434\u044C-\u0437\u043E\u043C\u0431\u0438");
        RUS_MOB_NAMES.put("ZOMBIE_VILLAGER", "\u0417\u043E\u043C\u0431\u0438-\u0436\u0438\u0442\u0435\u043B\u044C");
        RUS_MOB_NAMES.put("ZOMBIFIED_PIGLIN", "\u0417\u043E\u043C\u0431\u0438\u0444\u0438\u0446\u0438\u0440\u043E\u0432\u0430\u043D\u043D\u044B\u0439 \u043F\u0438\u0433\u043B\u0438\u043D");
    }

    private static int parseMobPage(String title) {
        if (!title.startsWith(TITLE_MOBS_BASE)) {
            return 0;
        }
        int idx = title.lastIndexOf(' ');
        if (idx < 0 || idx + 1 >= title.length()) {
            return 0;
        }
        String part = title.substring(idx + 1);
        int slash = part.indexOf('/');
        if (slash <= 0) {
            return 0;
        }
        try {
            int page = Integer.parseInt(part.substring(0, slash).trim());
            return Math.max(0, page - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private ItemStack item(Material mat, String name) {
        ItemStack it = new ItemStack(mat, 1);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.color(name));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack book(Enchantment ench, String name) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        if (meta != null) {
            meta.addStoredEnchant(ench, 1, true);
            meta.setDisplayName(Msg.color(name));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            book.setItemMeta(meta);
        }
        return book;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) {
            return;
        }
        String title = e.getView().getTitle();
        if (title.equals(TITLE_MAIN)) {
            handleMainClick(e);
        } else if (title.equals(TITLE_MODES)) {
            handleModesClick(e);
        } else if (title.equals(TITLE_COMMANDS)) {
            handleCommandsClick(e);
        } else if (title.equals(TITLE_ENCHANTS)) {
            handleEnchantClick(e);
        } else if (title.equals(TITLE_TRACK)) {
            handleTrackClick(e);
        } else if (title.startsWith(TITLE_MOBS_BASE)) {
            handleMobsClick(e);
        }
    }

    private void handleMainClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        Action action = mainActions.get(e.getRawSlot());
        if (action == null) {
            return;
        }
        switch (action) {
            case OPEN_MODES:
                p.openInventory(buildModesMenu());
                break;
            case OPEN_COMMANDS:
                p.openInventory(buildCommandsMenu());
                break;
            case OPEN_ENCHANTS:
                p.openInventory(buildEnchantMenu());
                break;
            case OPEN_TRACK:
                p.openInventory(buildTrackMenu());
                break;
            case OPEN_MOBS:
                p.openInventory(buildMobsMenu(0));
                break;
            default:
                break;
        }
    }
    private void handleModesClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        ModeAction action = modeActions.get(e.getRawSlot());
        if (action == null) {
            return;
        }
        switch (action) {
            case CREATIVE:
                p.setGameMode(GameMode.CREATIVE);
                Msg.send(p, "&a\u0420\u0435\u0436\u0438\u043C: \u043A\u0440\u0435\u0430\u0442\u0438\u0432.");
                break;
            case SURVIVAL:
                p.setGameMode(GameMode.SURVIVAL);
                Msg.send(p, "&a\u0420\u0435\u0436\u0438\u043C: \u0432\u044B\u0436\u0438\u0432\u0430\u043D\u0438\u0435.");
                break;
            case SPECTATOR:
                p.setGameMode(GameMode.SPECTATOR);
                Msg.send(p, "&a\u0420\u0435\u0436\u0438\u043C: \u043D\u0430\u0431\u043B\u044E\u0434\u0430\u0442\u0435\u043B\u044C.");
                break;
            case BACK:
                p.openInventory(buildMainMenu());
                break;
            default:
                break;
        }
    }

    private void handleCommandsClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        CommandAction action = commandActions.get(e.getRawSlot());
        if (action == null) {
            return;
        }
        switch (action) {
            case DISABLE_FEEDBACK:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule sendCommandFeedback false");
                Msg.send(p, "&e\u0424\u0438\u0434\u0431\u0435\u043A \u043A\u043E\u043C\u0430\u043D\u0434 \u043E\u0442\u043A\u043B\u044E\u0447\u0451\u043D.");
                break;
            case ENABLE_FEEDBACK:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gamerule sendCommandFeedback true");
                Msg.send(p, "&e\u0424\u0438\u0434\u0431\u0435\u043A \u043A\u043E\u043C\u0430\u043D\u0434 \u0432\u043A\u043B\u044E\u0447\u0451\u043D.");
                break;
            case OP_SELF:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "op " + p.getName());
                Msg.send(p, "&6\u0412\u044B \u043F\u043E\u043B\u0443\u0447\u0438\u043B\u0438 OP.");
                break;
            case BACK:
                p.openInventory(buildMainMenu());
                break;
            default:
                break;
        }
    }

    private void handleEnchantClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        EnchantAction action = enchantActions.get(e.getRawSlot());
        if (action == null) {
            return;
        }
        switch (action) {
            case DRILL:
                p.getInventory().addItem(book(DrillEnchant.get(), "&6\u0411\u0443\u0440"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u0411\u0443\u0440 \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case MAGNET:
                p.getInventory().addItem(book(MagnetEnchant.get(), "&6\u041C\u0430\u0433\u043D\u0438\u0442"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u041C\u0430\u0433\u043D\u0438\u0442 \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case SMELT:
                p.getInventory().addItem(book(AutoSmeltEnchant.get(), "&6\u0410\u0432\u0442\u043E\u043F\u043B\u0430\u0432\u043A\u0430"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u0410\u0432\u0442\u043E\u043F\u043B\u0430\u0432\u043A\u0430 \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case FARMER:
                p.getInventory().addItem(book(FarmerEnchant.get(), "&6\u0424\u0435\u0440\u043C\u0435\u0440"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u0424\u0435\u0440\u043C\u0435\u0440 \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case LUMBERJACK:
                p.getInventory().addItem(book(LumberjackEnchant.get(), "&6\u041B\u0435\u0441\u043E\u0440\u0443\u0431"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u041B\u0435\u0441\u043E\u0440\u0443\u0431 \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case SECOND_LIFE:
                p.getInventory().addItem(book(SecondLifeEnchant.get(), "&6\u0412\u0442\u043E\u0440\u0430\u044F \u0436\u0438\u0437\u043D\u044C"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u0412\u0442\u043E\u0440\u0430\u044F \u0436\u0438\u0437\u043D\u044C \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case ASSASSIN:
                p.getInventory().addItem(book(AssassinEnchant.get(), "&6\u0423\u0431\u0438\u0439\u0446\u0430"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u0423\u0431\u0438\u0439\u0446\u0430 \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case HORN:
                p.getInventory().addItem(book(HornEnchant.get(), "&6\u0420\u043E\u0433"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u0420\u043E\u0433 \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case SHELL:
                p.getInventory().addItem(book(ShellEnchant.get(), "&6\u041F\u0430\u043D\u0446\u0438\u0440\u044C"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u041F\u0430\u043D\u0446\u0438\u0440\u044C \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case BOOM_LEGS:
                p.getInventory().addItem(book(BoomLeggingsEnchant.get(), "&6\u041F\u043E\u0434\u0440\u044B\u0432"));
                Msg.send(p, "&6\u041A\u043D\u0438\u0433\u0430 \u041F\u043E\u0434\u0440\u044B\u0432 \u0432\u044B\u0434\u0430\u043D\u0430.");
                break;
            case BACK:
                p.openInventory(buildMainMenu());
                break;
            default:
                break;
        }
    }

    private void handleTrackClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player watcher = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();
        if (slot == 49) {
            stopFollow(watcher);
            Msg.send(watcher, "&e\u0421\u043B\u0435\u0436\u043A\u0430 \u043E\u0442\u043A\u043B\u044E\u0447\u0435\u043D\u0430.");
            return;
        }
        if (slot == 53) {
            watcher.openInventory(buildMainMenu());
            return;
        }
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (!(meta instanceof SkullMeta skull) || skull.getOwningPlayer() == null) {
            return;
        }
        Player target = skull.getOwningPlayer().getPlayer();
        if (target == null || !target.isOnline()) {
            Msg.send(watcher, "&c\u0418\u0433\u0440\u043E\u043A \u043D\u0435 \u043D\u0430\u0439\u0434\u0435\u043D.");
            return;
        }
        startFollow(watcher, target);
        Msg.send(watcher, "&a\u0421\u043B\u0435\u0436\u043A\u0430 \u0437\u0430: " + target.getName());
    }

    private void handleMobsClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        String title = e.getView().getTitle();
        int page = parseMobPage(title);
        int slot = e.getRawSlot();

        if (slot == 45) {
            p.openInventory(buildMobsMenu(page - 1));
            return;
        }
        if (slot == 53) {
            p.openInventory(buildMobsMenu(page + 1));
            return;
        }
        if (slot == 48) {
            p.openInventory(buildMainMenu());
            return;
        }
        if (slot == 49) {
            stopDisguise(p);
            return;
        }

        if (slot < 0 || slot >= MOBS_PAGE_SIZE) {
            return;
        }
        int index = page * MOBS_PAGE_SIZE + slot;
        if (index < 0 || index >= MOB_ENTRIES.size()) {
            return;
        }
        startDisguise(p, MOB_ENTRIES.get(index));
    }

    private void startDisguise(Player player, MobEntry entry) {
        stopDisguise(player);
        Entity mob = null;
        if (entry.bukkitType != null) {
            mob = player.getWorld().spawnEntity(player.getLocation(), entry.bukkitType);
        } else {
            mob = spawnModEntity(player, entry.id);
        }
        if (mob == null) {
            Msg.send(player, "&c\u041D\u0435 \u0443\u0434\u0430\u043B\u043E\u0441\u044C \u0441\u043E\u0437\u0434\u0430\u0442\u044C \u043C\u043E\u0431\u0430: " + entry.id);
            return;
        }
        prepareDisguiseEntity(mob);

        boolean wasHidden = follow.containsKey(player.getUniqueId());
        boolean prevInvisible = player.isInvisible();
        boolean prevCollidable = player.isCollidable();
        boolean prevCanPickup = player.getCanPickupItems();
        boolean prevInvulnerable = player.isInvulnerable();

        if (!wasHidden) {
            hideFromAll(player, true);
        }
        player.setInvisible(true);
        player.setCollidable(false);
        player.setCanPickupItems(false);
        player.setInvulnerable(true);

        Entity tracked = mob;
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || tracked.isDead()) {
                    stopDisguise(player);
                    cancel();
                    return;
                }
                tracked.teleport(player.getLocation());
                tracked.setVelocity(new Vector(0, 0, 0));
                if (tracked instanceof LivingEntity living) {
                    living.setRotation(player.getLocation().getYaw(), player.getLocation().getPitch());
                    living.setAI(false);
                    living.setSilent(true);
                    living.setInvulnerable(true);
                    living.setCollidable(false);
                    living.setGravity(false);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 1L);
        disguises.put(player.getUniqueId(), new DisguiseState(mob, task, !wasHidden, prevInvisible, prevCollidable, prevCanPickup, prevInvulnerable));
    }

    private Entity spawnModEntity(Player player, String id) {
        Location loc = player.getLocation();
        String tag = "faivents_disguise_" + player.getUniqueId().toString().replace("-", "");
        String cmd = String.format(Locale.US,
                "summon %s %.3f %.3f %.3f {Tags:[\"faivents_disguise\",\"%s\"],NoAI:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b}",
                id, loc.getX(), loc.getY(), loc.getZ(), tag);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);

        for (Entity ent : loc.getWorld().getNearbyEntities(loc, 3, 3, 3)) {
            if (ent instanceof Player) {
                continue;
            }
            if (ent.getScoreboardTags().contains(tag)) {
                return ent;
            }
        }
        return null;
    }

    private void prepareDisguiseEntity(Entity mob) {
        mob.setSilent(true);
        mob.setInvulnerable(true);
        setCollidableSafe(mob, false);
        mob.setGravity(false);
        setRemoveWhenFarAwaySafe(mob, false);
        if (mob instanceof LivingEntity living) {
            living.setAI(false);
        }
    }
    private void stopDisguise(Player player) {
        DisguiseState state = disguises.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (state.task != null) {
            state.task.cancel();
        }
        if (state.mob != null && !state.mob.isDead()) {
            state.mob.remove();
        }
        player.setInvisible(state.prevInvisible);
        player.setCollidable(state.prevCollidable);
        player.setCanPickupItems(state.prevCanPickup);
        player.setInvulnerable(state.prevInvulnerable);
        if (state.restoreVisibility && !follow.containsKey(player.getUniqueId())) {
            hideFromAll(player, false);
        }
    }

    private void startFollow(Player watcher, Player target) {
        stopFollow(watcher);

        boolean prevAllowFlight = watcher.getAllowFlight();
        boolean prevFlying = watcher.isFlying();
        boolean prevCollidable = watcher.isCollidable();
        boolean prevInvisible = watcher.isInvisible();
        boolean prevCanPickup = watcher.getCanPickupItems();
        boolean prevInvulnerable = watcher.isInvulnerable();
        GameMode prevGameMode = watcher.getGameMode();
        String prevListName = watcher.getPlayerListName();
        Location origin = watcher.getLocation().clone();

        hideFromAll(watcher, true);
        watcher.setGameMode(GameMode.SPECTATOR);
        watcher.setInvisible(true);
        watcher.setCollidable(false);
        watcher.setCanPickupItems(false);
        watcher.setInvulnerable(true);
        applyVanish(watcher, true);

        watcher.setSpectatorTarget(target);
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!watcher.isOnline() || target == null || !target.isOnline()) {
                    stopFollow(watcher);
                    cancel();
                    return;
                }
                if (watcher.getGameMode() != GameMode.SPECTATOR) {
                    stopFollow(watcher);
                    cancel();
                    return;
                }
                if (watcher.getSpectatorTarget() == null || !watcher.getSpectatorTarget().getUniqueId().equals(target.getUniqueId())) {
                    watcher.setSpectatorTarget(target);
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 10L);

        FollowState state = new FollowState(prevAllowFlight, prevFlying, prevCollidable, prevInvisible, prevCanPickup, prevInvulnerable, prevGameMode, prevListName, origin, task, target.getUniqueId());
        follow.put(watcher.getUniqueId(), state);
    }

    private void stopFollow(Player watcher) {
        FollowState state = follow.remove(watcher.getUniqueId());
        if (state == null) {
            return;
        }
        if (state.task != null) {
            state.task.cancel();
        }
        watcher.setAllowFlight(state.prevAllowFlight);
        watcher.setFlying(state.prevFlying);
        watcher.setCollidable(state.prevCollidable);
        watcher.setInvisible(state.prevInvisible);
        watcher.setCanPickupItems(state.prevCanPickup);
        watcher.setInvulnerable(state.prevInvulnerable);
        watcher.setSpectatorTarget(null);
        watcher.setGameMode(state.prevGameMode);
        applyVanish(watcher, false);
        if (state.prevListName != null) {
            watcher.setPlayerListName(state.prevListName);
        }
        if (state.origin != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (watcher.isOnline()) {
                    watcher.teleport(state.origin);
                }
            }, 2L);
        }
        if (!disguises.containsKey(watcher.getUniqueId())) {
            hideFromAll(watcher, false);
        }
    }

    private void stopFollowByTarget(UUID targetId) {
        List<UUID> toStop = new ArrayList<>();
        for (Map.Entry<UUID, FollowState> e : follow.entrySet()) {
            if (e.getValue().target.equals(targetId)) {
                toStop.add(e.getKey());
            }
        }
        for (UUID watcherId : toStop) {
            Player watcher = Bukkit.getPlayer(watcherId);
            if (watcher != null) {
                stopFollow(watcher);
                Msg.send(watcher, "&e\u0421\u043B\u0435\u0436\u043A\u0430 \u043E\u0442\u043A\u043B\u044E\u0447\u0435\u043D\u0430.");
            } else {
                follow.remove(watcherId);
            }
        }
    }

    private void hideFromAll(Player watcher, boolean hide) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (hide) {
                p.hidePlayer(plugin, watcher);
            } else {
                p.showPlayer(plugin, watcher);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        for (UUID watcherId : follow.keySet()) {
            Player watcher = Bukkit.getPlayer(watcherId);
            if (watcher != null) {
                joined.hidePlayer(plugin, watcher);
            }
        }
        for (UUID disguisedId : disguises.keySet()) {
            Player disguised = Bukkit.getPlayer(disguisedId);
            if (disguised != null) {
                joined.hidePlayer(plugin, disguised);
            }
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        if (!follow.containsKey(player.getUniqueId())) {
            return;
        }
        Advancement adv = event.getAdvancement();
        revokeAdvancement(player, adv);
        Bukkit.getScheduler().runTask(plugin, () -> revokeAdvancement(player, adv));
    }

    private void revokeAdvancement(Player player, Advancement adv) {
        AdvancementProgress progress = player.getAdvancementProgress(adv);
        for (String c : adv.getCriteria()) {
            progress.revokeCriteria(c);
        }
    }

    @EventHandler
    public void onSneakExit(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        if (follow.containsKey(player.getUniqueId())) {
            stopFollow(player);
            Msg.send(player, "&e\u0421\u043B\u0435\u0436\u043A\u0430 \u043E\u0442\u043A\u043B\u044E\u0447\u0435\u043D\u0430.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        stopDisguise(player);
        stopFollow(player);
        stopFollowByTarget(player.getUniqueId());
    }

    private static final class MobEntry {
        final String id;
        final String display;
        final EntityType bukkitType;
        final Material icon;

        MobEntry(String id, String display, EntityType bukkitType, Material icon) {
            this.id = id;
            this.display = display;
            this.bukkitType = bukkitType;
            this.icon = icon;
        }
    }

    private static final class DisguiseState {
        final Entity mob;
        final BukkitRunnable task;
        final boolean restoreVisibility;
        final boolean prevInvisible;
        final boolean prevCollidable;
        final boolean prevCanPickup;
        final boolean prevInvulnerable;

        DisguiseState(Entity mob, BukkitRunnable task, boolean restoreVisibility,
                      boolean prevInvisible, boolean prevCollidable, boolean prevCanPickup,
                      boolean prevInvulnerable) {
            this.mob = mob;
            this.task = task;
            this.restoreVisibility = restoreVisibility;
            this.prevInvisible = prevInvisible;
            this.prevCollidable = prevCollidable;
            this.prevCanPickup = prevCanPickup;
            this.prevInvulnerable = prevInvulnerable;
        }
    }

    private static final class FollowState {
        final boolean prevAllowFlight;
        final boolean prevFlying;
        final boolean prevCollidable;
        final boolean prevInvisible;
        final boolean prevCanPickup;
        final boolean prevInvulnerable;
        final GameMode prevGameMode;
        final String prevListName;
        final Location origin;
        final BukkitRunnable task;
        final UUID target;

        FollowState(boolean prevAllowFlight, boolean prevFlying, boolean prevCollidable,
                    boolean prevInvisible, boolean prevCanPickup, boolean prevInvulnerable, GameMode prevGameMode,
                    String prevListName,
                    Location origin, BukkitRunnable task, UUID target) {
            this.prevAllowFlight = prevAllowFlight;
            this.prevFlying = prevFlying;
            this.prevCollidable = prevCollidable;
            this.prevInvisible = prevInvisible;
            this.prevCanPickup = prevCanPickup;
            this.prevInvulnerable = prevInvulnerable;
            this.prevGameMode = prevGameMode;
            this.prevListName = prevListName;
            this.origin = origin;
            this.task = task;
            this.target = target;
        }
    }

    private void setCollidableSafe(Entity entity, boolean value) {
        try {
            entity.getClass().getMethod("setCollidable", boolean.class).invoke(entity, value);
        } catch (Exception ignored) {
            // Not supported on this server implementation
        }
    }

    private void setRemoveWhenFarAwaySafe(Entity entity, boolean value) {
        try {
            entity.getClass().getMethod("setRemoveWhenFarAway", boolean.class).invoke(entity, value);
        } catch (Exception ignored) {
            // Not supported on this server implementation
        }
    }

    private void applyVanish(Player player, boolean vanished) {
        if (vanished) {
            player.setPlayerListName("");
            player.setMetadata("vanished", new FixedMetadataValue(plugin, true));
            player.setMetadata("VANISHED", new FixedMetadataValue(plugin, true));
            player.setMetadata("essentialsvanished", new FixedMetadataValue(plugin, true));
        } else {
            player.removeMetadata("vanished", plugin);
            player.removeMetadata("VANISHED", plugin);
            player.removeMetadata("essentialsvanished", plugin);
        }
    }
}


