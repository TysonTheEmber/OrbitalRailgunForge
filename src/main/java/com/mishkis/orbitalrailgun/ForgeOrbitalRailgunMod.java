package com.mishkis.orbitalrailgun;

import com.mishkis.orbitalrailgun.net.NetworkHandler;
import com.mishkis.orbitalrailgun.registry.ModItems;
import com.mishkis.orbitalrailgun.util.OrbitalRailgunStrikeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ForgeOrbitalRailgunMod.MOD_ID)
public final class ForgeOrbitalRailgunMod {
    public static final String MOD_ID = "orbital_railgun";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public ForgeOrbitalRailgunMod() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.REGISTER.register(modBus);

        MinecraftForge.EVENT_BUS.register(this);

        NetworkHandler.init();
        OrbitalRailgunStrikeManager.initialize();
    }

    @SubscribeEvent
    public void handleServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer() != null) {
            OrbitalRailgunStrikeManager.tick(event.getServer());
        }
    }
}
