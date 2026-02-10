package me.dominus.logcleaner;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LogCleanerPlugin extends JavaPlugin {
    private BukkitTask scheduledTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registerCommand();
        scheduleCleanup();
    }

    @Override
    public void onDisable() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
    }

    private void registerCommand() {
        PluginCommand cmd = getCommand("logclean");
        if (cmd != null) {
            cmd.setExecutor(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("logcleaner.admin")) {
            sender.sendMessage("§cНет прав.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§eИспользование: /logclean <status|now|reload>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "status" -> {
                long periodTicks = getPeriodTicks();
                if (periodTicks <= 0) {
                    sender.sendMessage("§eАвтоочистка выключена.");
                } else {
                    sender.sendMessage("§aАвтоочистка включена. Интервал: " + formatPeriod(periodTicks));
                }
                sender.sendMessage("§akeep_days: " + getKeepDays());
                return true;
            }
            case "now" -> {
                runCleanupAsync(() -> sender.sendMessage("§aОчистка завершена."));
                sender.sendMessage("§eОчистка запущена...");
                return true;
            }
            case "reload" -> {
                reloadConfig();
                scheduleCleanup();
                sender.sendMessage("§aКонфигурация перезагружена.");
                return true;
            }
            default -> {
                sender.sendMessage("§eИспользование: /logclean <status|now|reload>");
                return true;
            }
        }
    }

    private void scheduleCleanup() {
        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }

        long periodTicks = getPeriodTicks();
        if (periodTicks <= 0) {
            return;
        }

        long initialDelay = Math.max(20L, periodTicks);
        scheduledTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::cleanupNow, initialDelay, periodTicks);
    }

    private void runCleanupAsync(Runnable onDone) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            cleanupNow();
            if (onDone != null) {
                Bukkit.getScheduler().runTask(this, onDone);
            }
        });
    }

    private void cleanupNow() {
        try {
            Path serverRoot = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
            Path logsDir = serverRoot.resolve("logs").toAbsolutePath().normalize();

            if (!logsDir.startsWith(serverRoot) || !Files.isDirectory(logsDir)) {
                return;
            }

            int keepDays = getKeepDays();
            Instant cutoff = Instant.now().minus(Duration.ofDays(keepDays));

            // First pass: delete old log files
            Files.walkFileTree(logsDir, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!file.startsWith(serverRoot)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
                    boolean isLog = name.endsWith(".log") || name.endsWith(".log.gz");
                    if (!isLog) {
                        return FileVisitResult.CONTINUE;
                    }

                    try {
                        Instant modified = Files.getLastModifiedTime(file).toInstant();
                        if (modified.isBefore(cutoff)) {
                            Files.deleteIfExists(file);
                        }
                    } catch (IOException ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            // Second pass: delete empty directories inside logs
            Files.walkFileTree(logsDir, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    if (dir.equals(logsDir)) {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!dir.startsWith(serverRoot)) {
                        return FileVisitResult.CONTINUE;
                    }
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                        if (!stream.iterator().hasNext()) {
                            Files.deleteIfExists(dir);
                        }
                    } catch (IOException ignored) {
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception ignored) {
        }
    }

    private long getPeriodTicks() {
        FileConfiguration cfg = getConfig();
        int minutes = cfg.getInt("every_minutes", 0);
        int hours = cfg.getInt("every_hours", 0);

        long totalMinutes = minutes > 0 ? minutes : (hours > 0 ? hours * 60L : 0L);
        if (totalMinutes <= 0) {
            return 0L;
        }
        return TimeUnit.MINUTES.toSeconds(totalMinutes) * 20L;
    }

    private int getKeepDays() {
        int keep = getConfig().getInt("keep_days", 7);
        return Math.max(0, keep);
    }

    private String formatPeriod(long ticks) {
        long seconds = ticks / 20L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        if (hours > 0) {
            return hours + " ч" + (minutes % 60L > 0 ? " " + (minutes % 60L) + " мин" : "");
        }
        return minutes + " мин";
    }
}
