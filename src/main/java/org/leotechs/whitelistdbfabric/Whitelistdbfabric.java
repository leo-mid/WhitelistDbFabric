package org.leotechs.whitelistdbfabric;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
//import eu.pb4.placeholders.api.Placeholders;
//import eu.pb4.placeholders.api.PlaceholderResult;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.leotechs.whitelistdbfabric.mixin.ServerLoginNetworkHandlerAccessor;

import java.io.File;
import java.util.UUID;

public class Whitelistdbfabric implements ModInitializer {

    public static final String MODID = "whitelistdb";

    private static WhitelistHandler whitelistHandler;
    private static ConfigManager configManager;
    private DbManager dbManager;

    @Override
    public void onInitialize() {
        File configDir = new File("config");
        if (!configDir.exists()) configDir.mkdirs();

        configManager = new ConfigManager(configDir);
        ConfigManager.Config cfg = configManager.get();

        this.dbManager = new DbManager(
                cfg.jdbcUrl(),
                cfg.getUsername(),
                cfg.getPassword()
        );

        PlayerCache.init();

        // Cache players on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PlayerCache.cachePlayer(handler.getPlayer());
        });

        whitelistHandler = new WhitelistHandler(dbManager, configManager);

        registerCommands();
        registerEvents();

//        Placeholders.register(Identifier.of("whitelistdb", "school"), (ctx, arg) -> {
//            if (arg == null) {
//                return PlaceholderResult.invalid("No argument!");
//            }
//
//            assert ctx.player() != null;
//            UUID uuid = ctx.player().getUuid();
//            String school = dbManager.getPlayerSchool(uuid);
//
//            return PlaceholderResult.value(school);
//        });

        System.out.println("[WhitelistDB] Loaded config and initialized database connection.");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(
                        Commands.literal("whitelistdb")
                                .then(Commands.literal("toggle")
                                        .requires(source -> Permissions.check(source, "whitelistdb.admin", 4))
                                        .executes(ctx -> {
                                            whitelistHandler.toggleWhitelist();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal(
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
                        Commands.literal("wban")
                            .requires(source -> Permissions.check(source, "whitelistdb.admin", 4))
                                .then(Commands.argument("player", StringArgumentType.greedyString())
                                .executes(this::banPlayer))
        ));
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> dispatcher.register(
                        Commands.literal("wunban")
                            .requires(source -> Permissions.check(source, "whitelistdb.admin", 4))
                                .then(Commands.argument("player", StringArgumentType.greedyString())
                                .executes(this::unbanPlayer))
        ));
    }

    private int banPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String playerToBan = StringArgumentType.getString(context, "player");

        if (playerToBan != null) {
            String reason = configManager.getBanReason();

            MinecraftServer server = source.getServer();
            PlayerList playerManager = server.getPlayerList();
            ServerPlayer player = playerManager.getPlayerByName(playerToBan);
            if(player != null){
                if(dbManager.banPlayer(player.getUUID())){
                    if (player != null){
                        player.connection.disconnect(Component.literal(reason).withStyle(ChatFormatting.RED));
                        source.sendSuccess(() -> Component.literal("Banned player: " + playerToBan), true);
                    } else{
                        source.sendSuccess(() -> Component.literal("Banned player: " + playerToBan), true);
                    }
                    return 1;
                }
            } else {
                UUID uuid = PlayerCache.getUuid(playerToBan);
                if(uuid != null){
                    if(dbManager.banPlayer(uuid)){
                        source.sendSuccess(() -> Component.literal("Banned player: " + playerToBan), true);
                        return 1;
                    }
                }
            }

            source.sendFailure(Component.nullToEmpty("Player: " + playerToBan + " not found."));
            return 0;
        } else {
            source.sendSuccess(() -> Component.literal("Forgot to add the player to ban"), true);
            return 0;
        }
    }

    private int unbanPlayer(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String playerToUnban = StringArgumentType.getString(context, "player");

        UUID uuid = PlayerCache.getUuid(playerToUnban);

        if (uuid != null) {
            if(dbManager.unbanPlayer(uuid)){
                source.sendSuccess(() -> Component.literal("Player: " + playerToUnban + " has been unbanned!"), true);
                return 1;
            }
        } else {
            source.sendFailure(Component.nullToEmpty("Player: " + playerToUnban + " not found."));
        }
        return 0;
    }

    private void registerEvents() {
        // Player join = check DB whitelist
        ServerLoginConnectionEvents.QUERY_START.register(
                (handler, server, sender, synchronizer) -> {

                    GameProfile profile = ((ServerLoginNetworkHandlerAccessor) handler).getProfile();
                    UUID uuid = profile.id();

                    if (!whitelistHandler.allowPlayer(uuid)) {
                        handler.disconnect(Component.literal(configManager.getMessage()));
                    }
                    if(!whitelistHandler.checkBanned(uuid)) {
                        handler.disconnect(Component.literal(configManager.getBanReason()));
                    }
                }
        );

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            System.out.println("[WhitelistDB] Whitelist enabled = "
                    + whitelistHandler.isWhitelistEnabled());
        });
    }

    public boolean isPlayerConnected(MinecraftServer server, UUID playerUuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        // If getPlayer(uuid) returns null, the player is not currently online (i.e., is "disconnected" or offline)
        return player != null;
    }
}