package com.guysmith.beardedfox.registry;

import com.guysmith.beardedfox.BeardedFox;
import com.guysmith.beardedfox.entity.BeardedFoxEntity;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {

    private static final Item BEARDED_FOX_SPAWN_EGG = new SpawnEggItem(BeardedFox.BEARDED_FOX_ENTITY_TYPE, 0x169B9B, 0x1A89BF, new Item.Settings().group(ItemGroup.MISC));

    public static void registerItems() {
        Registry.register(Registry.ITEM, new Identifier("beardedfox", "beardedfox_spawn_egg"), BEARDED_FOX_SPAWN_EGG);
    }

    /*public class ModifiedBannerItem extends BannerItem { // I'm not doing this, per se

        public ModifiedBannerItem(Block block, Block block2, Settings settings) {
            super(block, block2, settings);
        }

        @Override
        public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        // make it so that whenever I use a Banner on a BeardedFox, I get a Bearded Fox Banner
            return super.useOnEntity(stack, user, entity, hand);
        }

        // Its address or whatever it's called (maybe to be used in a redirect?):
        (Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;
    }*/
}
