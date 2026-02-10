package me.dominus.faivents.events;

import me.dominus.faivents.FAIventsPlugin;
import me.dominus.faivents.events.artifact.ArtifactHuntEvent;
import me.dominus.faivents.events.glitch.RealityGlitchEvent;
import me.dominus.faivents.events.horror.HorrorNightEvent;
import me.dominus.faivents.events.meteor.MeteorEvent;
import me.dominus.faivents.events.ufo.UfoAbductionEvent;
import me.dominus.faivents.util.Msg;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventManager {

    public interface EventController {
        String getId();

        boolean isEnabled();

        boolean start(CommandSender sender, String[] args);

        void stop(CommandSender sender);

        void status(CommandSender sender);

        void reload();

        boolean isRunning();
    }

    private final FAIventsPlugin plugin;
    private final Map<String, EventController> events = new HashMap<>();
    private final Set<String> running = new HashSet<>();

    public EventManager(FAIventsPlugin plugin) {
        this.plugin = plugin;
        register(new UfoAbductionEvent(plugin));
        register(new MeteorEvent(plugin));
        register(new RealityGlitchEvent(plugin));
        register(new HorrorNightEvent(plugin));
        register(new ArtifactHuntEvent(plugin));
    }

    private void register(EventController controller) {
        events.put(controller.getId(), controller);
        if (controller instanceof Listener) {
            plugin.getServer().getPluginManager().registerEvents((Listener) controller, plugin);
        }
    }

    public void startEvent(CommandSender sender, String id, String[] args) {
        EventController controller = events.get(id);
        if (controller == null) {
            Msg.send(sender, "&c\u0420\u045C\u0420\u00B5\u0420\u0451\u0420\u00B7\u0420\u0406\u0420\u00B5\u0421\u0403\u0421\u201A\u0420\u0405\u0421\u2039\u0420\u2116 \u0420\u0451\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A.");
            return;
        }
        if (!controller.isEnabled()) {
            Msg.send(sender, "&e\u0420\u00AD\u0421\u201A\u0420\u0455\u0421\u201A \u0420\u0451\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A \u0420\u0455\u0421\u201A\u0420\u0454\u0420\u00BB\u0421\u040B\u0421\u2021\u0420\u00B5\u0420\u0405 \u0420\u0406 \u0420\u0454\u0420\u0455\u0420\u0405\u0421\u201E\u0420\u0451\u0420\u0456\u0420\u00B5.");
            return;
        }
        boolean allowParallel = plugin.getConfig().getBoolean("allow_parallel_events", false);
        if (!allowParallel && !running.isEmpty() && !running.contains(id)) {
            Msg.send(sender, "&c\u0420\u040E\u0420\u00B5\u0420\u2116\u0421\u2021\u0420\u00B0\u0421\u0403 \u0421\u0453\u0420\u00B6\u0420\u00B5 \u0420\u00B7\u0420\u00B0\u0420\u0457\u0421\u0453\u0421\u2030\u0420\u00B5\u0420\u0405 \u0420\u0491\u0421\u0402\u0421\u0453\u0420\u0456\u0420\u0455\u0420\u2116 \u0420\u0451\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A. \u0420\u045B\u0421\u0403\u0421\u201A\u0420\u00B0\u0420\u0405\u0420\u0455\u0420\u0406\u0420\u0451\u0421\u201A\u0420\u00B5 \u0420\u00B5\u0420\u0456\u0420\u0455 \u0420\u0451\u0420\u00BB\u0420\u0451 \u0420\u0406\u0420\u0454\u0420\u00BB\u0421\u040B\u0421\u2021\u0420\u0451\u0421\u201A\u0420\u00B5 \u0420\u0457\u0420\u00B0\u0421\u0402\u0420\u00B0\u0420\u00BB\u0420\u00BB\u0420\u00B5\u0420\u00BB\u0421\u040A\u0420\u0405\u0420\u0455\u0421\u0403\u0421\u201A\u0421\u040A \u0420\u0406 \u0420\u0454\u0420\u0455\u0420\u0405\u0421\u201E\u0420\u0451\u0420\u0456\u0420\u00B5.");
            return;
        }
        if (controller.isRunning()) {
            Msg.send(sender, "&e\u0420\u0098\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A \u0421\u0453\u0420\u00B6\u0420\u00B5 \u0420\u00B7\u0420\u00B0\u0420\u0457\u0421\u0453\u0421\u2030\u0420\u00B5\u0420\u0405.");
            return;
        }
        boolean ok = controller.start(sender, args);
        if (ok) {
            running.add(id);
        }
    }

    public void stopEvent(CommandSender sender, String id) {
        EventController controller = events.get(id);
        if (controller == null) {
            Msg.send(sender, "&c\u0420\u045C\u0420\u00B5\u0420\u0451\u0420\u00B7\u0420\u0406\u0420\u00B5\u0421\u0403\u0421\u201A\u0420\u0405\u0421\u2039\u0420\u2116 \u0420\u0451\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A.");
            return;
        }
        if (!controller.isRunning()) {
            Msg.send(sender, "&e\u0420\u0098\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A \u0420\u0405\u0420\u00B5 \u0420\u00B7\u0420\u00B0\u0420\u0457\u0421\u0453\u0421\u2030\u0420\u00B5\u0420\u0405.");
            return;
        }
        controller.stop(sender);
        running.remove(id);
    }

    public void statusEvent(CommandSender sender, String id) {
        EventController controller = events.get(id);
        if (controller == null) {
            Msg.send(sender, "&c\u0420\u045C\u0420\u00B5\u0420\u0451\u0420\u00B7\u0420\u0406\u0420\u00B5\u0421\u0403\u0421\u201A\u0420\u0405\u0421\u2039\u0420\u2116 \u0420\u0451\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A.");
            return;
        }
        controller.status(sender);
    }

    public void reloadEvent(CommandSender sender, String id) {
        if (!events.containsKey(id)) {
            Msg.send(sender, "&c\u0420\u045C\u0420\u00B5\u0420\u0451\u0420\u00B7\u0420\u0406\u0420\u00B5\u0421\u0403\u0421\u201A\u0420\u0405\u0421\u2039\u0420\u2116 \u0420\u0451\u0420\u0406\u0420\u00B5\u0420\u0405\u0421\u201A.");
            return;
        }
        plugin.reloadConfig();
        Msg.init(plugin);
        for (EventController controller : events.values()) {
            controller.reload();
        }
        Msg.send(sender, "&a\u0420\u0459\u0420\u0455\u0420\u0405\u0421\u201E\u0420\u0451\u0420\u0456 \u0420\u0457\u0420\u00B5\u0421\u0402\u0420\u00B5\u0420\u00B7\u0420\u00B0\u0420\u0456\u0421\u0402\u0421\u0453\u0420\u00B6\u0420\u00B5\u0420\u0405.");
    }

    public void shutdown() {
        for (EventController controller : events.values()) {
            if (controller.isRunning()) {
                controller.stop(null);
            }
        }
        running.clear();
    }

    public void onEventStopped(String id) {
        running.remove(id);
    }

    public FAIventsPlugin getPlugin() {
        return plugin;
    }
}