package me.dominus.faivents;

import me.dominus.faivents.command.ChaseCommand;
import me.dominus.faivents.command.ChitCommand;
import me.dominus.faivents.command.EventCommand;
import me.dominus.faivents.command.InvCommand;
import me.dominus.faivents.command.ShopCommand;
import me.dominus.faivents.command.SecretCommand;
import me.dominus.faivents.enchant.*;
import me.dominus.faivents.events.EventManager;
import me.dominus.faivents.quarry.QuarryListener;
import me.dominus.faivents.quarry.QuarryManager;
import me.dominus.faivents.util.InvisListener;
import me.dominus.faivents.util.InvisManager;
import me.dominus.faivents.util.Msg;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FAIventsPlugin extends JavaPlugin {

    private EventManager eventManager;
    private InvisManager invisManager;
    private QuarryManager quarryManager;
    private boolean secretRequiresOp = false;

    @Override
    public void onEnable() {
        safeLoadConfig();
        Msg.init(this);

        eventManager = new EventManager(this);
        invisManager = new InvisManager(this);
        quarryManager = new QuarryManager(this);

        DrillEnchant.register(this);
        MagnetEnchant.register(this);
        AutoSmeltEnchant.register(this);
        FarmerEnchant.register(this);
        LumberjackEnchant.register(this);
        SecondLifeEnchant.register(this);
        AssassinEnchant.register(this);
        HornEnchant.register(this);
        ShellEnchant.register(this);
        BoomLeggingsEnchant.register(this);

        getServer().getPluginManager().registerEvents(new DrillListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomEnchantListener(), this);
        getServer().getPluginManager().registerEvents(new CustomAnvilListener(), this);
        getServer().getPluginManager().registerEvents(new SecondLifeListener(), this);
        getServer().getPluginManager().registerEvents(new FarmerListener(this), this);
        getServer().getPluginManager().registerEvents(new LumberjackListener(), this);
        getServer().getPluginManager().registerEvents(new AssassinListener(this, invisManager), this);
        getServer().getPluginManager().registerEvents(new HornListener(), this);
        getServer().getPluginManager().registerEvents(new ShellListener(), this);
        getServer().getPluginManager().registerEvents(new BoomLeggingsListener(this), this);
        getServer().getPluginManager().registerEvents(new InvisListener(this, invisManager), this);
        QuarryListener quarryListener = new QuarryListener(quarryManager);
        getServer().getPluginManager().registerEvents(quarryListener, this);

        EventCommand eventCmd = new EventCommand(eventManager);
        bindCommand(
                "event",
                eventCmd,
                eventCmd,
                "Manage events",
                "/event <ufo|meteor|glitch|horror|artifact> <start|stop|status> [player|all]",
                "faivents.admin",
                "&cYou don't have permission."
        );
        secretRequiresOp = getConfig().getBoolean("secret.require_op", false);
        SecretCommand secret = new SecretCommand(this);
        bindCommand(
                "secret",
                secret,
                null,
                "Secret admin menu",
                "/secret",
                null,
                null
        );
        getServer().getPluginManager().registerEvents(secret, this);

        bindCommand(
                "inv",
                new InvCommand(invisManager),
                null,
                "Toggle/invisibility tools",
                "/inv",
                "faivents.admin",
                "&cYou don't have permission."
        );
        ShopCommand shop = new ShopCommand(this, quarryManager);
        bindCommand(
                "shop",
                shop,
                null,
                "Open shop menu",
                "/shop",
                null,
                null
        );
        getServer().getPluginManager().registerEvents(shop, this);

        ChaseCommand chase = new ChaseCommand(quarryManager, quarryListener);
        bindCommand(
                "chase",
                chase,
                null,
                "Open your level 5 quarry storage",
                "/chase",
                null,
                null
        );
        ChitCommand chit = new ChitCommand(this);
        bindCommand(
                "chit",
                chit,
                null,
                "Anti-cheat check menu",
                "/chit <player>",
                null,
                null
        );
        getServer().getPluginManager().registerEvents(chit, this);

        registerBookRecipes();

        getLogger().info("FAIvents enabled");
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (invisManager != null) {
            invisManager.shutdown();
        }
        if (quarryManager != null) {
            quarryManager.shutdown();
        }
        getLogger().info("FAIvents disabled");
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public boolean isSecretRequiresOp() {
        return secretRequiresOp;
    }

    private void registerBookRecipes() {
        addRecipe("book_drill", CustomEnchantBooks.createBook(DrillEnchant.get(), "&6\u0411\u0443\u0440"), 'D', Material.DIAMOND_PICKAXE, 'R', Material.REDSTONE, 'B', Material.BOOK);
        addRecipe("book_magnet", CustomEnchantBooks.createBook(MagnetEnchant.get(), "&6\u041C\u0430\u0433\u043D\u0438\u0442"), 'I', Material.IRON_INGOT, 'H', Material.HOPPER, 'B', Material.BOOK);
        addRecipe("book_autosmelt", CustomEnchantBooks.createBook(AutoSmeltEnchant.get(), "&6\u0410\u0432\u0442\u043E\u043F\u043B\u0430\u0432\u043A\u0430"), 'F', Material.FURNACE, 'C', Material.COAL, 'B', Material.BOOK);
        addRecipe("book_farmer", CustomEnchantBooks.createBook(FarmerEnchant.get(), "&6\u0424\u0435\u0440\u043C\u0435\u0440"), 'W', Material.WHEAT, 'H', Material.GOLDEN_HOE, 'B', Material.BOOK);
        addRecipe("book_lumber", CustomEnchantBooks.createBook(LumberjackEnchant.get(), "&6\u041B\u0435\u0441\u043E\u0440\u0443\u0431"), 'A', Material.DIAMOND_AXE, 'L', Material.OAK_LOG, 'B', Material.BOOK);
        addRecipe("book_secondlife", CustomEnchantBooks.createBook(SecondLifeEnchant.get(), "&6\u0412\u0442\u043E\u0440\u0430\u044F \u0436\u0438\u0437\u043D\u044C"), 'G', Material.GOLDEN_APPLE, 'C', Material.DIAMOND_CHESTPLATE, 'B', Material.BOOK);
        addRecipe("book_assassin", CustomEnchantBooks.createBook(AssassinEnchant.get(), "&6\u0410\u0441\u0441\u0430\u0441\u0438\u043D"), 'N', Material.NETHER_STAR, 'H', Material.DIAMOND_HELMET, 'B', Material.BOOK);
        addRecipe("book_horn", CustomEnchantBooks.createBook(HornEnchant.get(), "&6\u0420\u043E\u0433"), 'O', Material.GOAT_HORN, 'G', Material.ENCHANTED_GOLDEN_APPLE, 'B', Material.BOOK);
        addRecipe("book_shell", CustomEnchantBooks.createBook(ShellEnchant.get(), "&6\u041F\u0430\u043D\u0446\u0438\u0440\u044C"), 'T', Material.TURTLE_HELMET, 'S', Material.SHIELD, 'B', Material.BOOK);
        addRecipe("book_boom", CustomEnchantBooks.createBook(BoomLeggingsEnchant.get(), "&6\u041F\u043E\u0434\u0440\u044B\u0432"), 'C', Material.CREEPER_HEAD, 'L', Material.DIAMOND_LEGGINGS, 'B', Material.BOOK);
    }


    private void addRecipe(String key, ItemStack result, char a, Material ma, char b, Material mb, char c, Material mc) {
        NamespacedKey ns = new NamespacedKey(this, key);
        ShapedRecipe r = new ShapedRecipe(ns, result);
        r.shape("" + a + b + a, "" + b + c + b, "" + a + b + a);
        r.setIngredient(a, ma);
        r.setIngredient(b, mb);
        r.setIngredient(c, mc);
        getServer().addRecipe(r);
    }

    private void addRecipe(String key, ItemStack result,
                           String row1,
                           String row2,
                           String row3,
                           char a, Material ma,
                           char b, Material mb,
                           char c, Material mc,
                           char d, Material md,
                           char e, Material me,
                           char f, Material mf,
                           char g, Material mg) {
        NamespacedKey ns = new NamespacedKey(this, key);
        ShapedRecipe r = new ShapedRecipe(ns, result);
        r.shape(row1, row2, row3);
        r.setIngredient(a, ma);
        r.setIngredient(b, mb);
        r.setIngredient(c, mc);
        r.setIngredient(d, md);
        r.setIngredient(e, me);
        r.setIngredient(f, mf);
        r.setIngredient(g, mg);
        getServer().addRecipe(r);
    }

    private void safeLoadConfig() {
        try {
            saveDefaultConfig();
            reloadConfig();
            fixMojibakeConfig();
        } catch (Exception e) {
            getLogger().warning("Invalid config.yml detected, resetting to default.");
            File dataFolder = getDataFolder();
            File cfg = new File(dataFolder, "config.yml");
            if (cfg.exists()) {
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                File broken = new File(dataFolder, "config.yml.broken-" + ts);
                boolean renamed = cfg.renameTo(broken);
                if (!renamed) {
                    cfg.delete();
                }
            }
            saveDefaultConfig();
            reloadConfig();
            fixMojibakeConfig();
        }
    }

    private void fixMojibakeConfig() {
        try {
            File cfg = new File(getDataFolder(), "config.yml");
            if (!cfg.exists()) {
                return;
            }
            String text = java.nio.file.Files.readString(cfg.toPath(), StandardCharsets.UTF_8);
            String fixed = fixMojibake(text);
            if (!fixed.equals(text)) {
                java.nio.file.Files.writeString(cfg.toPath(), fixed, StandardCharsets.UTF_8);
                reloadConfig();
                getLogger().info("Fixed mojibake in config.yml");
            }
        } catch (Exception e) {
            getLogger().warning("Failed to fix config.yml encoding: " + e.getMessage());
        }
    }

    private String fixMojibake(String text) {
        String converted = new String(text.getBytes(Charset.forName("Windows-1251")), StandardCharsets.UTF_8);
        if (isMojibake(text, converted)) {
            return converted;
        }
        return text;
    }

    private boolean isMojibake(String original, String converted) {
        return countBad(converted) < countBad(original);
    }

    private int countBad(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u0420' || c == '\u0421') {
                count++;
            }
        }
        return count;
    }

    private void registerFallbackCommand(String name,
                                         org.bukkit.command.CommandExecutor executor,
                                         TabCompleter tabCompleter,
                                         String description,
                                         String usage,
                                         String permission,
                                         String permissionMessage) {
        try {
            CommandMap map = getCommandMap();
            if (map == null) {
                getLogger().warning("CommandMap not found, cannot register /" + name);
                return;
            }
            Constructor<PluginCommand> ctor = PluginCommand.class.getDeclaredConstructor(String.class, JavaPlugin.class);
            ctor.setAccessible(true);
            PluginCommand cmd = ctor.newInstance(name, this);
            cmd.setExecutor(executor);
            if (tabCompleter != null) {
                cmd.setTabCompleter(tabCompleter);
            }
            if (description != null) {
                cmd.setDescription(description);
            }
            if (usage != null) {
                cmd.setUsage(usage);
            }
            if (permission != null) {
                cmd.setPermission(permission);
            }
            if (permissionMessage != null) {
                cmd.setPermissionMessage(permissionMessage);
            }
            map.register(getDescription().getName(), cmd);
            getLogger().warning("Registered fallback command /" + name);
        } catch (Exception ex) {
            getLogger().severe("Failed to register fallback command /" + name + ": " + ex.getMessage());
        }
    }

    private void bindCommand(String name,
                             CommandExecutor exec,
                             TabCompleter tab,
                             String desc,
                             String usage,
                             String perm,
                             String permMsg) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(exec);
            if (tab != null) {
                cmd.setTabCompleter(tab);
            }
            if (desc != null) {
                cmd.setDescription(desc);
            }
            if (usage != null) {
                cmd.setUsage(usage);
            }
            if (perm != null) {
                cmd.setPermission(perm);
            }
            if (permMsg != null) {
                cmd.setPermissionMessage(permMsg);
            }
            return;
        }
        registerFallbackCommand(name, exec, tab, desc, usage, perm, permMsg);
    }

    private CommandMap getCommandMap() {
        try {
            Field f = getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            return (CommandMap) f.get(getServer());
        } catch (Exception e) {
            return null;
        }
    }
}
