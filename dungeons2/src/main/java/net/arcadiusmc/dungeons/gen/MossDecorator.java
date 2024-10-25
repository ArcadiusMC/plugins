package net.arcadiusmc.dungeons.gen;

import static org.bukkit.Material.ANDESITE;
import static org.bukkit.Material.ANDESITE_STAIRS;
import static org.bukkit.Material.COBBLESTONE;
import static org.bukkit.Material.COBBLESTONE_STAIRS;
import static org.bukkit.Material.DEEPSLATE;
import static org.bukkit.Material.MOSS_BLOCK;
import static org.bukkit.Material.MOSS_CARPET;
import static org.bukkit.Material.STONE;
import static org.bukkit.Material.STONE_BRICKS;

import com.mojang.serialization.Codec;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.NoiseParameter;
import net.arcadiusmc.dungeons.gen.MossDecorator.MossConfig;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.BlockFilterArgument;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

public class MossDecorator extends NoiseDecorator<MossConfig> implements XyzFunction {

  public static final DecoratorType<MossDecorator, MossConfig> TYPE
      = DecoratorType.create(MossConfig.CODEC, MossDecorator::new);

  public MossDecorator(MossConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    if (config.getBlocks().isEmpty()) {
      throw new IllegalStateException("No 'overgrowth-blocks' set");
    }

    runForEachBlock();
  }

  private BlockData getOvergrowthBlock() {
    return randomFrom(config.getBlocks()).createBlockData();
  }

  @Override
  public void accept(int x, int y, int z) {
    if (!testNoise(x, y, z)) {
      return;
    }

    if (isAir(x, y, z)) {
      boolean supportUp = hasSupport(x, y - 1, z, BlockFace.UP);
      Material carpet = config.carpetBlock;
      float rate = config.carpetRate;

      if (supportUp && carpet != null && randomBool(rate)) {
        setBlock(x, y, z, carpet.createBlockData());
        mossify(x, y, z);
      }

      return;
    }

    if (isTagged(x, y, z, Tag.LEAVES)
        && canOverrideLeavesMoss(x, y, z)
        && randomBool(0.25)
    ) {
      setBlock(x, y, z, getOvergrowthBlock());
      return;
    }

    if (!canReplace(x, y, z)) {
      return;
    }

    setBlock(x, y, z, getOvergrowthBlock());
    mossify(x, y, z);
  }

  boolean canOverrideLeavesMoss(int x, int y, int z) {
    if (!isAir(x, y + 1, z)) {
      return false;
    }

    return (hasSupport(x + 1, y, z, BlockFace.WEST) && !isTagged(x + 1, y, z, Tag.LEAVES))
        || (hasSupport(x - 1, y, z, BlockFace.EAST) && !isTagged(x - 1, y, z, Tag.LEAVES))
        || (hasSupport(x, y, z + 1, BlockFace.NORTH) && !isTagged(x, y, z + 1, Tag.LEAVES))
        || (hasSupport(x, y, z - 1, BlockFace.SOUTH) && !isTagged(x, y, z - 1, Tag.LEAVES));
  }

  boolean canReplace(int x, int y, int z) {
    BlockState state = getBlock(x, y, z);
    if (state == null) {
      return false;
    }

    if (matchesAny(state, config.getCanReplace())) {
      return true;
    }
    if (matchesAny(state, config.getMaybeReplace())) {
      return randomBool();
    }

    return false;
  }

  @Getter @Setter
  public static class MossConfig implements NoiseSettingHolder {

    static final List<BlockFilterArgument.Result> DEFAULT_REPLACABLE = List.of(
        BlockFilters.create(STONE),
        BlockFilters.create(ANDESITE),
        BlockFilters.create(COBBLESTONE)
    );

    static final List<BlockFilterArgument.Result> DEFAULT_MAYBE_REPLACE = List.of(
        BlockFilters.create(STONE_BRICKS),
        BlockFilters.create(DEEPSLATE),
        BlockFilters.create(ANDESITE_STAIRS),
        BlockFilters.create(COBBLESTONE_STAIRS)
    );

    static final Codec<MossConfig> CODEC = ExistingObjectCodec.createCodec(
        MossConfig::new,
        builder -> {
          builder.optional("noise", NoiseParameter.CODEC)
              .getter(MossConfig::getNoise)
              .setter(MossConfig::setNoise);

          builder.optional("replaceable", BlockFilters.CODEC.listOf())
              .getter(MossConfig::getCanReplace)
              .setter(MossConfig::setCanReplace);

          builder.optional("maybe-replaceable", BlockFilters.CODEC.listOf())
              .getter(MossConfig::getMaybeReplace)
              .setter(MossConfig::setMaybeReplace);

          builder.optional("overgrowth-blocks", ExtraCodecs.MATERIAL_CODEC.listOf())
              .getter(MossConfig::getBlocks)
              .setter(MossConfig::setBlocks);

          builder.optional("carpet", ExtraCodecs.MATERIAL_CODEC)
              .getter(MossConfig::getCarpetBlock)
              .setter(MossConfig::setCarpetBlock);

          builder.optional("carpet-rate", Codec.FLOAT)
              .getter(MossConfig::getCarpetRate)
              .setter(MossConfig::setCarpetRate);
        }
    );

    private NoiseParameter noise = new NoiseParameter();

    private List<BlockFilterArgument.Result> canReplace = DEFAULT_REPLACABLE;
    private List<BlockFilterArgument.Result> maybeReplace = DEFAULT_MAYBE_REPLACE;

    private List<Material> blocks = List.of(MOSS_BLOCK);
    private Material carpetBlock = MOSS_CARPET;
    private float carpetRate = 0.5f;

    @Override
    public NoiseParameter noiseParameters() {
      return noise;
    }
  }
}
