package com.mishkis.orbitalrailgun.client.render.railgun;

import com.mishkis.orbitalrailgun.ForgeOrbitalRailgunMod;
import com.mishkis.orbitalrailgun.client.RailgunClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class PostChainManager implements ResourceManagerReloadListener {
    private final ResourceLocation shaderLocation;
    private final Predicate<RailgunClientState> shouldRender;

    private PostChain chain;

    private static final Field PASSES_FIELD = ObfuscationReflectionHelper.findField(PostChain.class, "passes");

    public PostChainManager(final ResourceLocation shaderLocation, final Predicate<RailgunClientState> shouldRender) {
        this.shaderLocation = shaderLocation;
        this.shouldRender = shouldRender;
    }

    @Override
    public void onResourceManagerReload(final ResourceManager resourceManager) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer == null) {
            return;
        }

        try {
            if (this.chain != null) {
                this.chain.close();
            }

            this.chain = new PostChain(minecraft.getTextureManager(), resourceManager, minecraft.getMainRenderTarget(), this.shaderLocation);
            this.chain.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
        } catch (final IOException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to load orbital railgun post chain {}", this.shaderLocation, exception);
            this.chain = null;
        }
    }

    public void render(final float partialTick, final boolean isGui) {
        final RailgunClientState state = RailgunClientState.get();
        if (this.chain == null || !this.shouldRender.test(state)) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        final Vec3 blockPosition = state.getTargetPosition();
        final float time = isGui ? state.getGuiShaderTime(partialTick) : state.getWorldShaderTime(partialTick);

        this.chain.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
        for (final PostPass pass : getPasses()) {
            final EffectInstance shader = pass.getEffect();
            Uniforms.setVec3(shader, "CameraPosition", cameraPosition);
            Uniforms.setVec3(shader, "BlockPosition", blockPosition);
            Uniforms.setFloat(shader, "iTime", time);
            Uniforms.setFloat(shader, "IsBlockHit", state.isBlockHit() ? 1.0F : 0.0F);
        }

        this.chain.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    @SuppressWarnings("unchecked")
    private List<PostPass> getPasses() {
        if (this.chain == null) {
            return Collections.emptyList();
        }

        try {
            return (List<PostPass>) PASSES_FIELD.get(this.chain);
        } catch (final IllegalAccessException exception) {
            ForgeOrbitalRailgunMod.LOGGER.error("Failed to access passes for shader {}", this.shaderLocation, exception);
            return Collections.emptyList();
        }
    }
}
