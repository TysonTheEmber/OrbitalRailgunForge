package com.mishkis.orbitalrailgun.net.msg;

import com.mishkis.orbitalrailgun.net.NetworkHandler;
import com.mishkis.orbitalrailgun.util.Raytracing;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SRequestFire(int chargeTicks, HitType hitType, BlockPos blockPos, int entityId, Vec3 hitPos, double hitDistance) {
    public enum HitType {
        BLOCK,
        ENTITY,
        MISS
    }

    public static void encode(final C2SRequestFire message, final net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeVarInt(message.chargeTicks());
        buf.writeEnum(message.hitType());
        buf.writeBoolean(message.blockPos() != null);
        if (message.blockPos() != null) {
            buf.writeBlockPos(message.blockPos());
        }
        buf.writeVarInt(message.entityId());
        buf.writeBoolean(message.hitPos() != null);
        if (message.hitPos() != null) {
            buf.writeDouble(message.hitPos().x);
            buf.writeDouble(message.hitPos().y);
            buf.writeDouble(message.hitPos().z);
        }
        buf.writeDouble(message.hitDistance());
    }

    public static C2SRequestFire decode(final net.minecraft.network.FriendlyByteBuf buf) {
        final int chargeTicks = buf.readVarInt();
        final HitType type = buf.readEnum(HitType.class);
        final BlockPos blockPos = buf.readBoolean() ? buf.readBlockPos() : null;
        final int entityId = buf.readVarInt();
        final Vec3 hitPos = buf.readBoolean() ? new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()) : null;
        final double distance = buf.readDouble();
        return new C2SRequestFire(chargeTicks, type, blockPos, entityId, hitPos, distance);
    }

    public static void handle(final C2SRequestFire message, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            NetworkHandler.handleFireRequest(player, message);
        });
        context.setPacketHandled(true);
    }
}
