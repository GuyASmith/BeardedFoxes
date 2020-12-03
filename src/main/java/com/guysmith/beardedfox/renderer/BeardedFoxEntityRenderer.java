package com.guysmith.beardedfox.renderer;

import com.guysmith.beardedfox.entity.BeardedFoxEntity;
import com.guysmith.beardedfox.model.BeardedFoxEntityModel;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class BeardedFoxEntityRenderer extends MobEntityRenderer<BeardedFoxEntity, BeardedFoxEntityModel> {

    public BeardedFoxEntityRenderer(EntityRenderDispatcher entityRenderDispatcher, BeardedFoxEntityModel entityModel, float f) {
        super(entityRenderDispatcher, entityModel, f);
    }

    @Override
    public Identifier getTexture(BeardedFoxEntity entity) {
        if(entity.isSleeping()) {
            return new Identifier("beardedfox", "textures/entity/bearded_fox/fox_sleep.png");
        } else {
            return new Identifier("beardedfox", "textures/entity/bearded_fox/fox.png");
        }
    }
}
