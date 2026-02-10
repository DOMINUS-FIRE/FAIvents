package me.dominus.faivents.command;

import me.dominus.faivents.util.InvisManager;
import me.dominus.faivents.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InvCommand implements CommandExecutor {

    private final InvisManager invisManager;

    public InvCommand(InvisManager invisManager) {
        this.invisManager = invisManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Msg.send(sender, "&c\u041A\u043E\u043C\u0430\u043D\u0434\u0430 \u0442\u043E\u043B\u044C\u043A\u043E \u0434\u043B\u044F \u0438\u0433\u0440\u043E\u043A\u0430.");
            return true;
        }
        Player player = (Player) sender;
        if (invisManager.hasSource(player, InvisManager.Source.COMMAND)) {
            invisManager.removeSource(player, InvisManager.Source.COMMAND);
            Msg.send(player, "&e\u041D\u0435\u0432\u0438\u0434\u0438\u043C\u043E\u0441\u0442\u044C \u0432\u044B\u043A\u043B\u044E\u0447\u0435\u043D\u0430.");
        } else {
            invisManager.addSource(player, InvisManager.Source.COMMAND, 0L);
            Msg.send(player, "&a\u041D\u0435\u0432\u0438\u0434\u0438\u043C\u043E\u0441\u0442\u044C \u0432\u043A\u043B\u044E\u0447\u0435\u043D\u0430.");
        }
        return true;
    }
}
