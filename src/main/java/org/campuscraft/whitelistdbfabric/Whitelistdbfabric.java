package org.campuscraft.whitelistdbfabric;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.campuscraft.whitelistdbfabric.mixin.ServerLoginNetworkHandlerAccessor;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

public class Whitelistdbfabric implements ModInitializer {

    public static final String MODID = "whitelistdb";

    private static WhitelistHandler whitelistHandler;
    private static ConfigManager configManager;
    private static DbManager dbManager;

    @Override
    public void onInitialize() {

        // ---- Load config from config directory ----
        File configDir = new File("config");
        if (!configDir.exists()) configDir.mkdirs();

        configManager = new ConfigManager(configDir);
        ConfigManager.Config cfg = configManager.get();

        // ---- Create DB manager using config ----
        this.dbManager = new DbManager(
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
                                                    () -> Text.literal(
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
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(
                        CommandManager.literal("wban")
                            .requires(source -> source.hasPermissionLevel(4))
                                .then(CommandManager.argument("player", StringArgumentType.greedyString())
                                .executes(this::banPlayer))
        ));
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(
                        CommandManager.literal("wunban")
                            .requires(source -> source.hasPermissionLevel(4))
                                .then(CommandManager.argument("player", StringArgumentType.greedyString())
                                .executes(this::unbanPlayer))
        ));
    }

    private int banPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String playerToBan = StringArgumentType.getString(context, "player");

        if (playerToBan != null) {
            String reason = configManager.getBanReason();
            UUID uuid = ApiManager.getUUID(playerToBan);

            if(dbManager.banPlayer(uuid)){
                MinecraftServer server = source.getServer();
                if(Objects.requireNonNull(server.getPlayerManager().getPlayer(uuid)).isDisconnected()){
                    source.sendFeedback(() -> Text.literal("Banned player: " + playerToBan), true);
                } else{
                    PlayerManager playerManager = server.getPlayerManager();
                    ServerPlayerEntity player = playerManager.getPlayer(uuid);
                    player.networkHandler.disconnect(Text.literal(reason).formatted(Formatting.RED));
                    source.sendFeedback(() -> Text.literal("Banned player: " + playerToBan), true);
                }
                return 1;
            }
            return 0;
        } else {
            source.sendError(Text.of("Player: " + playerToBan + " not found or is not online."));
            return 0;
        }
    }

    private int unbanPlayer(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerToUnban = StringArgumentType.getString(context, "player");

        if (playerToUnban != null) {
            if(dbManager.unbanPlayer(playerToUnban)){
                source.sendFeedback(() -> Text.literal("Player: " + playerToUnban + " has been unbanned!"), true);
                return 1;
            }
        } else {
            source.sendError(Text.of("Player: " + playerToUnban + " not found."));
        }
        return 0;
    }

    private void registerEvents() {
        // Player join = check DB whitelist
        ServerLoginConnectionEvents.QUERY_START.register(
                (handler, server, sender, synchronizer) -> {

                    GameProfile profile = ((ServerLoginNetworkHandlerAccessor) handler).getProfile();
                    UUID uuid = profile.getId();

                    if (!whitelistHandler.allowPlayer(uuid)) {
                        handler.disconnect(Text.literal(configManager.getMessage()));
                    }
                    if(!whitelistHandler.checkBanned(uuid)) {
                        handler.disconnect(Text.literal(configManager.getBanReason()));
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            System.out.println("[WhitelistDB] Whitelist enabled = "
                    + whitelistHandler.isWhitelistEnabled());
        });
    }
}