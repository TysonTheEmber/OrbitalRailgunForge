package com.mishkis.orbitalrailgun.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Raytracing {
    private Raytracing() {
    }

    public static HitResult raycast(final Player player, final double distance) {
        final Level level = player.level();
        final Vec3 eyePosition = player.getEyePosition(1.0F);
        final Vec3 lookDirection = player.getViewVector(1.0F);
        final Vec3 reach = eyePosition.add(lookDirection.scale(distance));

        final HitResult blockResult = level.clip(new ClipContext(eyePosition, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = distance;
        if (blockResult != null && blockResult.getType() != HitResult.Type.MISS) {
            maxDistance = blockResult.getLocation().distanceTo(eyePosition);
        }

        final AABB bounds = player.getBoundingBox().expandTowards(lookDirection.scale(distance)).inflate(1.0D);
        final EntityHitResult entityResult = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level, player, eyePosition, reach, bounds, Raytracing::canHitEntity, (float) maxDistance);

        if (entityResult != null) {
            return entityResult;
        }

        if (blockResult != null) {
            return blockResult;
        }

        return BlockHitResult.miss(reach, Direction.getNearest(lookDirection.x, lookDirection.y, lookDirection.z), BlockPos.containing(reach));
    }

    private static boolean canHitEntity(final Entity entity) {
        return !entity.isSpectator() && entity.isPickable();
    }
}
