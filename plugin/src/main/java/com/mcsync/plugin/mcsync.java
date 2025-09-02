package com.mcsync.plugin;

import org.bukkit.configuration.file.FileConfiguration;
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

        // Check if Discord Webhook is enabled.
        if (config.getBoolean("discordWebhookEnabled")){
            getServer().getPluginManager().registerEvents(new chatToDiscord(this), this);
        }
        
        // Register events
        // Register LoginGate for login/auth/permissions
        getServer().getPluginManager().registerEvents(new AsyncLoginGate(this), this);
        getLogger().info("LoginGate loaded: MCSync is controlling user authentication and permissions on login.");

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
