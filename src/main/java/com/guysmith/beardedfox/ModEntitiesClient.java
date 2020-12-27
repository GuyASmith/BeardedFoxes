package com.guysmith.beardedfox;

import com.guysmith.beardedfox.model.BeardedFoxEntityModel;
import com.guysmith.beardedfox.registry.ModEntityTypes;
import com.guysmith.beardedfox.renderer.BeardedFoxEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;

@Environment(EnvType.CLIENT)
public class ModEntitiesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.INSTANCE.register(ModEntityTypes.BEARDED_FOX, ((entityRenderDispatcher, context) -> {
            return new BeardedFoxEntityRenderer(entityRenderDispatcher, new BeardedFoxEntityModel(), 0.5f); // float is shadowRadius apparently
        }));
    }
}
