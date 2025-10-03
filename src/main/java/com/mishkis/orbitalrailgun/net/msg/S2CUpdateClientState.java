package com.mishkis.orbitalrailgun.net.msg;

import com.mishkis.orbitalrailgun.client.RailgunClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record S2CUpdateClientState(int cooldownTicks, boolean isCharging) {
    public static void encode(final S2CUpdateClientState message, final FriendlyByteBuf buf) {
        buf.writeVarInt(message.cooldownTicks());
        buf.writeBoolean(message.isCharging());
    }

    public static S2CUpdateClientState decode(final FriendlyByteBuf buf) {
        final int cooldown = buf.readVarInt();
        final boolean charging = buf.readBoolean();
        return new S2CUpdateClientState(cooldown, charging);
    }

    public static void handle(final S2CUpdateClientState message, final Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> RailgunClientState.get().updateFromServer(message));
        context.setPacketHandled(true);
    }
}
