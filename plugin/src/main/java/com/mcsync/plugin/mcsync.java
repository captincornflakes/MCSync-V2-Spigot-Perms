package com.mcsync.plugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class mcsync extends JavaPlugin {
    private FileConfiguration config;


    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig();
        config = getConfig();

        int pluginId = 24033;
        @SuppressWarnings("unused")
        Metrics metrics = new Metrics(this, pluginId);
        if (metrics != null){
            getLogger().info("MCSync has Bstats enabled!");
        }
        // Register events
        Plugin luckPermsPlugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (luckPermsPlugin != null && luckPermsPlugin.isEnabled()) {
            // LuckPerms is present and enabled
            getServer().getPluginManager().registerEvents(new LuckPermsAccess(this), this);
            getLogger().info("Loaded with LuckPerms support. MCSync is controlling user group mangement when they join.");
        } else {
            // LuckPerms is not present; load a different class
            getServer().getPluginManager().registerEvents(new RegularAccess(this), this);
            getLogger().info("LuckPerms not found. MCSync will only handle Access Control to your server.");
        }

        // Register commands
        if (this.getCommand("mcsync") != null) {
            this.getCommand("mcsync").setExecutor(new CommandMcsync(this));
        } else {
            getLogger().severe("Failed to register command: mcsync");
        }

        getLogger().info("MCSync has been enabled!");


    }

    @Override
    public void onDisable() {
        getLogger().info("MCSync has been disabled.");
    }
    public FileConfiguration getPluginConfig() {
        return config;
    }

}
