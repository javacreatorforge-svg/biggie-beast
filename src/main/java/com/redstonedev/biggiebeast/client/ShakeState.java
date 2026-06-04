package com.redstonedev.biggiebeast.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ShakeState {
    private ShakeState() {}
    public static volatile int ticks = 0;
    public static volatile float intensity = 0.0F;

    public static void add(int t, float i) {
        if (t > ticks) ticks = t;
        intensity = Math.max(intensity, i);
    }
    public static void tick() {
        if (ticks > 0) { ticks--; if (ticks == 0) intensity = 0.0F; }
    }
}
