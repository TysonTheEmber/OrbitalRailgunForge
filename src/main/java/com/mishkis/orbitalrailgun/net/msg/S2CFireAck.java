package com.mishkis.orbitalrailgun.net.msg;

import com.mishkis.orbitalrailgun.client.RailgunClientState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CFireAck(ResourceKey<Level> dimension,
                         BlockPos blockPos,
                         Vec3 hitPosition,
                         C2SRequestFire.HitType hitType,
                         double hitDistance,
                         int cooldownTicks,
                         long serverGameTime) {

    public static void encode(final S2CFireAck message, final FriendlyByteBuf buf) {
        buf.writeResourceLocation(message.dimension().location());
        buf.writeBlockPos(message.blockPos());
        buf.writeDouble(message.hitPosition().x);
        buf.writeDouble(message.hitPosition().y);
        buf.writeDouble(message.hitPosition().z);
        buf.writeEnum(message.hitType());
        buf.writeDouble(message.hitDistance());
        buf.writeVarInt(message.cooldownTicks());
        buf.writeVarLong(message.serverGameTime());
    }

    public static S2CFireAck decode(final FriendlyByteBuf buf) {
        final ResourceLocation dimensionId = buf.readResourceLocation();
        final ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        final BlockPos blockPos = buf.readBlockPos();
        final Vec3 hitPos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        final C2SRequestFire.HitType hitType = buf.readEnum(C2SRequestFire.HitType.class);
        final double distance = buf.readDouble();
        final int cooldown = buf.readVarInt();
        final long gameTime = buf.readVarLong();
        return new S2CFireAck(dimension, blockPos, hitPos, hitType, distance, cooldown, gameTime);
    }

    public static void handle(final S2CFireAck message, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> RailgunClientState.get().onServerFire(message));
        context.setPacketHandled(true);
    }
}
