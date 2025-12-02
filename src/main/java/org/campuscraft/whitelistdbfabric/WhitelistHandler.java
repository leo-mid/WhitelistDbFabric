package org.campuscraft.whitelistdbfabric;

import net.minecraft.server.network.ServerPlayerEntity;

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

    public boolean allowPlayer(ServerPlayerEntity player) {
        if (!whitelistEnabled) return true;

        return db.isPlayerWhitelisted(player.getUuid());
    }
}
