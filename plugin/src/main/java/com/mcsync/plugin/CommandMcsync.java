package com.mcsync.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

public class CommandMcsync implements CommandExecutor {
    private final mcsync plugin;

    public CommandMcsync(mcsync plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration config = plugin.getConfig();
        String prefix = ChatColor.LIGHT_PURPLE + "[" + ChatColor.BLUE + "MCSYNC" + ChatColor.LIGHT_PURPLE + "] ";

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "----------------- Help -----------------");
            sender.sendMessage(ChatColor.YELLOW + "/mcsync set <key> - Set server token");
            sender.sendMessage(ChatColor.YELLOW + "/mcsync get - Show your server token");
            sender.sendMessage(ChatColor.YELLOW + "/mcsync reload - Reload the config");
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "---------------------------------------");
        } else if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                sender.sendMessage(prefix + ChatColor.RED + "Please provide a server key.");
            } else {
                config.set("token", args[1]);
                plugin.saveConfig();
                sender.sendMessage(prefix + ChatColor.AQUA + "Server key set to: " + ChatColor.GREEN + args[1]);
            }
        } else if (args[0].equalsIgnoreCase("get")) {
            String token = config.getString("token", "Not set");
            sender.sendMessage(prefix + ChatColor.AQUA + "Your server key is: " + ChatColor.GREEN + token);
        } else if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(prefix + ChatColor.AQUA + "Configuration reloaded!");
        } else {
            sender.sendMessage(prefix + ChatColor.RED + "Unknown command. Use /mcsync help.");
        }
        return true;
    }
}
