package me.dominus.faivents;

import me.dominus.faivents.command.ChaseCommand;
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
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FAIventsPlugin extends JavaPlugin {

    private EventManager eventManager;
    private InvisManager invisManager;
    private QuarryManager quarryManager;

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

        EventCommand cmd = new EventCommand(eventManager);
        if (getCommand("event") != null) {
            getCommand("event").setExecutor(cmd);
            getCommand("event").setTabCompleter(cmd);
        }
        SecretCommand secret = new SecretCommand(this);
        if (getCommand("secret") != null) {
            getCommand("secret").setExecutor(secret);
        } else {
            registerFallbackCommand(
                    "secret",
                    secret,
                    null,
                    "Secret admin menu",
                    "/secret",
                    null,
                    null
            );
        }
        getServer().getPluginManager().registerEvents(secret, this);

        if (getCommand("inv") != null) {
            getCommand("inv").setExecutor(new InvCommand(invisManager));
        }
        ShopCommand shop = new ShopCommand(this, quarryManager);
        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(shop);
        }
        getServer().getPluginManager().registerEvents(shop, this);

        if (getCommand("chase") != null) {
            getCommand("chase").setExecutor(new ChaseCommand(quarryManager, quarryListener));
        }

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
        }
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
