package com.mcsync.plugin;

import static org.bukkit.Bukkit.getLogger;
import static org.bukkit.Bukkit.getServer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;

public class RegularAccess implements Listener {
    private final mcsync plugin;
    private boolean isKicked = false;

    public RegularAccess(mcsync plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        isKicked = true;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (isKicked) {
            event.setQuitMessage(null);
            isKicked = false;
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event){
        String uuid = event.getUniqueId().toString().replace("-", "");
        String playerName = event.getName();
        String token = getConfig().getString("token");
        String parameters = getConfig().getString("parameters", "").toLowerCase();
        boolean authorizePlayer = false;
        if (parameters.contains("debug")) {
            getLogger().info("PlayerJoin: " + playerName);
            getLogger().info("Token: " + token);
            getLogger().info("UUID: " + uuid);
        }
        // Check if the player is whitelisted
        if (getServer().getWhitelistedPlayers().stream().anyMatch(whitelistedPlayer -> whitelistedPlayer.getUniqueId().equals(uuid))) {
            authorizePlayer = true;
        } 
        else {
            try {
                String authResult = Auth.check(token, uuid, parameters);
                JSONObject resultObj = new JSONObject(authResult);
                authorizePlayer = resultObj.optBoolean("authorize", false);
            } 
            catch (Exception e) {
                getLogger().warning("Failed to parse auth result: " + e.getMessage());
            }
        }
        // If the player is not authorized, deny login
        if (!authorizePlayer) {
            String failMessage = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("fail_message", "You are not authorized to join this server."));
            event.disallow(Result.KICK_OTHER, failMessage);
        }
    }
}
