package com.mishkis.orbitalrailgun.client.event;

import com.mishkis.orbitalrailgun.client.RailgunClientState;
import com.mishkis.orbitalrailgun.item.OrbitalRailgunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
public final class ClientEvents {

    @SubscribeEvent
    public void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RailgunClientState.get().clientTick(Minecraft.getInstance());
        }
    }

    @SubscribeEvent
    public void onComputeFov(final ViewportEvent.ComputeFov event) {
        final RailgunClientState state = RailgunClientState.get();
        if (state.isCharging() || state.isAwaitingAck()) {
            event.setFOV(Minecraft.getInstance().options.fov().get());
        }
    }

    @SubscribeEvent
    public void onRenderLevelStage(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }

        final float partialTick = event.getPartialTick();
        com.mishkis.orbitalrailgun.client.ClientInit.worldPost().render(partialTick, false);
        com.mishkis.orbitalrailgun.client.ClientInit.guiPost().render(partialTick, true);
    }

    @SubscribeEvent
    public void onRenderGui(final RenderGuiEvent.Post event) {
        final RailgunClientState state = RailgunClientState.get();
        if (!state.shouldRenderGuiEffect()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final GuiGraphics graphics = event.getGuiGraphics();
        final int width = minecraft.getWindow().getGuiScaledWidth();
        final int height = minecraft.getWindow().getGuiScaledHeight();
        final int centerX = width / 2;
        final int centerY = height / 2;

        final float charge = state.getChargeProgress();
        final int barWidth = 140;
        final int barHeight = 6;
        final int barX = centerX - barWidth / 2;
        final int barY = centerY + 60;
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0x80000000);
        graphics.fill(barX, barY, barX + Math.round(barWidth * charge), barY + barHeight, 0xCC00FFFF);

        if (state.getCooldownTicks() > 0) {
            final int cooldownWidth = 120;
            final int cooldownHeight = 4;
            final int cooldownX = centerX - cooldownWidth / 2;
            final int cooldownY = barY + 12;
            graphics.fill(cooldownX, cooldownY, cooldownX + cooldownWidth, cooldownY + cooldownHeight, 0x80000000);
            final float cooldownProgress = Mth.clamp(1.0F - (state.getCooldownTicks() / (float) OrbitalRailgunItem.COOLDOWN_TICKS), 0.0F, 1.0F);
            graphics.fill(cooldownX, cooldownY, cooldownX + Math.round(cooldownWidth * cooldownProgress), cooldownY + cooldownHeight, 0xCCFF5555);
        }
    }
}
