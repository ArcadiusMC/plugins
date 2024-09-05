package net.arcadiusmc.bank;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.utils.io.ExtraCodecs;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.LootTables;

@Getter @Setter
public class VaultVariationTable {

  static final Codec<VaultVariationTable> CODEC
      = Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, ExtraCodecs.NAMESPACED_KEY))
      .xmap(
          map -> {
            VaultVariationTable table = new VaultVariationTable();

            map.forEach((s, innerMap) -> {
              innerMap.forEach((s1, key) -> {
                table.getVariantTable().put(s, s1, key);
              });
            });

            return table;
          },
          table -> table.getVariantTable().rowMap()
      );

  private final Table<String, String, NamespacedKey> variantTable = HashBasedTable.create();

  private static LootTable emptyLootTable() {
    return LootTables.EMPTY.getLootTable();
  }

  private LootTable fallback(String key) {
    NamespacedKey lootTableKey = NamespacedKey.fromString(key);
    if (lootTableKey == null) {
      return emptyLootTable();
    }

    LootTable table = Bukkit.getLootTable(lootTableKey);
    if (table == null) {
      return emptyLootTable();
    }

    return table;
  }

  public LootTable getLootTable(String variation, String key) {
    NamespacedKey lootTableKey = variantTable.get(variation, key);
    if (lootTableKey == null) {
      return fallback(key);
    }

    LootTable table = Bukkit.getLootTable(lootTableKey);
    if (table == null) {
      return fallback(key);
    }

    return table;
  }
}
