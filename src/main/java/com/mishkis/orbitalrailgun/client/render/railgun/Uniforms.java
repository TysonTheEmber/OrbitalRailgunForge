package com.mishkis.orbitalrailgun.client.render.railgun;

import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.world.phys.Vec3;

public final class Uniforms {
    private Uniforms() {
    }

    public static void setFloat(final EffectInstance shader, final String name, final float value) {
        final Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    public static void setVec3(final EffectInstance shader, final String name, final Vec3 vec) {
        final Uniform uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set((float) vec.x, (float) vec.y, (float) vec.z);
        }
    }
}
