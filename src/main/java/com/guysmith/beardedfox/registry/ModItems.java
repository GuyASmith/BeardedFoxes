package com.guysmith.beardedfox.registry;

import net.minecraft.item.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {

    private static final Item BEARDED_FOX_SPAWN_EGG = new SpawnEggItem(ModEntityTypes.BEARDED_FOX, 0x169B9B, 0x1A89BF, new Item.Settings().group(ItemGroup.MISC));

    public static void registerItems() {
        Registry.register(Registry.ITEM, new Identifier("beardedfox", "beardedfox_spawn_egg"), BEARDED_FOX_SPAWN_EGG);
    }
}
