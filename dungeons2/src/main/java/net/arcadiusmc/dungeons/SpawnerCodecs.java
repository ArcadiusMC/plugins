package net.arcadiusmc.dungeons;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.Results;
import org.apache.commons.lang3.Range;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.block.spawner.SpawnRule;
import org.bukkit.block.spawner.SpawnerEntry;
import org.bukkit.block.spawner.SpawnerEntry.Equipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

public final class SpawnerCodecs {
  private SpawnerCodecs() {}

  public static final int MIN_LIGHT = 0;
  public static final int MAX_LIGHT = 15;

  public static final Codec<Integer> LIGHT_LEVEL = Codec.intRange(MIN_LIGHT, MAX_LIGHT);

  public static final Codec<SpawnRule> RULE = ExistingObjectCodec.createCodec(
      SpawnerCodecs::emptyRule,
      builder -> {
        builder.optional("min-sky-light", LIGHT_LEVEL)
            .getter(SpawnRule::getMinSkyLight)
            .setter(SpawnRule::setMinSkyLight);
        builder.optional("max-sky-light", LIGHT_LEVEL)
            .getter(SpawnRule::getMaxSkyLight)
            .setter(SpawnRule::setMaxSkyLight);

        builder.optional("min-block-light", LIGHT_LEVEL)
            .getter(SpawnRule::getMinBlockLight)
            .setter(SpawnRule::setMinBlockLight);
        builder.optional("max-block-light", LIGHT_LEVEL)
            .getter(SpawnRule::getMaxBlockLight)
            .setter(SpawnRule::setMaxBlockLight);
      }
  );

  public static final Codec<Equipment> EQUIPMENT = ExistingObjectCodec.createCodec(
      SpawnerCodecs::emptyEquipment,
      builder -> {
        Codec<LootTable> loottableCodec = ExtraCodecs.NAMESPACED_KEY
            .comapFlatMap(
                key -> {
                  var table = Bukkit.getLootTable(key);
                  if (table == null) {
                    return Results.error("Unknown loot table: %s", key);
                  }

                  return Results.success(table);
                },
                Keyed::getKey
            );

        Codec<Map<EquipmentSlot, Float>> dropChanceMapCodec
            = Codec.unboundedMap(ExtraCodecs.enumCodec(EquipmentSlot.class), Codec.FLOAT);

        builder.optional("loot-table", loottableCodec)
            .getter(Equipment::getEquipmentLootTable)
            .setter(Equipment::setEquipmentLootTable);

        builder.optional("drop-chances", dropChanceMapCodec)
            .getter(Equipment::getDropChances)
            .setter((equipment, map) -> {
              equipment.getDropChances().clear();
              equipment.getDropChances().putAll(map);
            });
      }
  );

  public static final Codec<SpawnerEntry> SPAWNER_ENTRY = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            ExtraCodecs.ENTITY_SNAPSHOT.fieldOf("entity")
                .forGetter(SpawnerEntry::getSnapshot),

            RULE.optionalFieldOf("spawn-rule")
                .forGetter(o -> Optional.ofNullable(o.getSpawnRule())),

            EQUIPMENT.optionalFieldOf("equipment")
                .forGetter(o -> Optional.ofNullable(o.getEquipment())),

            Codec.INT.optionalFieldOf("weight", 10)
                .forGetter(SpawnerEntry::getSpawnWeight)
        )
        .apply(instance, (entitySnapshot, spawnRule, equipment, weight) -> {
          return new SpawnerEntry(
              entitySnapshot,
              weight,
              spawnRule.orElseGet(SpawnerCodecs::emptyRule),
              equipment.orElseGet(SpawnerCodecs::emptyEquipment)
          );
        });
  });

  public static SpawnRule emptyRule() {
    return new SpawnRule(MIN_LIGHT, MIN_LIGHT, MAX_LIGHT, MAX_LIGHT);
  }

  public static Equipment emptyEquipment() {
    return new Equipment(LootTables.EMPTY.getLootTable(), new HashMap<>());
  }
}
