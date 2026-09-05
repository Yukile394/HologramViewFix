package com.yukile.hologramviewfix.command;

import com.yukile.hologramviewfix.HologramViewFix;
import com.yukile.hologramviewfix.hologram.HologramTracker;
import com.yukile.hologramviewfix.integration.HologramIntegration;
import com.yukile.hologramviewfix.util.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles /hologramviewfix and its subcommands: reload, status, scan, debug.
 */
public final class HologramViewFixCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "status", "scan", "debug");

    private final HologramViewFix plugin;

    public HologramViewFixCommand(HologramViewFix plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "status" -> handleStatus(sender);
            case "scan" -> handleScan(sender);
            case "debug" -> handleDebug(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("hologramviewfix.reload")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(Component.text("HologramViewFix configuration reloaded.", NamedTextColor.GREEN));
    }

    private void handleStatus(CommandSender sender) {
        if (!sender.hasPermission("hologramviewfix.status")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        ConfigManager cfg = plugin.getConfigManager();
        sender.sendMessage(Component.text("HologramViewFix Status", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("----------------------", NamedTextColor.GRAY));
        sender.sendMessage(line("Status", cfg.isEnabled() ? "Enabled" : "Disabled"));
        sender.sendMessage(line("Server", plugin.getServer().getVersion()));
        sender.sendMessage(line("Java", System.getProperty("java.version")));
        sender.sendMessage(Component.empty());

        for (HologramIntegration integration : plugin.getIntegrations()) {
            String state = integration.isAvailable() ? "Hooked" : "Not Found";
            sender.sendMessage(line(integration.getName(), state));
        }

        sender.sendMessage(Component.empty());
        sender.sendMessage(line("Tracked Holograms", String.valueOf(plugin.getHologramCache().size())));
        sender.sendMessage(line("View Distance", String.valueOf(cfg.getViewDistance())));
        sender.sendMessage(line("Check Interval", cfg.getCheckInterval() + " ticks"));
    }

    private Component line(String key, String value) {
        return Component.text(key + ": ", NamedTextColor.GRAY)
                .append(Component.text(value, NamedTextColor.WHITE));
    }

    private void handleScan(CommandSender sender) {
        if (!sender.hasPermission("hologramviewfix.scan")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can run a scan centered on their location.", NamedTextColor.RED));
            return;
        }

        HologramTracker tracker = plugin.getHologramTracker();
        int radius = 3;
        Chunk center = player.getLocation().getChunk();
        int found = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Chunk chunk = player.getWorld().getChunkAt(center.getX() + dx, center.getZ() + dz);
                if (chunk.isLoaded()) {
                    int before = tracker.getCache().size();
                    tracker.scanChunk(chunk);
                    found += tracker.getCache().size() - before;
                }
            }
        }
        sender.sendMessage(Component.text("Scan complete. Newly detected holograms: " + found, NamedTextColor.GREEN));
    }

    private void handleDebug(CommandSender sender) {
        if (!sender.hasPermission("hologramviewfix.debug")) {
            sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
            return;
        }
        boolean newState = !plugin.getConfig().getBoolean("debug", false);
        plugin.getConfig().set("debug", newState);
        plugin.saveConfig();
        plugin.reloadPlugin();
        sender.sendMessage(Component.text("Debug mode is now " + (newState ? "ON" : "OFF"), NamedTextColor.YELLOW));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /hologramviewfix <reload|status|scan|debug>", NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> results = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    results.add(sub);
                }
            }
            return results;
        }
        return List.of();
    }
}
