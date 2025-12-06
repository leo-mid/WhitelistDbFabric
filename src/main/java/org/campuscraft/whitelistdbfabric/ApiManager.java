package org.campuscraft.whitelistdbfabric;

import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class ApiManager {
    public static UUID getUUID(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();
            if (status == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonParser parser = new JsonParser();
                String uuidString = parser.parse(response.toString()).getAsJsonObject().get("id").getAsString();
                return UUID.fromString(uuidString.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
            } else {
                System.err.println("Failed to fetch UUID for " + username + ". Status code: " + status);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
