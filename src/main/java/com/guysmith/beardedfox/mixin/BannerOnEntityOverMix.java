package com.guysmith.beardedfox.mixin;

import com.guysmith.beardedfox.registry.ModEntityTypes;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BannerItem.class)
abstract class BannerOnEntityOverMix extends Item {
    // the IDE wanted me to make this, dunno if it'll work...?
    public BannerOnEntityOverMix(Settings settings) {
        super(settings);
    }
    /*@Redirect(method = "super.useOnEntity",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/item/BannerItem;(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;"))
    private static ActionResult createBFBanner() {
        return ActionResult.PASS;
    }*/

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if(entity.getType() == ModEntityTypes.BEARDED_FOX) {
            BannerItem banner = (BannerItem)stack.getItem();

            CompoundTag tag = stack.getSubTag("BlockEntityTag");

            /*if(tag == null) { // will tag be null without patterns?
                return ActionResult.FAIL;
            }*/

            if(tag != null && tag.contains("Patterns")) {
                // i.e: the banners have specific data, don't count them as usable for this purpose
                return ActionResult.PASS;
            }

            ItemStack foxBanner = new ItemStack(Items.WHITE_BANNER);
            CompoundTag foxTag = foxBanner.getOrCreateSubTag("BlockEntityTag");
            ListTag listTag;

            switch(banner.getColor()) {
                case RED:
                    // add banner patterns
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.ORANGE)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.RED)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.RED)
                            .add(BannerPattern.CREEPER, DyeColor.RED)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.ORANGE)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.red_bf_banner").formatted(Formatting.GOLD));
                    break;
                case CYAN:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.BLUE)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.CYAN)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.CYAN)
                            .add(BannerPattern.CREEPER, DyeColor.CYAN)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.BLUE)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.cyan_bf_banner").formatted(Formatting.GOLD));
                    break;
                case BLUE:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.GRAY)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.BLUE)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.BLUE)
                            .add(BannerPattern.CREEPER, DyeColor.BLUE)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.GRAY)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.blue_bf_banner").formatted(Formatting.GOLD));
                    break;
                case GRAY:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.LIGHT_GRAY)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.GRAY)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.GRAY)
                            .add(BannerPattern.CREEPER, DyeColor.GRAY)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.LIGHT_GRAY)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.gray_bf_banner").formatted(Formatting.GOLD));
                    break;
                case LIME:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.YELLOW)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.LIME)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.LIME)
                            .add(BannerPattern.CREEPER, DyeColor.LIME)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.YELLOW)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.lime_bf_banner").formatted(Formatting.GOLD));
                    break;
                case PINK:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.MAGENTA)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.PINK)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.PINK)
                            .add(BannerPattern.CREEPER, DyeColor.PINK)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.MAGENTA)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.pink_bf_banner").formatted(Formatting.GOLD));
                    break;
                case BLACK:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.BLACK)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.GRAY)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.BLACK)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CREEPER, DyeColor.BLACK)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.BLACK)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.black_bf_banner").formatted(Formatting.GOLD));
                    break;
                case BROWN:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.GRAY)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.BROWN)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.BROWN)
                            .add(BannerPattern.CREEPER, DyeColor.BROWN)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.GRAY)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.brown_bf_banner").formatted(Formatting.GOLD));
                    break;
                case GREEN:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.BROWN)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.GREEN)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.GREEN)
                            .add(BannerPattern.CREEPER, DyeColor.GREEN)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.BROWN)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.green_bf_banner").formatted(Formatting.GOLD));
                    break;
                case WHITE:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.GRADIENT, DyeColor.LIGHT_GRAY) // added gradient so you can see the thing
                            .add(BannerPattern.STRIPE_CENTER, DyeColor.LIGHT_BLUE)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.WHITE)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.WHITE)
                            .add(BannerPattern.CREEPER, DyeColor.WHITE)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.LIGHT_BLUE)
                            .add(BannerPattern.GRADIENT, DyeColor.GRAY).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.white_bf_banner").formatted(Formatting.GOLD));
                    break;
                case ORANGE:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.YELLOW)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.ORANGE)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.ORANGE)
                            .add(BannerPattern.CREEPER, DyeColor.ORANGE)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.YELLOW)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.orange_bf_banner").formatted(Formatting.GOLD));
                    break;
                case PURPLE:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.GRAY)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.PURPLE)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.PURPLE)
                            .add(BannerPattern.CREEPER, DyeColor.PURPLE)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.GRAY)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.purple_bf_banner").formatted(Formatting.GOLD));
                    break;
                case YELLOW:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.ORANGE)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.YELLOW)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.YELLOW)
                            .add(BannerPattern.CREEPER, DyeColor.YELLOW)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.ORANGE)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.yellow_bf_banner").formatted(Formatting.GOLD));
                    break;
                case MAGENTA:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.PURPLE)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.MAGENTA)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.MAGENTA)
                            .add(BannerPattern.CREEPER, DyeColor.MAGENTA)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.PURPLE)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.magenta_bf_banner").formatted(Formatting.GOLD));
                    break;
                case LIGHT_BLUE:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.CYAN)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.LIGHT_BLUE)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.LIGHT_BLUE)
                            .add(BannerPattern.CREEPER, DyeColor.LIGHT_BLUE)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.CYAN)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.light_blue_bf_banner").formatted(Formatting.GOLD));
                    break;
                case LIGHT_GRAY:
                    listTag = (new BannerPattern.Patterns()).add(BannerPattern.STRIPE_CENTER, DyeColor.GRAY)
                            .add(BannerPattern.RHOMBUS_MIDDLE, DyeColor.BLACK)
                            .add(BannerPattern.CURLY_BORDER, DyeColor.LIGHT_GRAY)
                            .add(BannerPattern.CIRCLE_MIDDLE, DyeColor.LIGHT_GRAY)
                            .add(BannerPattern.CREEPER, DyeColor.LIGHT_GRAY)
                            .add(BannerPattern.TRIANGLE_TOP, DyeColor.GRAY)
                            .add(BannerPattern.GRADIENT, DyeColor.BLACK).toTag();
                    foxBanner.setCustomName(new TranslatableText("item.beardedfox.light_gray_bf_banner").formatted(Formatting.GOLD));
                    break;
                default:
                    listTag = null;
                    break;
            }
            if(listTag != null) {
                foxTag.put("Patterns", listTag);
                foxBanner.addHideFlag(ItemStack.TooltipSection.ADDITIONAL);
                user.inventory.insertStack(foxBanner);
                stack.decrement(1);
            } else {
                return ActionResult.FAIL;
            }
        }
        return ActionResult.PASS;
    }
}
