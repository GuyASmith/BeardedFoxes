package com.guysmith.beardedfox.mixin;

import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.SpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

// big thanks to the folks who made the Glow Squid mod, a lot of this code is identical because we needed to do the same stuff
@Mixin(SpawnSettings.class)
public interface SpawnSettingsAccessor {
    @Accessor("spawners")
    Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> getSpawners();

    @Accessor("spawners")
    void setSpawners(Map<SpawnGroup, List<SpawnSettings.SpawnEntry>> spawners);
}
