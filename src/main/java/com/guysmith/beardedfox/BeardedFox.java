package com.guysmith.beardedfox;

import com.guysmith.beardedfox.entity.BeardedFoxEntity;
import com.guysmith.beardedfox.registry.ModItems;
import com.guysmith.beardedfox.registry.SpawnInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class BeardedFox implements ModInitializer {

    public static final String MOD_ID = "beardedfox";

    public static final EntityType<BeardedFoxEntity> BEARDED_FOX_ENTITY_TYPE = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(MOD_ID, "beardedfox"),
            FabricEntityTypeBuilder.<BeardedFoxEntity>create(SpawnGroup.CREATURE, BeardedFoxEntity::new).dimensions(EntityDimensions.fixed(0.6f, 0.7f)).build()
    );

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(BEARDED_FOX_ENTITY_TYPE, BeardedFoxEntity.createFoxAttributes());
        ModItems.registerItems(); // spawn egg
        SpawnInitializer.initialize();// natural mob spawning
    }
}
