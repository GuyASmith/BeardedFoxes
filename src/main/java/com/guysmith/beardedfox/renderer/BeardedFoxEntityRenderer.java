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
            switch (entity.getFoxType()) {
                case CYAN:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/cyan_fox_sleep.png");
                case RED:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/red_fox_sleep.png");
                case GRAY:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/gray_fox_sleep.png");
                case BROWN:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/brown_fox_sleep.png");
                case GREEN:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/green_fox_sleep.png");
                case WHITE:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/white_fox_sleep.png");
                default: // also Cyan
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/fox_sleep.png");
            }
        } else {
            switch (entity.getFoxType()) {
                case CYAN:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/cyan_fox.png");
                case RED:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/red_fox.png");
                case GRAY:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/gray_fox.png");
                case BROWN:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/brown_fox.png");
                case GREEN:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/green_fox.png");
                case WHITE:
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/white_fox.png");
                default: // also Cyan
                    return new Identifier("beardedfox", "textures/entity/bearded_fox/fox.png");
            }
        }
    }
}
