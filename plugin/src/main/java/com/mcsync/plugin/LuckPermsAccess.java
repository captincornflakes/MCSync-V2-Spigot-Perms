package com.mcsync.plugin;

import java.util.UUID;

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

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

public class LuckPermsAccess implements Listener {
    private final mcsync plugin;
    private final LuckPerms luckPerms;
    private boolean isKicked = false;

    public LuckPermsAccess(mcsync plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
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
        UUID uuid = event.getPlayer().getUniqueId();
        LuckPerms api = LuckPermsProvider.get();
        User user = api.getUserManager().getUser(uuid);
        String permissionsMode = getConfig().getString("permissionsMode", "node").toLowerCase();
        String[] permissionNodes = {
            getConfig().getString("override"),
            getConfig().getString("sub-t1"),
            getConfig().getString("sub-t2"),
            getConfig().getString("sub-t3")
        };
        api.getUserManager().modifyUser(uuid, userMod -> {
            if (userMod == null) return;

            for (String permission : permissionNodes) {
                if (permission != null && !permission.isEmpty()) {
                    Node node = "group".equals(permissionsMode) ?
                            Node.builder("group." + permission).build() :
                            Node.builder(permission).build();
                    userMod.data().remove(node);
                }
            }

            if (getConfig().getString("parameters", "").toLowerCase().contains("debug")) {getLogger().warning("Removed permissions for user: " + uuid);}
        });
    }

    @EventHandler
    public void onPlayerJoin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        UUID uuid = event.getUniqueId();
        String token = getConfig().getString("token");
        String parameters = getConfig().getString("parameters", "").toLowerCase();
        String failMessage = ChatColor.translateAlternateColorCodes('&', getConfig().getString("fail_message", "You are not authorized to join this server."));
        if (parameters.contains("debug")) { getLogger().info("PlayerJoin: " + playerName + " Token: " + token + " UUID: " + uuid);}
        
        if (getServer().getWhitelistedPlayers().stream().anyMatch(whitelistedPlayer -> whitelistedPlayer.getUniqueId().equals(uuid))) {
            event.allow();
            assignPermissions(playerName, uuid, parameters, 0);
            if (parameters.contains("debug")) {getLogger().info("User is Whitelisted: " + playerName);}
            return;
        }

        int tier = 0;
        boolean authorizePlayer = false;
        try {
            String authResult = Auth.check(token, uuid.toString().replace("-", ""), parameters);
            JSONObject resultObj = new JSONObject(authResult);
            authorizePlayer = resultObj.optBoolean("authorizePlayer", false);
            tier = resultObj.optInt("tier", 0);
            } 
        catch (Exception e) {
            getLogger().warning("Failed to parse auth result: " + e.getMessage());
            }
        if (parameters.contains("debug")) {getLogger().info("finalAuthorizePlayer: " + authorizePlayer + " finalTier: " + tier);}
        if (!authorizePlayer) {
            if (parameters.contains("debug")) {getLogger().info("Called Final Kick");}
            event.disallow(Result.KICK_OTHER, failMessage);
            } 
        else {
            if (parameters.contains("debug")) {getLogger().info("Called Final Perm set");}
            assignPermissions(playerName, uuid, parameters, tier);
        }
    }

    private void assignPermissions(String player, UUID uuid, String parameters, int tier) {
        if (luckPerms == null) {
            if (parameters.contains("debug")) { getLogger().warning("LuckPerms is not initialized!");}
            return;
            }
        luckPerms.getUserManager().modifyUser(uuid, user -> {
            if (user == null) {getLogger().warning("Failed to load user data for UUID: " + uuid);
                return;
            }
            String permissionsMode = getConfig().getString("permissionsMode");
            String permissionNode = null;
            if (tier == 1) { permissionNode = getConfig().getString("sub-t1");} 
            else if (tier == 2) {permissionNode = getConfig().getString("sub-t2");}
            else if (tier == 3) {permissionNode = getConfig().getString("sub-t3");}
            else {permissionNode = getConfig().getString("override");}
            if (permissionNode != null) {
                Node node = "group".equalsIgnoreCase(permissionsMode) ?
                        InheritanceNode.builder(permissionNode).build() :
                        Node.builder(permissionNode).build();
                user.data().add(node);
                if (parameters.contains("debug")) {getLogger().info("Assigned permission '" + permissionNode + "' to " + player + " (Tier: " + tier + ")");}
            } 
            else if (parameters.contains("debug")) {getLogger().warning("No permission node found for tier " + tier + " or override.");}
        });
    }

}
