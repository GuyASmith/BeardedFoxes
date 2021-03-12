package com.guysmith.beardedfox.registry;

import com.guysmith.beardedfox.BeardedFox;
import com.guysmith.beardedfox.entity.BeardedFoxEntity;
import com.guysmith.beardedfox.entity.DivineFoxEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModEntityTypes {
    public static final EntityType<BeardedFoxEntity> BEARDED_FOX = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(BeardedFox.MOD_ID, "beardedfox"),
            FabricEntityTypeBuilder.<BeardedFoxEntity>create(SpawnGroup.CREATURE, BeardedFoxEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 0.7f)).build()
    );

    public static final EntityType<DivineFoxEntity> DIVINE_FOX = Registry.register(
            Registry.ENTITY_TYPE,
            new Identifier(BeardedFox.MOD_ID, "divinefox"),
            FabricEntityTypeBuilder.<DivineFoxEntity>create(SpawnGroup.MISC, DivineFoxEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 0.7f)).build()
    );
}
