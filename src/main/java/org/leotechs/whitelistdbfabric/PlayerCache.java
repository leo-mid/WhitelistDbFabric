package org.leotechs.whitelistdbfabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PlayerCache {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, UUID> CACHE = new HashMap<>();
    private static Path cacheFile;

    public static void init() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        cacheFile = configDir.resolve("whitelistdb/player_cache.json");
        load();
    }

    /// Caches player when they join
    /// @param player - the player who just logged in

    public static void cachePlayer(ServerPlayer player) {
        String name = player.getName().getString().toLowerCase(Locale.ROOT);
        UUID uuid = player.getUUID();

        CACHE.put(name, uuid);
        save();
    }

    /// Get UUID by name
    /// @param username - the players username
    /// @return - the players uuid
    public static UUID getUuid(String username) {
        return CACHE.get(username.toLowerCase(Locale.ROOT));
    }

    /// Offline fallback (optional)
    /// @param username - the players username
    /// @return - offline players uuid
    public static UUID offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
        );
    }

    /// Loads the cache from the file
    private static void load() {
        if (!Files.exists(cacheFile)) return;

        try (Reader reader = Files.newBufferedReader(cacheFile)) {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> raw = GSON.fromJson(reader, type);

            raw.forEach((k, v) -> CACHE.put(k, UUID.fromString(v)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /// Saves the cache to the file
    private static void save() {
        try {
            Files.createDirectories(cacheFile.getParent());

            Map<String, String> raw = new HashMap<>();
            CACHE.forEach((k, v) -> raw.put(k, v.toString()));

            try (Writer writer = Files.newBufferedWriter(cacheFile)) {
                GSON.toJson(raw, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
