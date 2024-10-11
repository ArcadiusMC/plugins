package net.arcadiusmc.dungeons.gen;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.gen.FoliageDecorator.FoliageConfig;
import net.arcadiusmc.utils.WeightedList;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.BlockFilterArgument;
import org.bukkit.Material;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;

public class FoliageDecorator extends Decorator<FoliageConfig> implements XyzFunction {

  public static final DecoratorType<FoliageDecorator, FoliageConfig> TYPE
      = DecoratorType.create(FoliageConfig.CODEC, FoliageDecorator::new);

  public FoliageDecorator(FoliageConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    if (config.getCanGenerateOn().isEmpty()) {
      throw new IllegalStateException("No 'generate-on' blocks specified");
    }
    if (config.getBlocks().isEmpty()) {
      throw new IllegalStateException("No foliage blocks specified (under value 'blocks')");
    }

    runForEachBlock();
  }

  private boolean canGenerateOn(int x, int y, int z) {
    return matchesAny(x, y, z, config.getCanGenerateOn());
  }

  @Override
  public void accept(int x, int y, int z) {
    if (!isAir(x, y, z)) {
      return;
    }

    int by = y - 1;
    if (!canGenerateOn(x, by, z)) {
      return;
    }

    float rate = config.getRate();
    if (random.nextFloat() >= rate) {
      return;
    }

    WeightedList<Material> foliageList = config.getBlocks();

    if (foliageList.isEmpty()) {
      return;
    }

    Material m = foliageList.get(random);
    BlockData data = m.createBlockData();

    if (data instanceof Bisected bis) {
      bis.setHalf(Half.BOTTOM);
      setBlock(x, y, z, bis);

      int uy = y + 1;

      if (isAir(x, uy, z)) {
        Bisected upper = (Bisected) m.createBlockData();
        upper.setHalf(Half.TOP);
        setBlock(x, uy, z, upper);
      }

      return;
    }

    setBlock(x, y, z, data);
  }

  @Getter @Setter
  public static class FoliageConfig {
    static final Codec<WeightedList<Material>> MATERIALS_CODEC
        = Codec.mapPair(
            ExtraCodecs.MATERIAL_CODEC.fieldOf("value"),
            Codec.INT.optionalFieldOf("weight", 1)
        )
        .codec()
        .listOf()
        .xmap(
            pairs -> {
              WeightedList<Material> list = new WeightedList<>();
              pairs.forEach(p -> list.add(p.getSecond(), p.getFirst()));
              return list;
            },
            weighted -> ObjectLists.emptyList()
        );

    static final Codec<FoliageConfig> CODEC = ExistingObjectCodec.createCodec(
        FoliageConfig::new,
        builder -> {
          builder.field("blocks", MATERIALS_CODEC)
              .getter(FoliageConfig::getBlocks)
              .setter(FoliageConfig::setBlocks);

          builder.field("foliage-rate", Codec.FLOAT)
              .getter(FoliageConfig::getRate)
              .setter(FoliageConfig::setRate);

          builder.field("generate-on", BlockFilters.CODEC.listOf())
              .getter(FoliageConfig::getCanGenerateOn)
              .setter(FoliageConfig::setCanGenerateOn);
        }
    );

    float rate = 0.25f;
    WeightedList<Material> blocks = new WeightedList<>();
    List<BlockFilterArgument.Result> canGenerateOn = List.of();
  }
}
