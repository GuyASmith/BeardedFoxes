package com.guysmith.beardedfox.renderer;

import com.guysmith.beardedfox.BeardedFox;
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
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/cyan_fox_sleep.png");
                case RED:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/red_fox_sleep.png");
                case GRAY:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/gray_fox_sleep.png");
                case BROWN:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/brown_fox_sleep.png");
                case GREEN:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/green_fox_sleep.png");
                case WHITE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/white_fox_sleep.png");
                case LIGHT_BLUE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/light_blue_fox_sleep.png");
                case LIGHT_GRAY:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/light_gray_fox_sleep.png");
                case ORANGE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/orange_fox_sleep.png");
                case YELLOW:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/yellow_fox_sleep.png");
                case BLUE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/blue_fox_sleep.png");
                case BLACK:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/black_fox_sleep.png");
                case PINK:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/pink_fox_sleep.png");
                case MAGENTA:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/magenta_fox_sleep.png");
                case PURPLE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/purple_fox_sleep.png");
                default: // it must be Lime, or it's an error, which will show brightly
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/lime_fox_sleep.png");
            }
        } else {
            switch (entity.getFoxType()) {
                case CYAN:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/cyan_fox.png");
                case RED:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/red_fox.png");
                case GRAY:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/gray_fox.png");
                case BROWN:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/brown_fox.png");
                case GREEN:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/green_fox.png");
                case WHITE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/white_fox.png");
                case LIGHT_BLUE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/light_blue_fox.png");
                case LIGHT_GRAY:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/light_gray_fox.png");
                case ORANGE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/orange_fox.png");
                case YELLOW:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/yellow_fox.png");
                case BLUE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/blue_fox.png");
                case BLACK:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/black_fox.png");
                case PINK:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/pink_fox.png");
                case MAGENTA:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/magenta_fox.png");
                case PURPLE:
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/purple_fox.png");
                default: // must be Lime
                    return new Identifier(BeardedFox.MOD_ID, "textures/entity/bearded_fox/lime_fox.png");
            }
        }
    }
}
