package me.dominus.faivents.command;

import me.dominus.faivents.events.EventManager;
import me.dominus.faivents.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventCommand implements CommandExecutor, TabCompleter {

    private final EventManager eventManager;

    public EventCommand(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("faivents.admin")) {
            Msg.send(sender, "&c\u0423 \u0432\u0430\u0441 \u043D\u0435\u0442 \u043F\u0440\u0430\u0432.");
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        String type = args[0].toLowerCase();
        String action = args[1].toLowerCase();

        if ("meteor".equals(type) && !"start".equals(action)) {
            Msg.send(sender, "&e\u0414\u043B\u044F \u043C\u0435\u0442\u0435\u043E\u0440\u0430 \u0434\u043E\u0441\u0442\u0443\u043F\u043D\u0430 \u0442\u043E\u043B\u044C\u043A\u043E \u043A\u043E\u043C\u0430\u043D\u0434\u0430 start.");
            return true;
        }

        switch (action) {
            case "start":
                eventManager.startEvent(sender, type, args);
                return true;
            case "stop":
                eventManager.stopEvent(sender, type);
                return true;
            case "status":
                eventManager.statusEvent(sender, type);
                return true;
            default:
                Msg.send(sender, "&c\u041D\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043D\u043E\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435.");
                sendHelp(sender);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("ufo", "meteor", "glitch", "horror", "artifact"), args[0]);
        }
        if (args.length == 2) {
            if ("meteor".equalsIgnoreCase(args[0])) {
                return filterPrefix(List.of("start"), args[1]);
            }
            return filterPrefix(Arrays.asList("start", "stop", "status"), args[1]);
        }
        if (args.length == 3 && "start".equalsIgnoreCase(args[1])) {
            List<String> names = new ArrayList<>();
            names.add("all");
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return filterPrefix(names, args[2]);
        }
        return new ArrayList<>();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        List<String> out = new ArrayList<>();
        for (String s : list) {
            if (s.startsWith(prefix.toLowerCase())) {
                out.add(s);
            }
        }
        return out;
    }

    private void sendHelp(CommandSender sender) {
        Msg.send(sender, "&e\u041A\u043E\u043C\u0430\u043D\u0434\u0430: /event <ufo|meteor|glitch|horror|artifact> <start|stop|status> [\u043D\u0438\u043A|all]");
        Msg.send(sender, "&7\u041F\u0440\u0438\u043C\u0435\u0440: &f/event ufo start Player &7(\u0437\u0430\u0431\u0440\u0430\u0442\u044C \u0438\u0433\u0440\u043E\u043A\u0430)");
        Msg.send(sender, "&7\u041F\u0440\u0438\u043C\u0435\u0440: &f/event meteor start Player &7(\u043C\u0435\u0442\u0435\u043E\u0440 \u043F\u043E \u0438\u0433\u0440\u043E\u043A\u0443)");
        Msg.send(sender, "&7\u041F\u0440\u0438\u043C\u0435\u0440: &f/event glitch start all &7(\u0438\u0441\u043A\u0430\u0436\u0435\u043D\u0438\u0435 \u0434\u043B\u044F \u0432\u0441\u0435\u0445)");
    }
}