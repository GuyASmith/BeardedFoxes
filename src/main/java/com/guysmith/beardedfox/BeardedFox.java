package com.guysmith.beardedfox;

import com.guysmith.beardedfox.entity.BeardedFoxEntity;
import com.guysmith.beardedfox.entity.DivineFoxEntity;
import com.guysmith.beardedfox.registry.ModEntityTypes;
import com.guysmith.beardedfox.registry.ModItems;
import com.guysmith.beardedfox.registry.SpawnInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public class BeardedFox implements ModInitializer {

    public static final String MOD_ID = "beardedfox";

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(ModEntityTypes.BEARDED_FOX, BeardedFoxEntity.createFoxAttributes());
        FabricDefaultAttributeRegistry.register(ModEntityTypes.DIVINE_FOX, DivineFoxEntity.createFoxAttributes());
        ModItems.registerItems(); // spawn egg
        SpawnInitializer.initialize(); // natural mob spawning
    }
}
