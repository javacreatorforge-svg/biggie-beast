package com.redstonedev.biggiebeast.init;

import com.redstonedev.biggiebeast.BiggieBeast;
import com.redstonedev.biggiebeast.entity.CaseOhEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BiggieBeast.MODID);

    public static final RegistryObject<EntityType<CaseOhEntity>> CASEOH =
            ENTITIES.register("caseoh", () -> EntityType.Builder
                    .<CaseOhEntity>of(CaseOhEntity::new, MobCategory.MONSTER)
                    .sized(1.6F, 3.2F).clientTrackingRange(20)
                    .build(new ResourceLocation(BiggieBeast.MODID, "caseoh").toString()));
}
