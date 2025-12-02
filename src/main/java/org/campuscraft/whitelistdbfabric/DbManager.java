package org.campuscraft.whitelistdbfabric;

import java.sql.*;
import java.util.UUID;

public class DbManager {

    private Connection conn;

    public DbManager(String url, String user, String pass) {
        try {
            conn = DriverManager.getConnection(url, user, pass);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isPlayerWhitelisted(UUID uuid) {
        if (conn == null) return false;
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
}
