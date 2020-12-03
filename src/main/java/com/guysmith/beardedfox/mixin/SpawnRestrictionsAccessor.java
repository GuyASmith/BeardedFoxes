package com.guysmith.beardedfox.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// big thanks to the folks who made the Glow Squid mod, a lot of this code is identical because we needed to do the same stuff
@Mixin(SpawnRestriction.class)
public interface SpawnRestrictionsAccessor {
    @Invoker
    static <T extends MobEntity> void invokeRegister(EntityType<T> type, SpawnRestriction.Location location, Heightmap.Type hmType, SpawnRestriction.SpawnPredicate<T> predicate) {
        throw new UnsupportedOperationException();
    }
}
