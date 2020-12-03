package com.guysmith.beardedfox.registry;

import com.guysmith.beardedfox.BeardedFox;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {

    public static void registerItems() {
        Registry.register(Registry.ITEM, new Identifier("beardedfox", "beardedfox_spawn_egg"), new SpawnEggItem(BeardedFox.BEARDED_FOX_ENTITY_TYPE, 0x169B9B, 0x1A89BF, new Item.Settings().group(ItemGroup.MISC)));
    }
}
