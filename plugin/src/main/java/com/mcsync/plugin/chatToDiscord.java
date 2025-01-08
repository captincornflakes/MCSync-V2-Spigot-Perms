package com.mcsync.plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class chatToDiscord implements Listener {
    private final Plugin plugin;

    public chatToDiscord(Plugin plugin) {
        this.plugin = plugin;
    }

    private String getWebhookUrl() {
        return plugin.getConfig().getString("discordWebhookURL", "");
    }

    private boolean isDebugEnabled() {
        return plugin.getConfig().getString("parameters", "").toLowerCase().contains("debug");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerName = event.getPlayer().getName();
        String message = event.getMessage();

        if (isDebugEnabled()) {
            plugin.getLogger().info("Player " + playerName + " sent message: " + message);
        }

        // Format the chat message as "MinecraftUsername: Message"
        String formattedMessage = playerName + ": " + message;

        // Format the JSON payload for Discord
        String jsonPayload = String.format(
            "{\"content\": \"%s\"}",
            formattedMessage);

        if (isDebugEnabled()) {
            plugin.getLogger().info("Payload created: " + jsonPayload);
        }

        // Send the message to the Discord webhook
        sendToDiscord(jsonPayload);
    }

    private void sendToDiscord(String jsonPayload) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (isDebugEnabled()) {
                plugin.getLogger().info("Attempting to send payload to Discord webhook...");
            }

            try {
                // Open connection to the Discord webhook URL
                String webhookUrl = getWebhookUrl();
                if (webhookUrl.isEmpty()) {
                    plugin.getLogger().severe("Discord webhook URL is not configured!");
                    return;
                }

                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Configure the connection
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Write the JSON payload
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(jsonPayload.getBytes());
                    outputStream.flush();
                }

                if (isDebugEnabled()) {
                    plugin.getLogger().info("Payload sent. Waiting for response...");
                }

                // Check the response
                int responseCode = connection.getResponseCode();
                if (responseCode == 204) { // Discord webhook responds with 204 No Content
                    if (isDebugEnabled()) {
                        plugin.getLogger().info("Message successfully sent to Discord.");
                    }
                } else {
                    plugin.getLogger().warning("Failed to send message to Discord. Response code: " + responseCode);
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Error while sending message to Discord: " + e.getMessage());
            }
        });
    }
}
