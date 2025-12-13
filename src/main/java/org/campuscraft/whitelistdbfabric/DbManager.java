package org.campuscraft.whitelistdbfabric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DbManager {

    private Connection conn;
    private static ConfigManager configManager;
    public static final Logger LOGGER = LoggerFactory.getLogger("whitelistdb");


    public DbManager(String url, String user, String pass) {
        try {
            conn = DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            LOGGER.error("Failed to connect to database", e);
        }
        File configDir = new File("config");
        if (!configDir.exists()) configDir.mkdirs();

        configManager = new ConfigManager(configDir);
    }

    public boolean isPlayerWhitelisted(UUID uuid) {
        ConfigManager.Config cfg = configManager.get();
        if(conn == null){
            try {
                conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.getUsername(), cfg.getPassword());
            } catch (SQLException e) {
                LOGGER.error("Failed to connect to database", e);
            }
        }

        if(conn == null){
            return false;
        }

        try (PreparedStatement st = conn.prepareStatement("SELECT 1 FROM server_whitelists WHERE uuid = ? LIMIT 1")) {
            st.setObject(1, uuid);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to connect to database", e);
        }
        return false;
    }

    public boolean isPlayerBanned(UUID uuid) {
        ConfigManager.Config cfg = configManager.get();
        if(conn == null){
            try {
                conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.getUsername(), cfg.getPassword());
            } catch (SQLException e) {
                LOGGER.error("Failed to connect to database", e);
            }
        }

        if(conn == null){
            return false;
        }

        try( PreparedStatement st = conn.prepareStatement("SELECT banned FROM server_whitelists WHERE uuid = ? LIMIT 1")) {
            st.setObject(1,uuid);
            try(ResultSet rs = st.executeQuery()) {
                if(rs.next()) {
                    return rs.getBoolean("banned");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to see if user is banned. Is the user in the whitelist?r");
        }
        return false;
    }

    public boolean banPlayer(UUID uuid){
        ConfigManager.Config cfg = configManager.get();
        if(conn == null){
            try {
                conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.getUsername(), cfg.getPassword());
            } catch (SQLException e) {
                LOGGER.error("Failed to connect to database", e);
            }
        }

        if(conn == null){
            return false;
        }

        if(isPlayerWhitelisted(uuid)) {
            String sql = "UPDATE server_whitelists SET banned = true WHERE uuid = ?";
            try {
                PreparedStatement st = conn.prepareStatement(sql);
                st.setObject(1, uuid);
                st.executeUpdate();
                return true;
            } catch (SQLException e) {
                LOGGER.error("Failed to ban the user: ", e);
            }
        }
        return false;
    }

    public boolean unbanPlayer(String username){
        ConfigManager.Config cfg = configManager.get();
        if(conn == null){
            try {
                conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.getUsername(), cfg.getPassword());
            } catch (SQLException e) {
                LOGGER.error("Failed to connect to database", e);
            }
        }

        if(conn == null){
            return false;
        }

        UUID uuid = ApiManager.getUUID(username);

        String sql = "UPDATE server_whitelists SET banned = false WHERE UUID = ?";
        try {
            PreparedStatement st = conn.prepareStatement(sql);
            st.setObject(1, uuid);
            st.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.error("Failed to unban the user: ", e);
        }
        return false;
    }

    public String getPlayerSchool(UUID uuid){
        ConfigManager.Config cfg = configManager.get();
        if(conn == null){
            try {
                conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.getUsername(), cfg.getPassword());
            } catch (SQLException e) {
                LOGGER.error("Failed to connect to database", e);
            }
        }

        if(conn == null){
            return null;
        }

        String sql = "SELECT school FROM server_whitelists WHERE uuid = ? LIMIT 1";
        try{
            PreparedStatement st = conn.prepareStatement(sql);
            st.setObject(1, uuid);
            ResultSet rs = st.executeQuery();
            if(rs.next()){
                return rs.getString("school");
            }
        } catch (SQLException e){
            LOGGER.error("Failed to get school for user: ", e);
        }
        return null;
    }
}
