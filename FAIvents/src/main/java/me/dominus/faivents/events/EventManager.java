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
            Msg.send(sender, "&c\u041D\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043D\u044B\u0439 \u0438\u0432\u0435\u043D\u0442.");
            return;
        }
        if (!controller.isEnabled()) {
            Msg.send(sender, "&e\u042D\u0442\u043E\u0442 \u0438\u0432\u0435\u043D\u0442 \u043E\u0442\u043A\u043B\u044E\u0447\u0435\u043D \u0432 \u043A\u043E\u043D\u0444\u0438\u0433\u0435.");
            return;
        }
        boolean allowParallel = plugin.getConfig().getBoolean("allow_parallel_events", false);
        if (!allowParallel && !running.isEmpty() && !running.contains(id)) {
            Msg.send(sender, "&c\u0421\u0435\u0439\u0447\u0430\u0441 \u0443\u0436\u0435 \u0437\u0430\u043F\u0443\u0449\u0435\u043D \u0434\u0440\u0443\u0433\u043E\u0439 \u0438\u0432\u0435\u043D\u0442. \u041E\u0441\u0442\u0430\u043D\u043E\u0432\u0438\u0442\u0435 \u0435\u0433\u043E \u0438\u043B\u0438 \u0432\u043A\u043B\u044E\u0447\u0438\u0442\u0435 \u043F\u0430\u0440\u0430\u043B\u043B\u0435\u043B\u044C\u043D\u043E\u0441\u0442\u044C \u0432 \u043A\u043E\u043D\u0444\u0438\u0433\u0435.");
            return;
        }
        if (controller.isRunning()) {
            Msg.send(sender, "&e\u0418\u0432\u0435\u043D\u0442 \u0443\u0436\u0435 \u0437\u0430\u043F\u0443\u0449\u0435\u043D.");
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
            Msg.send(sender, "&c\u041D\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043D\u044B\u0439 \u0438\u0432\u0435\u043D\u0442.");
            return;
        }
        if (!controller.isRunning()) {
            Msg.send(sender, "&e\u0418\u0432\u0435\u043D\u0442 \u043D\u0435 \u0437\u0430\u043F\u0443\u0449\u0435\u043D.");
            return;
        }
        controller.stop(sender);
        running.remove(id);
    }

    public void statusEvent(CommandSender sender, String id) {
        EventController controller = events.get(id);
        if (controller == null) {
            Msg.send(sender, "&c\u041D\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043D\u044B\u0439 \u0438\u0432\u0435\u043D\u0442.");
            return;
        }
        controller.status(sender);
    }

    public void reloadEvent(CommandSender sender, String id) {
        if (!events.containsKey(id)) {
            Msg.send(sender, "&c\u041D\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043D\u044B\u0439 \u0438\u0432\u0435\u043D\u0442.");
            return;
        }
        plugin.reloadConfig();
        Msg.init(plugin);
        for (EventController controller : events.values()) {
            controller.reload();
        }
        Msg.send(sender, "&a\u041A\u043E\u043D\u0444\u0438\u0433 \u043F\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043D.");
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
