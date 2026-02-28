package org.leotechs.whitelistdbfabric;

import java.util.UUID;

public class WhitelistHandler {

    private boolean whitelistEnabled;
    private final DbManager db;
    private final ConfigManager config;

    /// Creates the WhitelistHandler object
    /// @param db - the database manager
    /// @param config - the config manager

    public WhitelistHandler(DbManager db, ConfigManager config) {
        this.db = db;
        this.config = config;
        this.whitelistEnabled = this.config.isEnabled();
    }

    /// Returns if the whitelist is enabled
    /// @return whitelistEnabled

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    /// Changes the whitelist status

    public void toggleWhitelist() {
        whitelistEnabled = !whitelistEnabled;
        config.setWhitelistEnabled(whitelistEnabled);
        config.save();
    }

    /// Checks to see if the player is whitelisted or not
    /// @param uuid - The uuid of the player logging in
    /// @returns if the player is able to access or not

    public boolean allowPlayer(UUID uuid) {
        if (!whitelistEnabled) return true;

        return db.isPlayerWhitelisted(uuid);
    }

    /// Checks to see if the player is banned or not
    /// @param uuid - The uuid of the player logging in
    /// @returns if the player is banend or not

    public boolean checkBanned(UUID uuid) {
        return !db.isPlayerBanned(uuid);
    }
}
