package com.redstonedev.biggiebeast.client;

import com.redstonedev.biggiebeast.BiggieBeast;
import com.redstonedev.biggiebeast.client.renderer.CaseOhRenderer;
import com.redstonedev.biggiebeast.init.ModEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Random;

public class ClientSetup {
    private static final Random RNG = new Random();

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> EntityRenderers.register(ModEntities.CASEOH.get(), CaseOhRenderer::new));
    }

    @Mod.EventBusSubscriber(modid = BiggieBeast.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) ShakeState.tick();
        }

        @SubscribeEvent
        public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            if (ShakeState.ticks <= 0) return;
            float mag = 3.5F * ShakeState.intensity * (ShakeState.ticks / 8.0F);
            event.setYaw(event.getYaw() + (RNG.nextFloat() - 0.5F) * mag);
            event.setPitch(event.getPitch() + (RNG.nextFloat() - 0.5F) * mag);
            event.setRoll(event.getRoll() + (RNG.nextFloat() - 0.5F) * mag);
        }
    }
}
