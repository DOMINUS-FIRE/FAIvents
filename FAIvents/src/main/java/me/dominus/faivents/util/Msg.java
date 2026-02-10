package me.dominus.faivents.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class Msg {

    private static String prefix = "";

    private Msg() {
    }

    public static void init(JavaPlugin plugin) {
        prefix = color(plugin.getConfig().getString("prefix", "&6[FAIvents]&r "));
    }

    public static void send(CommandSender sender, String msg) {
        sender.sendMessage(prefix + color(msg));
    }

    public static String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}

