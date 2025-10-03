package com.mishkis.orbitalrailgun.net;

import com.mishkis.orbitalrailgun.ForgeOrbitalRailgunMod;
import com.mishkis.orbitalrailgun.item.OrbitalRailgunItem;
import com.mishkis.orbitalrailgun.net.msg.C2SRequestFire;
import com.mishkis.orbitalrailgun.net.msg.S2CFireAck;
import com.mishkis.orbitalrailgun.net.msg.S2CUpdateClientState;
import com.mishkis.orbitalrailgun.util.OrbitalRailgunStrikeManager;
import com.mishkis.orbitalrailgun.util.Raytracing;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int index = 0;

    private NetworkHandler() {
    }

    public static void init() {
        CHANNEL.registerMessage(nextId(), C2SRequestFire.class, C2SRequestFire::encode, C2SRequestFire::decode, C2SRequestFire::handle);
        CHANNEL.registerMessage(nextId(), S2CFireAck.class, S2CFireAck::encode, S2CFireAck::decode, S2CFireAck::handle);
        CHANNEL.registerMessage(nextId(), S2CUpdateClientState.class, S2CUpdateClientState::encode, S2CUpdateClientState::decode, S2CUpdateClientState::handle);
    }

    private static int nextId() {
        return index++;
    }

    public static void sendToServer(final Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(final ServerPlayer player, final Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToTracking(final Level level, final BlockPos pos, final double radius, final Object message) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, radius, level.dimension())), message);
    }

    public static void handleFireRequest(final ServerPlayer player, final C2SRequestFire message) {
        final Optional<OrbitalRailgunItem> held = OrbitalRailgunItem.getHeld(player);
        if (held.isEmpty()) {
            return;
        }

        final OrbitalRailgunItem railgunItem = held.get();
        if (player.getCooldowns().isOnCooldown(railgunItem)) {
            return;
        }

        if (message.chargeTicks() < OrbitalRailgunItem.MAX_CHARGE_TICKS) {
            return;
        }

        final HitResult serverResult = Raytracing.raycast(player, OrbitalRailgunItem.MAX_DISTANCE);
        if (serverResult.getType() == HitResult.Type.MISS) {
            sendToPlayer(player, new S2CUpdateClientState(0, false));
            return;
        }

        final BlockPos strikePos;
        final Vec3 hitLocation = serverResult.getLocation();
        final C2SRequestFire.HitType hitType;
        switch (serverResult.getType()) {
            case BLOCK -> {
                strikePos = ((BlockHitResult) serverResult).getBlockPos();
                hitType = C2SRequestFire.HitType.BLOCK;
            }
            case ENTITY -> {
                final Entity entity = ((EntityHitResult) serverResult).getEntity();
                strikePos = entity.blockPosition();
                hitType = C2SRequestFire.HitType.ENTITY;
            }
            default -> {
                strikePos = BlockPos.containing(hitLocation);
                hitType = C2SRequestFire.HitType.MISS;
            }
        }

        final ServerLevel level = player.serverLevel();
        railgunItem.handleServerFire(player, serverResult, message.chargeTicks());
        OrbitalRailgunStrikeManager.queueStrike(level, strikePos);

        final double distance = hitLocation.distanceTo(player.getEyePosition());
        final S2CFireAck ack = new S2CFireAck(level.dimension(), strikePos, hitLocation, hitType, distance, OrbitalRailgunItem.COOLDOWN_TICKS, level.getGameTime());
        sendToTracking(level, strikePos, 512.0D, ack);
    }
}
