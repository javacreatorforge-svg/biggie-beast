package com.redstonedev.biggiebeast.client.renderer;

import com.redstonedev.biggiebeast.client.model.CaseOhModel;
import com.redstonedev.biggiebeast.entity.CaseOhEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class CaseOhRenderer extends GeoEntityRenderer<CaseOhEntity> {
    public CaseOhRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CaseOhModel());
        this.shadowRadius = 0.9F;
    }
}
