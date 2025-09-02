package com.mcsync.plugin;

import java.util.UUID;

public class PermissionAssigner {
    private final net.luckperms.api.LuckPerms luckPerms;
    private final org.bukkit.configuration.file.FileConfiguration config;

    public PermissionAssigner(net.luckperms.api.LuckPerms luckPerms, org.bukkit.configuration.file.FileConfiguration config) {
        this.luckPerms = luckPerms;
        this.config = config;
    }

    public void assignPermissions(String player, UUID uuid, String parameters, int tier) {
        if (luckPerms == null) {
            if (parameters.contains("debug")) { org.bukkit.Bukkit.getLogger().warning("LuckPerms is not initialized!");}
            return;
        }
        luckPerms.getUserManager().modifyUser(uuid, user -> {
            if (user == null) {
                org.bukkit.Bukkit.getLogger().warning("Failed to load user data for UUID: " + uuid);
                return;
            }
            String permissionsMode = config.getString("permissionsMode");
            String permissionNode = null;
            switch (tier) {
                case 1:
                    permissionNode = config.getString("sub-t1");
                    break;
                case 2:
                    permissionNode = config.getString("sub-t2");
                    break;
                case 3:
                    permissionNode = config.getString("sub-t3");
                    break;
                default:
                    permissionNode = config.getString("override");
            }
            if (permissionNode != null) {
                net.luckperms.api.node.Node node = "group".equalsIgnoreCase(permissionsMode) ?
                        net.luckperms.api.node.types.InheritanceNode.builder(permissionNode).build() :
                        net.luckperms.api.node.Node.builder(permissionNode).build();
                user.data().add(node);
                if (parameters.contains("debug")) {
                    org.bukkit.Bukkit.getLogger().info("Assigned permission '" + permissionNode + "' to " + player + " (Tier: " + tier + ")");
                }
            } else if (parameters.contains("debug")) {
                org.bukkit.Bukkit.getLogger().warning("No permission node found for tier " + tier + " or override.");
            }
        });
    }
}
