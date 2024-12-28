package com.mcsync.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class mcsync extends JavaPlugin {
    private LuckPerms luckPerms;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        // Load configuration
        saveDefaultConfig();
        config = getConfig();

        // Initialize LuckPerms
        try {
            luckPerms = LuckPermsProvider.get();
            getLogger().info("LuckPerms integration successful.");
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms is not available. Some functionality may be limited.");
        }

        // Register events and commands
        getServer().getPluginManager().registerEvents(new McsyncEventListener(this), this);
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

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    public FileConfiguration getPluginConfig() {
        return config;
    }
}
