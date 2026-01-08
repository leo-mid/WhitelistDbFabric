package org.leotechs.whitelistdbfabric;

import java.util.UUID;

public class WhitelistHandler {

    private boolean whitelistEnabled;
    private final DbManager db;
    private final ConfigManager config;

    public WhitelistHandler(DbManager db, ConfigManager config) {
        this.db = db;
        this.config = config;
        this.whitelistEnabled = this.config.isEnabled();
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public void toggleWhitelist() {
        whitelistEnabled = !whitelistEnabled;
        config.setWhitelistEnabled(whitelistEnabled);
        config.save();
    }

    public boolean allowPlayer(UUID uuid) {
        if (!whitelistEnabled) return true;

        return db.isPlayerWhitelisted(uuid);
    }

    public boolean checkBanned(UUID uuid) {
        return !db.isPlayerBanned(uuid);
    }
}
