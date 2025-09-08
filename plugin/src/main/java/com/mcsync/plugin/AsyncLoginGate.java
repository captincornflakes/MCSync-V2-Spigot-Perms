package com.mcsync.plugin;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent.Result;
import org.json.JSONObject;

public class AsyncLoginGate implements Listener {
    private final java.util.Set<UUID> toKick = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private final mcsync plugin;
    private final net.luckperms.api.LuckPerms luckPerms;
    private final boolean luckPermsEnabled;
    private final PermissionAssigner permissionAssigner;

    public AsyncLoginGate(mcsync plugin) {
        this.plugin = plugin;
        org.bukkit.plugin.Plugin lpPlugin = org.bukkit.Bukkit.getPluginManager().getPlugin("LuckPerms");
        this.luckPermsEnabled = lpPlugin != null && lpPlugin.isEnabled();
        this.luckPerms = luckPermsEnabled ? net.luckperms.api.LuckPermsProvider.get() : null;
        this.permissionAssigner = luckPermsEnabled ? new PermissionAssigner(this.luckPerms, plugin.getConfig()) : null;
    }

    private org.bukkit.configuration.file.FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
    UUID uuid = event.getUniqueId();
    String playerName = event.getName();
    String token = getConfig().getString("token");
    String parametersRaw = getConfig().getString("parameters", "");
    String parameters = parametersRaw == null ? "" : parametersRaw.toLowerCase();
    String failMessageRaw = getConfig().getString("fail_message", "You are not authorized to join this server.");
    String failMessage = ChatColor.translateAlternateColorCodes('&', failMessageRaw == null ? "You are not authorized to join this server." : failMessageRaw);
    if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] PreLogin called for %s UUID: %s", playerName, uuid));

        // Move all logic inside async
        boolean isWhitelisted = getServer().getWhitelistedPlayers().stream().anyMatch(whitelistedPlayer -> whitelistedPlayer.getUniqueId().equals(uuid));
        boolean authorizePlayer = false;
        int tier = 0;
        if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Async thread started for %s UUID: %s", playerName, uuid));
        if (isWhitelisted) {
            authorizePlayer = true;
            tier = 0;
            if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] User is Whitelisted: %s", playerName));
        } else {
            try {
                if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Calling Auth.check for %s UUID: %s", playerName, uuid));
                String authResult = Auth.check(token, uuid.toString().replace("-", ""), parameters);
                if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Auth.check result for %s: %s", playerName, authResult));
                JSONObject resultObj = new JSONObject(authResult);
                authorizePlayer = resultObj.optBoolean("authorize", false);
                tier = resultObj.optInt("tier", 0);
                if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Authorize: %s finalTier: %d", authorizePlayer, tier));
            } catch (org.json.JSONException | NullPointerException e) {
                if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().warning(String.format("[AsyncLoginGate][DEBUG] Failed to parse auth result: %s", e.getMessage()));
            } catch (Exception e) {
                if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().warning(String.format("[AsyncLoginGate][DEBUG] Unexpected error: %s", e.getMessage()));
            }
        }
        if (!authorizePlayer) {
            if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Kicking %s UUID: %s (pre-login)", playerName, uuid));
            event.disallow(Result.KICK_OTHER, failMessage);
            toKick.add(uuid);
            if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Added %s to toKick set", uuid));
        } else {
            // If authorized, ensure they're not in the toKick set
            boolean removed = toKick.remove(uuid);
            if (removed && parameters.contains("debug")) {
                org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Removed %s from toKick set after successful auth", uuid));
            }
            if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info("[AsyncLoginGate][DEBUG] Called Final Perm set");
            event.allow();
            if (luckPermsEnabled && permissionAssigner != null) {
                final int finalTier = tier;
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (parameters.contains("debug")) org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Assigning permissions to %s UUID: %s tier: %d", playerName, uuid, finalTier));
                    permissionAssigner.assignPermissions(playerName, uuid, parameters, finalTier);
                });
            }
        }
    }

    private org.bukkit.Server getServer() {
        return org.bukkit.Bukkit.getServer();
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getName();
        String parametersRaw = getConfig().getString("parameters", "");
        String parameters = parametersRaw == null ? "" : parametersRaw.toLowerCase();
        if (parameters.contains("debug")) {
            org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] PlayerJoin event for %s UUID: %s", playerName, uuid));
        }
        boolean shouldKick = toKick.contains(uuid);
        // Also check by player name (case-insensitive)
        if (!shouldKick) {
            for (UUID kickUuid : toKick) {
                org.bukkit.entity.Player kickPlayer = org.bukkit.Bukkit.getPlayer(kickUuid);
                if (kickPlayer != null && kickPlayer.getName().equalsIgnoreCase(playerName)) {
                    shouldKick = true;
                    uuid = kickUuid; // Use the UUID from the toKick set for removal
                    if (parameters.contains("debug")) {
                        org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Found player by name in toKick: %s UUID: %s", playerName, uuid));
                    }
                    break;
                }
            }
        }
        if (shouldKick) {
            if (parameters.contains("debug")) {
                org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Kicking post-login: %s UUID: %s", playerName, uuid));
            }
            String failMessageRaw = getConfig().getString("fail_message", "You are not authorized to join this server.");
            String failMessage = ChatColor.translateAlternateColorCodes('&', failMessageRaw == null ? "You are not authorized to join this server." : failMessageRaw);
            event.getPlayer().kickPlayer(failMessage);
            toKick.remove(uuid);
            if (parameters.contains("debug")) {
                org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Removed %s from toKick set after kick", uuid));
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String parametersRaw = getConfig().getString("parameters", "");
        String parameters = parametersRaw == null ? "" : parametersRaw.toLowerCase();
        if (toKick.remove(uuid)) {
            if (parameters.contains("debug")) {
                org.bukkit.Bukkit.getLogger().info(String.format("[AsyncLoginGate][DEBUG] Removed %s from toKick set on disconnect", uuid));
            }
        }
    }
}
