package com.mishkis.orbitalrailgun.util;

import com.mishkis.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OrbitalRailgunStrikeManager {
    private static final int RADIUS = 24;
    private static final int RADIUS_SQUARED = RADIUS * RADIUS;
    private static final boolean[][] MASK = new boolean[RADIUS * 2 + 1][RADIUS * 2 + 1];

    private static final ResourceKey<net.minecraft.world.damagesource.DamageType> STRIKE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "strike"));

    private static final Map<StrikeKey, StrikeData> ACTIVE_STRIKES = new ConcurrentHashMap<>();

    private OrbitalRailgunStrikeManager() {
    }

    public static void initialize() {
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                MASK[x + RADIUS][z + RADIUS] = x * x + z * z <= RADIUS_SQUARED;
            }
        }
    }

    public static void queueStrike(final ServerLevel level, final BlockPos origin) {
        final List<Integer> entities = new ArrayList<>();
        for (final Entity entity : level.getEntities(null, AABB.ofSize(origin.getCenter(), 1000.0D, 1000.0D, 1000.0D))) {
            entities.add(entity.getId());
        }
        ACTIVE_STRIKES.put(new StrikeKey(level.dimension(), origin), new StrikeData(level.dimension(), origin, entities, level.getServer().getTickCount()));
    }

    public static void tick(final MinecraftServer server) {
        ACTIVE_STRIKES.entrySet().removeIf(entry -> {
            final StrikeData data = entry.getValue();
            final ServerLevel level = server.getLevel(data.dimension());
            if (level == null) {
                return true;
            }

            final long age = server.getTickCount() - data.startedAt();
            if (age >= 700) {
                applyDamage(level, data);
                clearArea(level, data.origin());
                return true;
            }

            if (age >= 400) {
                pullEntities(level, data, age);
            }

            return false;
        });
    }

    private static void pullEntities(final ServerLevel level, final StrikeData data, final long age) {
        for (final int entityId : data.entityIds()) {
            final Entity entity = level.getEntity(entityId);
            if (entity == null) {
                continue;
            }

            final Vec3 direction = Vec3.atCenterOf(data.origin()).subtract(entity.position());
            final double magnitude = Math.min(1.0D / Math.abs(direction.length() - 20.0D) * 4.0D * (age - 400.0D) / 300.0D, 5.0D);
            final Vec3 normalized = direction.normalize();
            entity.push(normalized.x * magnitude, normalized.y * magnitude, normalized.z * magnitude);
            entity.hurtMarked = true;
        }
    }

    private static void applyDamage(final ServerLevel level, final StrikeData data) {
        final DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(STRIKE_DAMAGE));
        for (final int entityId : data.entityIds()) {
            final Entity entity = level.getEntity(entityId);
            if (entity == null) {
                continue;
            }

            if (entity.position().distanceToSqr(Vec3.atCenterOf(data.origin())) <= RADIUS_SQUARED) {
                entity.hurt(source, 100000.0F);
            }
        }
    }

    private static void clearArea(final ServerLevel level, final BlockPos origin) {
        final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y <= level.getMaxBuildHeight(); y++) {
            for (int x = -RADIUS; x <= RADIUS; x++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (MASK[x + RADIUS][z + RADIUS]) {
                        cursor.set(origin.getX() + x, y, origin.getZ() + z);
                        level.removeBlock(cursor, false);
                    }
                }
            }
        }
    }

    private record StrikeKey(ResourceKey<Level> dimension, BlockPos origin) {
    }

    private record StrikeData(ResourceKey<Level> dimension, BlockPos origin, List<Integer> entityIds, long startedAt) {
    }
}
