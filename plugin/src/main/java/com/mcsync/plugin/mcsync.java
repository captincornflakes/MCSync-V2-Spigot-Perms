package com.mcsync.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.logging.Level;

import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;

public class mcsync extends JavaPlugin implements Listener {
    private Metrics metrics;
    private LuckPerms luckPerms;
    static mcsync instance;
    @SuppressWarnings("FieldMayBeFinal")
    private FileConfiguration config = getConfig();
    @SuppressWarnings("FieldMayBeFinal")
    private String prefix = ChatColor.LIGHT_PURPLE + "[" + ChatColor.BLUE + "MCSYNC" + ChatColor.LIGHT_PURPLE + "] " + ChatColor.RESET;
    @SuppressWarnings("FieldMayBeFinal")
    private String endpointLocation = "https://mcsync.live/api.php";

    private boolean isKicked = false;

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        super.onEnable();
        saveDefaultConfig();
        instance = this;
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("mcsync").setExecutor(new CommandMcsync(this));
        getLogger().info("MCSync has been enabled!");
        //Enable Bstats
       // int pluginId = 24033;
        //metrics = new Metrics(this, pluginId);
        //getLogger().info("bStats initialized. " + isMetricsRunning());
        this.luckPerms = LuckPermsProvider.get();
    }
    public boolean isMetricsRunning() {
        return metrics != null;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    @Override
    public void onDisable() {
        getLogger().info("MCSync has been disabled.");
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        isKicked = true;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Suppress quit message if the player was kicked
        if (isKicked) {
            event.setQuitMessage(null);
            isKicked = false;
        }

        // Retrieve the UUID of the quitting player
        UUID uuid = event.getPlayer().getUniqueId();
        LuckPerms api = LuckPermsProvider.get();
        User user = api.getUserManager().getUser(uuid);

        // Retrieve the parameters and configuration keys
        String parameters = config.getString("parameters", "").toLowerCase();
        String permissionsMode = config.getString("permissionsMode", "node"); // Default to "node" if not set
        String configFollower = config.getString("follower");
        String configSubt1 = config.getString("sub-t1");
        String configSubt2 = config.getString("sub-t2");
        String configSubt3 = config.getString("sub-t3");
        String configSubGifted = config.getString("sub-gifted");
        String configStreamer = config.getString("streamer");

        // Modify the user's permissions based on the permissionsMode
        api.getUserManager().modifyUser(uuid, userMod -> {
            if (userMod != null) {
                if ("node".equalsIgnoreCase(permissionsMode)) {
                    // If using node-based permissions, remove the corresponding nodes
                    if (configFollower != null) userMod.data().remove(Node.builder(configFollower).build());
                    if (configSubt1 != null) userMod.data().remove(Node.builder(configSubt1).build());
                    if (configSubt2 != null) userMod.data().remove(Node.builder(configSubt2).build());
                    if (configSubt3 != null) userMod.data().remove(Node.builder(configSubt3).build());
                    if (configSubGifted != null) userMod.data().remove(Node.builder(configSubGifted).build());
                    if (configStreamer != null) userMod.data().remove(Node.builder(configStreamer).build());
                } else if ("group".equalsIgnoreCase(permissionsMode)) {
                    // If using group-based permissions, remove the corresponding groups
                    if (configFollower != null) userMod.data().remove(Node.builder("group." + configFollower).build());
                    if (configSubt1 != null) userMod.data().remove(Node.builder("group." + configSubt1).build());
                    if (configSubt2 != null) userMod.data().remove(Node.builder("group." + configSubt2).build());
                    if (configSubt3 != null) userMod.data().remove(Node.builder("group." + configSubt3).build());
                    if (configSubGifted != null) userMod.data().remove(Node.builder("group." + configSubGifted).build());
                    if (configStreamer != null) userMod.data().remove(Node.builder("group." + configStreamer).build());
                }
            }

            // Debug logging if "parameters" contains "debug"
            if (parameters.contains("debug")) {
                getLogger().warning("Removed permissions or group for user: " + uuid);
            }
        });
    }

    

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String token = config.getString("token");
        String permissionsMode = config.getString("permissionsMode");
        String parameters = config.getString("parameters", "").toLowerCase();
        boolean authorizePlayer = false;
        int tier = 0;
        if (player.isWhitelisted()) {
            authorizePlayer = true;
        }
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
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                JSONObject data = new JSONObject(response.toString());
                authorizePlayer = data.getBoolean("subscriber");
                tier = data.getInt("tier");
                if (parameters.contains("debug")) {
                    getLogger().info("Response: " + response.toString());
                }
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error during HTTP request: {0}", e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        if (!parameters.contains("perms")) {
            if (authorizePlayer) {
                if (parameters.contains("debug")) {
                    getLogger().warning("ok: " + tier + " " + permissionsMode);
                }
            } else {

                String configMessage = getConfig().getString("fail_message",  "You are not authorized to join this server.");
                String coloredMessage = ChatColor.translateAlternateColorCodes('&', configMessage);
                player.kickPlayer(coloredMessage);
            }
        }
        assignPermissions(player, uuid, parameters, tier);
    }
    //permissionsMode
    private void assignPermissions(Player player, UUID uuid, String parameters, int tier) {
        if (luckPerms == null) {
            if (parameters.contains("debug")) {
                getLogger().warning("LuckPerms is not initialized!");
            }
            return;
        }
    
        luckPerms.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            if (user == null) {
                getLogger().warning("Failed to load user data for UUID: " + uuid);
                return;
            }

            
    
            String permissionsMode = config.getString("permissionsMode");
            luckPerms.getUserManager().modifyUser(uuid, userMod -> {
                
                userMod.data().remove(Node.builder("group." + config.getString("overide")).build());
                userMod.data().remove(Node.builder("group." + config.getString("sub-t1")).build());
                userMod.data().remove(Node.builder("group." + config.getString("sub-t1")).build());
                userMod.data().remove(Node.builder("group." + config.getString("sub-t3")).build());
                
                userMod.data().remove(Node.builder("" + config.getString("overide")).build());
                userMod.data().remove(Node.builder("" + config.getString("sub-t1")).build());
                userMod.data().remove(Node.builder("" + config.getString("sub-t1")).build());
                userMod.data().remove(Node.builder("" + config.getString("sub-t3")).build());

                if ("node".equalsIgnoreCase(permissionsMode)) {
                    // Handle node-based permissions
                    String newNodeKey = null;
                    if (tier == 0) {
                        newNodeKey = config.getString("overide", "default.override");
                    } else if (tier == 1) {
                        newNodeKey = config.getString("sub-t1", "default.sub-t1");
                    } else if (tier == 2) {
                        newNodeKey = config.getString("sub-t2", "default.sub-t2");
                    } else if (tier == 3) {
                        newNodeKey = config.getString("sub-t3", "default.sub-t3");
                    }
                    // Validate newNodeKey
                    if (newNodeKey == null || newNodeKey.isEmpty()) {
                        getLogger().warning("Failed to assign a valid node key for tier " + tier);
                        return;
                    }
                    getLogger().info("Assigning node key: " + newNodeKey);
                    // Add the new node
                    userMod.data().add(Node.builder(newNodeKey).build());
                }
                
                else if ("group".equalsIgnoreCase(permissionsMode)) {
                    String newGroup = null;
                    if (tier == 0) {
                        newGroup = config.getString("overide");
                    } else if (tier == 1) {
                        newGroup = config.getString("sub-t1");
                    } else if (tier == 2) {
                        newGroup = config.getString("sub-t2");
                    } else if (tier == 3) {
                        newGroup = config.getString("sub-t3");
                    }
                    if (newGroup != null) {
                        userMod.data().add(InheritanceNode.builder(newGroup).build());
                        }
                        
                    }
            else {
                    getLogger().warning("Unknown permissions mode: " + permissionsMode);
                }
            });
            if (parameters.contains("debug")) {
                getLogger().info("Permissions assigned for " + player.getName() + " (Tier: " + tier + ", Mode: " + permissionsMode + ")");
            }
        });
    }
    
    
    

    public class CommandMcsync implements CommandExecutor {

        private final mcsync plugin; // Reference to the main plugin instance

        public CommandMcsync(mcsync plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (command.getName().equalsIgnoreCase("mcsync")) {
                FileConfiguration config = plugin.getConfig(); // Access config dynamically
                String prefix = plugin.prefix; // Use the prefix from the main class
                String token = config.getString("token");
    
                if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                    // Help message
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + ChatColor.STRIKETHROUGH.toString() + "--------------------------------------------");
                    sender.sendMessage(ChatColor.GOLD + "The following are valid commands for MCSync:");
                    sender.sendMessage(ChatColor.GOLD + "| " + ChatColor.YELLOW + "/mcsync set <key>" + ChatColor.GRAY + ChatColor.ITALIC + " (Set server token)");
                    sender.sendMessage(ChatColor.GOLD + "| " + ChatColor.YELLOW + "/mcsync get" + ChatColor.GRAY + ChatColor.ITALIC + " (Show your server token)");
                    sender.sendMessage(ChatColor.GOLD + "| " + ChatColor.YELLOW + "/mcsync reload" + ChatColor.GRAY + ChatColor.ITALIC + " (Reload the config)");
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + ChatColor.STRIKETHROUGH.toString() + "--------------------------------------------");
                } else if (args[0].equalsIgnoreCase("set")) {
                    if (args.length < 2) {
                        sender.sendMessage(prefix + ChatColor.RED + "Please supply a server key.");
                    } else {
                        String newToken = args[1];
                        config.set("token", newToken);
                        plugin.saveConfig(); // Save changes to the config
                        sender.sendMessage(prefix + ChatColor.AQUA + "Server key set to " + ChatColor.GREEN + newToken);
                    }
                } else if (args[0].equalsIgnoreCase("get")) {
                    sender.sendMessage(prefix + ChatColor.AQUA + "Your server key is: " + ChatColor.GREEN + token);
                } else if (args[0].equalsIgnoreCase("reload")) {
                    plugin.reloadConfig();
                    sender.sendMessage(prefix + ChatColor.AQUA + "MCSync Configuration reloaded successfully!");
                } else {
                    sender.sendMessage(prefix + ChatColor.RED + "Unknown command. Use /mcsync help for a list of commands.");
                }
                return true;
            }
            return false;
        }
    }
}