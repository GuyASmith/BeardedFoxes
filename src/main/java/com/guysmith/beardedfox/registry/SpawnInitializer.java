package com.guysmith.beardedfox.registry;

import com.google.common.collect.ImmutableMap;
import com.guysmith.beardedfox.BeardedFox;
import com.guysmith.beardedfox.entity.BeardedFoxEntity;
import com.guysmith.beardedfox.mixin.SpawnRestrictionsAccessor;
import com.guysmith.beardedfox.mixin.SpawnSettingsAccessor;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// big thanks to the folks who made the Glow Squid mod, a lot of this code is identical because we needed to do the same stuff
// vcokltf's Glow Squid mod  -  https://github.com/CarbonAgony/GlowSquid
// apparently, their source for this was YanisBft's MooBlooms mod  -  https://github.com/YanisBft/MooBlooms
public class SpawnInitializer {
    private static final List<RegistryKey<Biome>> cyanBiomes = BeardedFoxEntity.Type.CYAN.getBiomes();

    public static void initialize() {
        SpawnRestrictionsAccessor.invokeRegister(ModEntityTypes.BEARDED_FOX, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, BeardedFoxEntity::canMobSpawn);

        for(Biome biome: BuiltinRegistries.BIOME) {
            for(RegistryKey<Biome> key : cyanBiomes) {
                if(biome == BuiltinRegistries.BIOME.get(key)) {
                    addSpawnToBiome(biome, new SpawnSettings.SpawnEntry(ModEntityTypes.BEARDED_FOX, 6, 2,4));
                    break; // speeds up the process
                }
            }
        }
    }

    private static void addSpawnToBiome(Biome biome, SpawnSettings.SpawnEntry... spawners) {
        convertImmutableSpawners(biome);
        List<SpawnSettings.SpawnEntry> spawnEntries = new ArrayList<>(((SpawnSettingsAccessor) biome.getSpawnSettings()).getSpawners().get(SpawnGroup.CREATURE));
        spawnEntries.addAll(Arrays.asList(spawners));
        ((SpawnSettingsAccessor) biome.getSpawnSettings()).getSpawners().put(SpawnGroup.CREATURE, spawnEntries);
    }

    private static void convertImmutableSpawners(Biome biome) {
        if(((SpawnSettingsAccessor) biome.getSpawnSettings()).getSpawners() instanceof ImmutableMap) {
            ((SpawnSettingsAccessor) biome.getSpawnSettings()).setSpawners(new HashMap<>((((SpawnSettingsAccessor) biome.getSpawnSettings()).getSpawners())));
        }
    }
}
