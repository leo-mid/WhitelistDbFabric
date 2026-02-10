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

    // Cache player when they join
    public static void cachePlayer(ServerPlayer player) {
        String name = player.getName().getString().toLowerCase(Locale.ROOT);
        UUID uuid = player.getUUID();

        CACHE.put(name, uuid);
        save();
    }

    // Lookup UUID by name
    public static UUID getUuid(String username) {
        return CACHE.get(username.toLowerCase(Locale.ROOT));
    }

    // Offline fallback (optional)
    public static UUID offlineUuid(String username) {
        return UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
        );
    }

    // Load cache from disk
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

    // Save cache to disk
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
