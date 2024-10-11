package net.arcadiusmc.bank;

import com.google.common.base.Strings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.loot.LootTable;

@Getter @Setter
public class ChestGroup {

  static final Codec<ChestGroup> CODEC = RecordCodecBuilder.create(instance -> {
    return instance
        .group(
            FacingPosition.CODEC.listOf()
                .optionalFieldOf("positions", List.of())
                .forGetter(ChestGroup::getPositions),

            Codec.STRING
                .optionalFieldOf("loot-table", "")
                .forGetter(ChestGroup::getLootTableId)
        )
        .apply(instance, (positions, lootTableKey) -> {
          ChestGroup group = new ChestGroup();
          group.getPositions().addAll(positions);
          group.setLootTableId(lootTableKey);
          return group;
        });
  });

  private final List<FacingPosition> positions = new ArrayList<>();
  private String lootTableId = "";

  public void spawn(World world, Random random, String variant, VaultVariationTable table) {
    if (positions.isEmpty() || Strings.isNullOrEmpty(lootTableId)) {
      return;
    }

    LootTable lootTable = table.getLootTable(variant, lootTableId);

    for (FacingPosition position : positions) {
      Block block = world.getBlockAt(position.x(), position.y(), position.z());

      if (block.getType() != Material.CHEST) {
        org.bukkit.block.data.type.Chest blockData
            = (org.bukkit.block.data.type.Chest) Material.CHEST.createBlockData();

        blockData.setFacing(position.facing());
        block.setBlockData(blockData, false);
      }

      Chest chest = (Chest) block.getState();

      chest.getSnapshotInventory().clear();
      chest.setLootTable(lootTable);
      chest.update(true, false);
    }
  }

}
