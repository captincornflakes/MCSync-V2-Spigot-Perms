package com.mcsync.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;

import static org.bukkit.Bukkit.getLogger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.json.JSONObject;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

public class McsyncEventListener implements Listener {
    private final mcsync plugin;
    private final LuckPerms luckPerms;
    private boolean isKicked = false;
    private final String endpointLocation = "https://mcsync.live/api.php";

    public McsyncEventListener(mcsync plugin) {
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
            getConfig().getString("follower"),
            getConfig().getString("sub-t1"),
            getConfig().getString("sub-t2"),
            getConfig().getString("sub-t3"),
            getConfig().getString("sub-gifted"),
            getConfig().getString("streamer")
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

            if (getConfig().getString("parameters", "").toLowerCase().contains("debug")) {
                getLogger().warning("Removed permissions for user: " + uuid);
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String token = getConfig().getString("token");
        String parameters = getConfig().getString("parameters", "").toLowerCase();
        boolean authorizePlayer = player.isWhitelisted();
        int tier = 0;

        if (parameters.contains("debug")) {
            getLogger().info("PlayerJoin: " + player.getName());
            getLogger().info("Token: " + token);
            getLogger().info("UUID: " + uuid);
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(endpointLocation + "?token=" + token + "&uuid=" + uuid.toString().replace("-", ""));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    JSONObject data = new JSONObject(response.toString());
                    authorizePlayer = data.getBoolean("subscriber");
                    tier = data.getInt("tier");

                    if (parameters.contains("debug")) {
                        getLogger().info("Response: " + response);
                    }
                }
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error during HTTP request: {0}", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        if (!authorizePlayer) {
            String failMessage = ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("fail_message", "You are not authorized to join this server."));
            player.kickPlayer(failMessage);
        } else {
            assignPermissions(player, uuid, parameters, tier);
        }
    }

    private void assignPermissions(Player player, UUID uuid, String parameters, int tier) {
        if (luckPerms == null) {
            if (parameters.contains("debug")) {
                getLogger().warning("LuckPerms is not initialized!");
            }
            return;
        }

        luckPerms.getUserManager().modifyUser(uuid, user -> {
            if (user == null) {
                getLogger().warning("Failed to load user data for UUID: " + uuid);
                return;
            }

            String permissionsMode = getConfig().getString("permissionsMode");
            String[] permissionNodes = {
                getConfig().getString("overide"),
                getConfig().getString("sub-t1"),
                getConfig().getString("sub-t2"),
                getConfig().getString("sub-t3")
            };

            for (String permission : permissionNodes) {
                if (permission != null) {
                    Node node = "group".equalsIgnoreCase(permissionsMode) ?
                            InheritanceNode.builder(permission).build() :
                            Node.builder(permission).build();
                    user.data().add(node);
                }
            }

            if (parameters.contains("debug")) {
                getLogger().info("Assigned permissions for " + player.getName() + " (Tier: " + tier + ")");
            }
        });
    }
}
