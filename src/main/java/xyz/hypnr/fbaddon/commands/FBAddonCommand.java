package xyz.hypnr.fbaddon.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.hypnr.fbaddon.DirectionalFBAddon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FBAddonCommand implements CommandExecutor, TabCompleter {

    private final DirectionalFBAddon plugin;

    public FBAddonCommand(DirectionalFBAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fbaddon.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§e/fbaddon reload §7- reload config");
            sender.sendMessage("§e/fbaddon enable §7- enable directional knockback");
            sender.sendMessage("§e/fbaddon disable §7- disable directional knockback");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.reloadAddonConfig();
                sender.sendMessage("§aConfig reloaded.");
                return true;
            case "enable":
                plugin.setFeatureEnabled(true);
                sender.sendMessage("§aDirectional knockback enabled.");
                return true;
            case "disable":
                plugin.setFeatureEnabled(false);
                sender.sendMessage("§cDirectional knockback disabled.");
                return true;
            default:
                sender.sendMessage("§cUnknown subcommand.");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("fbaddon.admin")) return new ArrayList<>();
        if (args.length == 1) {
            return Arrays.asList("reload", "enable", "disable");
        }
        return new ArrayList<>();
    }
}
