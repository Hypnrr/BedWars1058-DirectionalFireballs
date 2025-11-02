package xyz.hypnr.fbaddon2;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FBAddonsCommand implements CommandExecutor, TabCompleter {

    private final DirectionalFBsPlugin plugin;

    public FBAddonsCommand(DirectionalFBsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fbaddon.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.reloadConfig();
                plugin.refreshListenerFromConfig();
                sender.sendMessage("§aDirectional fireball settings reloaded from config.");
                return true;
            case "enable":
                if (plugin.isFeatureEnabled()) {
                    sender.sendMessage("§eFireball features are already enabled.");
                } else {
                    plugin.setFeatureEnabled(true);
                    sender.sendMessage("§aFireball features enabled.");
                }
                return true;
            case "disable":
                if (!plugin.isFeatureEnabled()) {
                    sender.sendMessage("§eFireball features are already disabled.");
                } else {
                    plugin.setFeatureEnabled(false);
                    sender.sendMessage("§eFireball features disabled.");
                }
                return true;
            case "status":
                sender.sendMessage("§7Directional fireballs active: " + (plugin.isFeatureEnabled() ? "§aYes" : "§cNo"));
                return true;
            case "help":
                sendHelp(sender, label);
                return true;
            default:
                sendHelp(sender, label);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = Arrays.asList("reload", "enable", "disable", "status", "help");
            String p = args[0].toLowerCase();
            List<String> out = new ArrayList<String>();
            for (String s : base) if (s.startsWith(p)) out.add(s);
            return out;
        }
        return new ArrayList<String>();
    }

    private void sendHelp(CommandSender sender, String label) {
        String version = plugin.getDescription().getVersion();
        sender.sendMessage("§6§lDirectional Fireball Suite §7(v" + version + ") by Hypnr");
        sender.sendMessage("§eUsage: /" + label + " <reload|enable|disable|status|help>");
        sender.sendMessage(" §7• §freload §8- §7Reload config.yml and apply new tuning");
        sender.sendMessage(" §7• §fenable §8- §7Turn on directional knockback without restarting");
        sender.sendMessage(" §7• §fdisable §8- §7Temporarily pause all custom knockback");
        sender.sendMessage(" §7• §fstatus §8- §7View whether the feature is currently enabled");
        sender.sendMessage(" §7• §fhelp §8- §7Show this guide again");
    }
}
