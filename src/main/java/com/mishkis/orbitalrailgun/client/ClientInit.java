package com.mishkis.orbitalrailgun.client;

import com.mishkis.orbitalrailgun.ForgeOrbitalRailgunMod;
import com.mishkis.orbitalrailgun.client.event.ClientEvents;
import com.mishkis.orbitalrailgun.client.render.railgun.PostChainManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod.EventBusSubscriber(modid = ForgeOrbitalRailgunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientInit {
    private static final PostChainManager WORLD_POST = new PostChainManager(new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "shaders/post/orbital_railgun.json"), state -> state.shouldRenderWorldEffect());
    private static final PostChainManager GUI_POST = new PostChainManager(new ResourceLocation(ForgeOrbitalRailgunMod.MOD_ID, "shaders/post/orbital_railgun_gui.json"), state -> state.shouldRenderGuiEffect());

    private ClientInit() {
    }

    public static void register() {
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(ClientInit::onRegisterReloadListeners);
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Client events are registered via static subscribers.
        });
    }

    private static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(WORLD_POST);
        event.registerReloadListener(GUI_POST);
    }

    public static PostChainManager worldPost() {
        return WORLD_POST;
    }

    public static PostChainManager guiPost() {
        return GUI_POST;
    }
}
