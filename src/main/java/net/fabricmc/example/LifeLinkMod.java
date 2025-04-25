package net.fabricmc.example;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

public class LifeLinkMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("lifelink");
    public static boolean isActive = false;
    public static boolean naturalDeathsEnabled = false;
    public static String firstDeathPlayer = null;
    public static Set<String> deadPlayers = new HashSet<>();
    private static final Map<UUID, Float> lastHealth = new HashMap<>();
    private static boolean allPlayersShouldBeDead = false;

    @Override
    public void onInitialize() {
        LOGGER.info("LifeLinkMod initializing");

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));

        // Listen for player combat deaths (handled by vanilla event)
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            LOGGER.info("AFTER_KILLED_OTHER_ENTITY event fired");
            if (isActive && entity instanceof ServerPlayerEntity player && !deadPlayers.contains(player.getName().getString())) {
                if (!naturalDeathsEnabled) { // Only trigger for combat deaths if natural deaths are disabled
                    LOGGER.info("Combat death detected for player: {}", player.getName().getString());
                    firstDeathPlayer = player.getName().getString();
                    killAllPlayers(player.getServer());
                    allPlayersShouldBeDead = true; // Mark all future joiners as dead
                }
            }
        });

        // Workaround for natural deaths: check player health each tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!isActive || !naturalDeathsEnabled) return;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (deadPlayers.contains(player.getName().getString())) continue;
                float health = player.getHealth();
                UUID uuid = player.getUuid();
                float prev = lastHealth.getOrDefault(uuid, health);
                if (prev > 0 && health <= 0) {
                    LOGGER.info("Natural death detected for player: {}", player.getName().getString());
                    firstDeathPlayer = player.getName().getString();
                    killAllPlayers(server);
                    allPlayersShouldBeDead = true; // Mark all future joiners as dead
                    break; // Only trigger once per tick
                }
                lastHealth.put(uuid, health);
            }
        });

        // Listen for player joins
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            LOGGER.info("Player join event: {}", player.getName().getString());
            String name = player.getName().getString();
            // If LifeLink is active and all players should be dead, kill new joiners
            if (isActive && allPlayersShouldBeDead && !deadPlayers.contains(name)) {
                LOGGER.info("New player joined after LifeLink death, killing: {}", name);
                deadPlayers.add(name);
                player.kill((ServerWorld) player.getWorld());
                player.changeGameMode(GameMode.SPECTATOR);
                player.sendMessage(Text.literal("You are dead! First death: " + firstDeathPlayer), false);
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("You are dead!")));
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("First death: " + firstDeathPlayer)));
            } else if (isActive && deadPlayers.contains(name)) {
                LOGGER.info("Dead player joined: {}", name);
                player.kill((ServerWorld) player.getWorld());
                player.changeGameMode(GameMode.SPECTATOR);
                player.sendMessage(Text.literal("You are dead! First death: " + firstDeathPlayer), false);
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("You are dead!")));
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("First death: " + firstDeathPlayer)));
            } else if (isActive && !allPlayersShouldBeDead) {
                // Ensure new joiners are in survival if revived
                player.changeGameMode(GameMode.SURVIVAL);
                if (player.isDead()) {
                    player.requestRespawn();
                }
            }
        });

        // Reset state on world load
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started, resetting LifeLink state");
            resetState();
            lastHealth.clear();
            allPlayersShouldBeDead = false;
        });
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("lifelink")
            .then(literal("start").executes(ctx -> {
                isActive = true;
                firstDeathPlayer = null;
                deadPlayers.clear();
                lastHealth.clear();
                allPlayersShouldBeDead = false;
                // Initialize health tracking for all players
                for (ServerPlayerEntity player : ctx.getSource().getServer().getPlayerManager().getPlayerList()) {
                    lastHealth.put(player.getUuid(), player.getHealth());
                }
                ctx.getSource().sendFeedback(() -> Text.literal("Life Link started!"), false);
                return 1;
            }))
            .then(literal("stop").executes(ctx -> {
                isActive = false;
                firstDeathPlayer = null;
                deadPlayers.clear();
                lastHealth.clear();
                allPlayersShouldBeDead = false;
                ctx.getSource().sendFeedback(() -> Text.literal("Life Link stopped!"), false);
                return 1;
            }))
            .then(literal("revive").executes(ctx -> {
                reviveAllPlayers(ctx.getSource().getServer());
                allPlayersShouldBeDead = false; // New joiners should be alive
                ctx.getSource().sendFeedback(() -> Text.literal("All players revived!"), false);
                return 1;
            }))
            .then(literal("naturaldeaths").executes(ctx -> {
                naturalDeathsEnabled = !naturalDeathsEnabled;
                ctx.getSource().sendFeedback(() -> Text.literal("Natural deaths are now " + (naturalDeathsEnabled ? "enabled" : "disabled")), false);
                return 1;
            }))
        );
    }

    public static void killAllPlayers(MinecraftServer server) {
        LOGGER.info("killAllPlayers called");
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!deadPlayers.contains(player.getName().getString())) {
                LOGGER.info("Killing player: {}", player.getName().getString());
                deadPlayers.add(player.getName().getString());
                player.kill((ServerWorld) player.getWorld());
                player.changeGameMode(GameMode.SPECTATOR);
                player.sendMessage(Text.literal("You died because " + firstDeathPlayer + " died!"), false);
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("You are dead!")));
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("First death: " + firstDeathPlayer)));
            }
        }
    }

    private void reviveAllPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.changeGameMode(GameMode.SURVIVAL);
            if (player.isDead()) {
                player.requestRespawn();
            }
        }
        deadPlayers.clear();
        firstDeathPlayer = null;
    }

    private void resetState() {
        isActive = false;
        firstDeathPlayer = null;
        deadPlayers.clear();
    }
}
