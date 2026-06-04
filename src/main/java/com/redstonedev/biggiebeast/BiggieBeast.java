package com.redstonedev.biggiebeast;

import com.mojang.logging.LogUtils;
import com.redstonedev.biggiebeast.client.ClientSetup;
import com.redstonedev.biggiebeast.entity.CaseOhEntity;
import com.redstonedev.biggiebeast.event.ForgeEvents;
import com.redstonedev.biggiebeast.init.ModEntities;
import com.redstonedev.biggiebeast.init.ModItems;
import com.redstonedev.biggiebeast.init.ModSounds;
import com.redstonedev.biggiebeast.network.PacketHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import software.bernie.geckolib3.GeckoLib;

@Mod(BiggieBeast.MODID)
public class BiggieBeast {
    public static final String MODID = "biggie_beast";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BiggieBeast() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        GeckoLib.initialize();
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::attributes);
        MinecraftForge.EVENT_BUS.register(new ForgeEvents());
    }

    private void commonSetup(final FMLCommonSetupEvent e) {
        e.enqueueWork(PacketHandler::register);
        LOGGER.info("Biggie Beast loaded - he can smell you");
    }
    private void clientSetup(final FMLClientSetupEvent e) { ClientSetup.onClientSetup(e); }
    private void attributes(final EntityAttributeCreationEvent e) {
        e.put(ModEntities.CASEOH.get(), CaseOhEntity.createAttributes().build());
    }
}
