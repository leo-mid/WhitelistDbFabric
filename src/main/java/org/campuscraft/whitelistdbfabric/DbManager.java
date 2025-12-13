package org.campuscraft.whitelistdbfabric;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DbManager {

    private Connection conn;
    private static ConfigManager configManager;


    public DbManager(String url, String user, String pass) {
        try {
            conn = DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            e.printStackTrace();
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
                e.printStackTrace();
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
            e.printStackTrace();
        }
        return false;
    }

    public boolean isPlayerBanned(UUID uuid) {
        ConfigManager.Config cfg = configManager.get();
        if(conn == null){
            try {
                conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.getUsername(), cfg.getPassword());
            } catch (SQLException e) {
                e.printStackTrace();
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
            System.out.println(e.getMessage());
        }
        return false;
    }

    public boolean banPlayer(UUID uuid){
        ConfigManager.Config cfg = configManager.get();
        if(conn == null){
            try {
                conn = DriverManager.getConnection(cfg.jdbcUrl(), cfg.getUsername(), cfg.getPassword());
            } catch (SQLException e) {
                e.printStackTrace();
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
                System.out.println(e.getMessage() + uuid);
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
                e.printStackTrace();
            }
        }

        if(conn == null){
            return false;
        }

        UUID uuid = ApiManager.getUUID(username);

        String sql =  "UPDATE server_whitelists SET banned = false WHERE UUID = ?";
        try {
            PreparedStatement st = conn.prepareStatement(sql);
            st.setObject(1, uuid);
            st.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}
