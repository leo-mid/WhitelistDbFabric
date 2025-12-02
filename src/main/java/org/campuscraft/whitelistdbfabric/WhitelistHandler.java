package org.campuscraft.whitelistdbfabric;

import net.minecraft.server.network.ServerPlayerEntity;

public class WhitelistHandler {

    private boolean whitelistEnabled = true;
    private final DbManager db;

    public WhitelistHandler(DbManager db) {
        this.db = db;
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public void toggleWhitelist() {
        whitelistEnabled = !whitelistEnabled;
    }

    public boolean allowPlayer(ServerPlayerEntity player) {
        if (!whitelistEnabled) return true;

        return db.isPlayerWhitelisted(player.getUuid());
    }
}
