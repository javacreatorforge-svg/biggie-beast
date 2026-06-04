package com.redstonedev.biggiebeast.init;

import com.redstonedev.biggiebeast.BiggieBeast;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BiggieBeast.MODID);

    public static final RegistryObject<SoundEvent> STOMP = register("stomp");
    public static final RegistryObject<SoundEvent> DEATH = register("death");
    public static final List<RegistryObject<SoundEvent>> AMBIENT = new ArrayList<>();
    static {
        AMBIENT.add(register("ambient1"));
        AMBIENT.add(register("ambient2"));
        AMBIENT.add(register("ambient3"));
        AMBIENT.add(register("ambient4"));
    }
    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> new SoundEvent(new ResourceLocation(BiggieBeast.MODID, name)));
    }
}
