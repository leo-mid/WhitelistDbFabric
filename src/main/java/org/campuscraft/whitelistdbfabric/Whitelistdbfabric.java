package org.campuscraft.whitelistdbfabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.command.CommandManager;

import java.io.File;

public class Whitelistdbfabric implements ModInitializer {

    public static final String MODID = "whitelistdb";

    private static WhitelistHandler whitelistHandler;
    private static ConfigManager configManager;

    @Override
    public void onInitialize() {

        // ---- Load config from config directory ----
        File configDir = new File("config");
        if (!configDir.exists()) configDir.mkdirs();

        configManager = new ConfigManager(configDir);
        ConfigManager.Config cfg = configManager.get();

        // ---- Create DB manager using config ----
        DbManager dbManager = new DbManager(
                cfg.jdbcUrl(),
                cfg.getUsername(),
                cfg.getPassword()
        );

        whitelistHandler = new WhitelistHandler(dbManager, configManager);

        registerCommands();
        registerEvents();

        System.out.println("[WhitelistDB] Loaded config and initialized database connection.");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(
                        CommandManager.literal("whitelistdb")
                                .then(CommandManager.literal("toggle")
                                        .requires(src -> src.hasPermissionLevel(4))
                                        .executes(ctx -> {
                                            whitelistHandler.toggleWhitelist();
                                            ctx.getSource().sendFeedback(
                                                    () -> net.minecraft.text.Text.literal(
                                                            "Whitelist is now "
                                                                    + (whitelistHandler.isWhitelistEnabled() ? "ENABLED" : "DISABLED")
                                                    ),
                                                    true
                                            );
                                            return 1;
                                        })
                                )
                )
        );
    }

    private void registerEvents() {

        // Player join = check DB whitelist
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!whitelistHandler.allowPlayer(handler.getPlayer())) {
                handler.disconnect(
                        net.minecraft.text.Text.literal(configManager.getMessage())
                );
            }
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            System.out.println("[WhitelistDB] Whitelist enabled = "
                    + whitelistHandler.isWhitelistEnabled());
        });
    }
}
