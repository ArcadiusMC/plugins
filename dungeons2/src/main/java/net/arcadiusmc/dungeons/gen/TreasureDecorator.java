package net.arcadiusmc.dungeons.gen;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.arcadiusmc.dungeons.gen.TreasureDecorator.TreasureConfig;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.BlockFilterArgument;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;

public class TreasureDecorator extends Decorator<TreasureConfig> {

  static final DecoratorType<TreasureDecorator, TreasureConfig> TYPE
      = DecoratorType.create(TreasureConfig.CODEC, TreasureDecorator::new);

  public TreasureDecorator(TreasureConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    if (config.lootTables.isEmpty()) {
      return;
    }

    List<LootTable> lootTables = new ObjectArrayList<>();
    for (NamespacedKey lootTable : config.lootTables) {
      LootTable table = Bukkit.getLootTable(lootTable);
      if (table != null) {
        lootTables.add(table);
      }
    }

    if (lootTables.isEmpty()) {
      return;
    }

    List<GeneratorFunction> treasures = getFunctions(LevelFunctions.TREASURE);
    if (treasures.isEmpty()) {
      return;
    }

    for (GeneratorFunction treasure : treasures) {
      processTreasure(treasure, lootTables);
    }
  }

  void processTreasure(GeneratorFunction f, List<LootTable> lootTables) {
    int x = f.getPosition().x();
    int y = f.getPosition().y();
    int z = f.getPosition().z();

    if (!canPlace(x, y, z)) {
      return;
    }

    BlockData data = randomFrom(config.chestMaterials).createBlockData();

    if (data instanceof Directional directional) {
      orient(directional, x, y, z);
    }

    BlockState state = data.createBlockState();

    if (state instanceof Lootable lootable) {
      LootTable table = randomFrom(lootTables);
      lootable.setLootTable(table);
    }

    setBlock(x, y, z, state);
  }

  void orient(Directional data, int x, int y, int z) {
    Set<BlockFace> allowed = data.getFaces();
  }

  boolean canPlace(int x, int y, int z) {
    BlockState state = getBlock(x, y, z);
    if (state == null || state.getType().isAir()) {
      return true;
    }

    return matchesAny(state, config.canReplace);
  }

  @Getter @Setter
  public static class TreasureConfig {
    static final List<BlockFilterArgument.Result> DEFAULT_CAN_REPLACE = List.of(
        BlockFilters.create(Material.GLOW_LICHEN),
        BlockFilters.create(Material.VINE),
        BlockFilters.parse("#flowers"),
        BlockFilters.parse("#leaves")
    );

    static final List<Material> CHEST_MATERIALS = List.of(Material.CHEST, Material.BARREL);

    static final Codec<TreasureConfig> CODEC = ExistingObjectCodec.createCodec(
        TreasureConfig::new,
        builder -> {
          builder.optional("can-replace", BlockFilters.CODEC.listOf())
              .getter(TreasureConfig::getCanReplace)
              .setter(TreasureConfig::setCanReplace);

          builder.optional("container-types", ExtraCodecs.MATERIAL_CODEC.listOf())
              .getter(TreasureConfig::getChestMaterials)
              .setter(TreasureConfig::setChestMaterials);

          builder.optional("loot-tables", ExtraCodecs.NAMESPACED_KEY.listOf())
              .getter(TreasureConfig::getLootTables)
              .setter(TreasureConfig::setLootTables);
        }
    );

    private List<BlockFilterArgument.Result> canReplace = DEFAULT_CAN_REPLACE;
    private List<Material> chestMaterials = CHEST_MATERIALS;
    private List<NamespacedKey> lootTables = List.of();
  }
}
