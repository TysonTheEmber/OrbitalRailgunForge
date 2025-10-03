package com.mishkis.orbitalrailgun.item;

import com.mishkis.orbitalrailgun.net.NetworkHandler;
import com.mishkis.orbitalrailgun.net.msg.S2CUpdateClientState;
import com.mishkis.orbitalrailgun.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public class OrbitalRailgunItem extends Item {
    public static final int MAX_CHARGE_TICKS = 80;
    public static final int COOLDOWN_TICKS = 2400;
    public static final double MAX_DISTANCE = 300.0D;

    public OrbitalRailgunItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(final ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(final ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void releaseUsing(final ItemStack stack, final Level level, final LivingEntity entity, final int timeLeft) {
        if (!level.isClientSide && entity instanceof final ServerPlayer serverPlayer) {
            final int usedTicks = this.getUseDuration(stack) - timeLeft;
            if (usedTicks < MAX_CHARGE_TICKS) {
                NetworkHandler.sendToPlayer(serverPlayer, new S2CUpdateClientState(0, false));
            }
        }
    }

    public void handleServerFire(final ServerPlayer player, final HitResult hitResult, final int chargeTicks) {
        if (chargeTicks < MAX_CHARGE_TICKS) {
            return;
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
    }

    public static boolean isHolding(final Player player) {
        return player != null && (player.getMainHandItem().is(ModItems.ORBITAL_RAILGUN.get()) || player.getOffhandItem().is(ModItems.ORBITAL_RAILGUN.get()));
    }

    public static Optional<OrbitalRailgunItem> getHeld(final Player player) {
        if (player == null) {
            return Optional.empty();
        }

        final ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof final OrbitalRailgunItem item) {
            return Optional.of(item);
        }

        final ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof final OrbitalRailgunItem item) {
            return Optional.of(item);
        }

        return Optional.empty();
    }
}
