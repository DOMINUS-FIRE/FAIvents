package me.dominus.faivents.command;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.dominus.faivents.quarry.QuarryListener;
import me.dominus.faivents.quarry.QuarryManager;
import me.dominus.faivents.quarry.QuarryManager.QuarryData;
import me.dominus.faivents.util.Msg;

public class ChaseCommand implements CommandExecutor {

    private final QuarryManager quarryManager;
    private final QuarryListener quarryListener;

    public ChaseCommand(QuarryManager quarryManager, QuarryListener quarryListener) {
        this.quarryManager = quarryManager;
        this.quarryListener = quarryListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Msg.send(sender, "&c\u041A\u043E\u043C\u0430\u043D\u0434\u0430 \u0442\u043E\u043B\u044C\u043A\u043E \u0434\u043B\u044F \u0438\u0433\u0440\u043E\u043A\u043E\u0432.");
            return true;
        }
        Player player = (Player) sender;
        UUID id = player.getUniqueId();
        Location loc = quarryListener.findQuarryByOwner(id, 5);
        if (loc == null) {
            Msg.send(player, "&c\u0423 \u0442\u0435\u0431\u044F \u043D\u0435\u0442 \u043A\u0430\u0440\u044C\u0435\u0440\u0430 5 \u0443\u0440\u043E\u0432\u043D\u044F.");
            return true;
        }
        QuarryData qd = quarryManager.getData(loc.getBlock());
        if (qd == null) {
            Msg.send(player, "&c\u041A\u0430\u0440\u044C\u0435\u0440\u0430 \u043D\u0435\u0442 \u0432 \u043F\u0430\u043C\u044F\u0442\u0438, \u043F\u043E\u0441\u0442\u0430\u0432\u044C \u0435\u0433\u043E \u0437\u0430\u043D\u043E\u0432\u043E.");
            return true;
        }
        quarryListener.openRemoteStorage(player, loc.getBlock());
        return true;
    }
}
