package com.mcsync.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import static org.bukkit.Bukkit.getLogger;
import org.json.JSONObject;

public class Auth {
    public static String check(String token, String uuid, String parameters) {
        boolean authorize = false;
        int tier = 0;
        try {
            URL url = new URL("https://api.mcsync.live/?token=" + token + "&uuid=" + uuid.replace("-", ""));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
                    authorize = data.getBoolean("subscriber");
                    tier = data.getInt("tier");

                    if (parameters.contains("debug")) {
                        getLogger().info("Response: " + response);
                    }
                } catch (IOException ex) {
                    getLogger().info("Error: No API response");
                }
            }
            connection.disconnect();
        } catch (ProtocolException ex) {
            getLogger().info("Error: Protocol exception");
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONObject result = new JSONObject();
        result.put("authorize", authorize);
        result.put("tier", tier);
        if (parameters.contains("debug")) {
            getLogger().info("Auth check return result: " + result);
        }
        return result.toString();
    }
}
