package org.campuscraft.whitelistdbfabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean isEnabled() {
        return config.enabled;
    }

    public void setWhitelistEnabled(boolean enabled) {
        config.enabled = enabled;
    }

    public static class Config {
        private String host = "localhost";
        private int port = 5432;
        private String database = "minecraft";
        private String username = "postgres";
        private String password = "password";
        private boolean ssl = false;
        private String message = "You are not whitelisted!";
        private boolean enabled = true;


        public String jdbcUrl() {
            return "jdbc:postgresql://" + host + ":" + port + "/" + database +
                    (ssl ? "?sslmode=require" : "");
        }

        public String getUsername(){
            return this.username;
        }

        public String getPassword(){
            return this.password;
        }
    }

    private final File configFile;
    private Config config;

    public ConfigManager(File configDir) {
        this.configFile = new File(configDir, "whitelistdb-config.json");
        load();
    }

    public Config get() {
        return config;
    }

    public void load() {
        try {
            if (!configFile.exists()) {
                config = new Config();
                save(); // create default config
                return;
            }

            try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, Config.class);
            }

        } catch (Exception e) {
            e.printStackTrace();
            config = new Config();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMessage() {
        return config.message;
    }

}
