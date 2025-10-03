package com.mishkis.orbitalrailgun.client;

import com.mishkis.orbitalrailgun.item.OrbitalRailgunItem;
import com.mishkis.orbitalrailgun.net.NetworkHandler;
import com.mishkis.orbitalrailgun.net.msg.C2SRequestFire;
import com.mishkis.orbitalrailgun.net.msg.S2CFireAck;
import com.mishkis.orbitalrailgun.net.msg.S2CUpdateClientState;
import com.mishkis.orbitalrailgun.registry.ModItems;
import com.mishkis.orbitalrailgun.util.Raytracing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RailgunClientState {
    private static final RailgunClientState INSTANCE = new RailgunClientState();

    private boolean charging;
    private boolean awaitingAck;
    private boolean hudOverrideActive;
    private boolean storedHudHidden;
    private int chargeTicks;
    private int cooldownTicks;
    private int effectTicks;
    private float worldShaderTime;
    private float guiShaderTime;
    private Vec3 targetPosition = Vec3.ZERO;
    private C2SRequestFire.HitType hitType = C2SRequestFire.HitType.MISS;
    private double hitDistance;
    private ResourceKey<Level> effectDimension;
    private boolean blockHit;

    private RailgunClientState() {
    }

    public static RailgunClientState get() {
        return INSTANCE;
    }

    public void clientTick(final Minecraft minecraft) {
        final LocalPlayer player = minecraft.player;
        if (player == null) {
            resetHud(minecraft);
            return;
        }

        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }

        final HitResult hitResult = minecraft.hitResult != null ? minecraft.hitResult : Raytracing.raycast(player, OrbitalRailgunItem.MAX_DISTANCE);
        updateHitData(player, hitResult);

        final ItemCooldowns cooldowns = player.getCooldowns();
        final boolean usingRailgun = player.isUsingItem() && player.getUseItem().is(ModItems.ORBITAL_RAILGUN.get());
        if (usingRailgun && !cooldowns.isOnCooldown(ModItems.ORBITAL_RAILGUN.get())) {
            if (!this.charging) {
                beginCharging(minecraft);
            }
            if (this.chargeTicks < OrbitalRailgunItem.MAX_CHARGE_TICKS) {
                this.chargeTicks++;
            }
            if (this.chargeTicks >= OrbitalRailgunItem.MAX_CHARGE_TICKS && !this.awaitingAck) {
                this.chargeTicks = OrbitalRailgunItem.MAX_CHARGE_TICKS;
                requestFire(player, hitResult);
            }
        } else if (this.charging && !this.awaitingAck) {
            stopCharging(minecraft);
        }

        if (this.awaitingAck) {
            this.guiShaderTime += 1.0F / 20.0F;
        } else if (!this.charging) {
            this.guiShaderTime = 0.0F;
        }

        if (this.effectTicks > 0) {
            this.effectTicks++;
            this.worldShaderTime += 1.0F / 20.0F;
            if (this.effectTicks >= 1600 || minecraft.level == null || !minecraft.level.dimension().equals(this.effectDimension)) {
                this.effectTicks = 0;
                this.worldShaderTime = 0.0F;
            }
        }

        applyHudOverride(minecraft, shouldHideHud());
    }

    private void beginCharging(final Minecraft minecraft) {
        this.charging = true;
        this.chargeTicks = 0;
        applyHudOverride(minecraft, true);
    }

    private void stopCharging(final Minecraft minecraft) {
        this.charging = false;
        this.chargeTicks = 0;
        applyHudOverride(minecraft, false);
    }

    private void applyHudOverride(final Minecraft minecraft, final boolean hide) {
        if (hide) {
            if (!this.hudOverrideActive) {
                this.storedHudHidden = minecraft.options.hideGui;
                this.hudOverrideActive = true;
            }
            minecraft.options.hideGui = true;
        } else if (this.hudOverrideActive) {
            minecraft.options.hideGui = this.storedHudHidden;
            this.hudOverrideActive = false;
        }
    }

    private void resetHud(final Minecraft minecraft) {
        if (this.hudOverrideActive) {
            minecraft.options.hideGui = this.storedHudHidden;
            this.hudOverrideActive = false;
        }
    }

    private void requestFire(final LocalPlayer player, final HitResult hitResult) {
        final C2SRequestFire.HitType type;
        BlockPos blockPos = null;
        int entityId = -1;
        Vec3 hitPos = hitResult != null ? hitResult.getLocation() : player.position();
        double distance = hitPos.distanceTo(player.getEyePosition());
        switch (hitResult.getType()) {
            case BLOCK -> {
                type = C2SRequestFire.HitType.BLOCK;
                blockPos = ((BlockHitResult) hitResult).getBlockPos();
            }
            case ENTITY -> {
                type = C2SRequestFire.HitType.ENTITY;
                entityId = ((EntityHitResult) hitResult).getEntity().getId();
                blockPos = ((EntityHitResult) hitResult).getEntity().blockPosition();
            }
            default -> type = C2SRequestFire.HitType.MISS;
        }

        this.awaitingAck = true;
        this.guiShaderTime = 0.0F;
        NetworkHandler.sendToServer(new C2SRequestFire(this.chargeTicks, type, blockPos, entityId, hitPos, distance));
        player.stopUsingItem();
    }

    private void updateHitData(final LocalPlayer player, final HitResult hitResult) {
        Vec3 position = player.position();
        this.hitType = C2SRequestFire.HitType.MISS;
        this.blockHit = false;
        this.hitDistance = 0.0D;

        if (hitResult != null) {
            position = hitResult.getLocation();
            this.hitDistance = position.distanceTo(player.getEyePosition());
            switch (hitResult.getType()) {
                case BLOCK -> {
                    this.hitType = C2SRequestFire.HitType.BLOCK;
                    this.blockHit = true;
                    this.targetPosition = ((BlockHitResult) hitResult).getBlockPos().getCenter();
                }
                case ENTITY -> {
                    this.hitType = C2SRequestFire.HitType.ENTITY;
                    this.blockHit = true;
                    final Entity entity = ((EntityHitResult) hitResult).getEntity();
                    this.targetPosition = entity.blockPosition().getCenter();
                }
                default -> this.targetPosition = position;
            }
        } else {
            this.targetPosition = player.position();
        }
    }

    public void onServerFire(final S2CFireAck message) {
        final Minecraft minecraft = Minecraft.getInstance();
        this.effectDimension = message.dimension();
        this.targetPosition = message.blockPos().getCenter();
        this.hitType = message.hitType();
        this.blockHit = message.hitType() != C2SRequestFire.HitType.MISS;
        this.hitDistance = message.hitDistance();
        this.worldShaderTime = 0.0F;
        this.effectTicks = 1;
        this.awaitingAck = false;
        this.charging = false;
        this.cooldownTicks = message.cooldownTicks();
        this.guiShaderTime = 0.0F;
        applyHudOverride(minecraft, false);
    }

    public void updateFromServer(final S2CUpdateClientState message) {
        this.cooldownTicks = message.cooldownTicks();
        if (!message.isCharging()) {
            final Minecraft minecraft = Minecraft.getInstance();
            stopCharging(minecraft);
            this.awaitingAck = false;
        }
    }

    public boolean shouldRenderWorldEffect() {
        return this.effectTicks > 0;
    }

    public boolean shouldRenderGuiEffect() {
        return this.charging || this.awaitingAck;
    }

    public Vec3 getTargetPosition() {
        return this.targetPosition;
    }

    public float getChargeProgress() {
        return this.charging ? (float) this.chargeTicks / (float) OrbitalRailgunItem.MAX_CHARGE_TICKS : 0.0F;
    }

    public boolean isCharging() {
        return this.charging;
    }

    public boolean isBlockHit() {
        return this.blockHit;
    }

    public float getWorldShaderTime(final float partialTick) {
        return this.worldShaderTime + partialTick / 20.0F;
    }

    public float getGuiShaderTime(final float partialTick) {
        return this.guiShaderTime + partialTick / 20.0F;
    }

    public double getHitDistance() {
        return this.hitDistance;
    }

    private boolean shouldHideHud() {
        return this.charging || this.awaitingAck;
    }

    public int getCooldownTicks() {
        return this.cooldownTicks;
    }

    public boolean isAwaitingAck() {
        return this.awaitingAck;
    }
}
