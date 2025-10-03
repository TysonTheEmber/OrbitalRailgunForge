package com.mishkis.orbitalrailgun.registry;

import com.mishkis.orbitalrailgun.ForgeOrbitalRailgunMod;
import com.mishkis.orbitalrailgun.item.OrbitalRailgunItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, ForgeOrbitalRailgunMod.MOD_ID);

    public static final RegistryObject<OrbitalRailgunItem> ORBITAL_RAILGUN = REGISTER.register("orbital_railgun", OrbitalRailgunItem::new);

    private ModItems() {
    }
}
