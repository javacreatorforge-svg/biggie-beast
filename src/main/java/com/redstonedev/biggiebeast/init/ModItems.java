package com.redstonedev.biggiebeast.init;

import com.redstonedev.biggiebeast.BiggieBeast;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BiggieBeast.MODID);

    public static final RegistryObject<ForgeSpawnEggItem> CASEOH_SPAWN_EGG =
            ITEMS.register("caseoh_spawn_egg", () -> new ForgeSpawnEggItem(
                    ModEntities.CASEOH, 0x230CB1, 0x190882,
                    new Item.Properties().tab(CreativeModeTab.TAB_MISC)));
}
