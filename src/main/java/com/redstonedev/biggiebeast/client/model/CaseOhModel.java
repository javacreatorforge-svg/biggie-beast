package com.redstonedev.biggiebeast.client.model;

import com.redstonedev.biggiebeast.BiggieBeast;
import com.redstonedev.biggiebeast.entity.CaseOhEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class CaseOhModel extends AnimatedGeoModel<CaseOhEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(BiggieBeast.MODID, "geo/caseoh.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(BiggieBeast.MODID, "textures/entity/caseoh.png");
    private static final ResourceLocation ANIM = new ResourceLocation(BiggieBeast.MODID, "animations/caseoh.animation.json");
    @Override public ResourceLocation getModelResource(CaseOhEntity e)     { return MODEL; }
    @Override public ResourceLocation getTextureResource(CaseOhEntity e)   { return TEXTURE; }
    @Override public ResourceLocation getAnimationResource(CaseOhEntity e) { return ANIM; }
}
