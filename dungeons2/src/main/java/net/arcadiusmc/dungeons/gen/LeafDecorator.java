package net.arcadiusmc.dungeons.gen;

import static org.bukkit.Material.AZALEA_LEAVES;
import static org.bukkit.Material.BIRCH_LEAVES;
import static org.bukkit.Material.DARK_OAK_LEAVES;
import static org.bukkit.Material.GLOW_LICHEN;
import static org.bukkit.Material.MANGROVE_LEAVES;
import static org.bukkit.Material.OAK_LEAVES;
import static org.bukkit.Material.SHORT_GRASS;
import static org.bukkit.Material.SPRUCE_LEAVES;
import static org.bukkit.Material.TALL_GRASS;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.dungeons.NoiseParameter;
import net.arcadiusmc.dungeons.gen.LeafDecorator.LeafConfig;
import net.arcadiusmc.utils.io.ExistingObjectCodec;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.forthecrown.grenadier.types.BlockFilterArgument;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.joml.Vector2i;

public class LeafDecorator extends NoiseDecorator<LeafConfig> implements XyzFunction {

  public static final DecoratorType<LeafDecorator, LeafConfig> TYPE
      = DecoratorType.create(LeafConfig.CODEC, LeafDecorator::new);

  final int[] xMoves = {-1, 0, 1};
  final int[] yMoves = {-1, 0, 1};
  final int[] zMoves = {-1, 0, 1};

  int lastXMove = 0;
  int lastZMove = 0;

  public LeafDecorator(LeafConfig config) {
    super(config);
  }

  @Override
  public void execute() {
    if (config.leafMaterials.isEmpty()) {
      throw new IllegalStateException("No 'leaf-blocks' set");
    }
    if (config.maxLength < 1) {
      throw new IllegalStateException("Max leaf length less than 1");
    }
    if (!config.ceilingSnakeAllowed && !config.floorSnakeAllowed) {
      throw new IllegalStateException(
          "Both ceiling-leaves-allowed and floor-leaves-allowed are set to false"
      );
    }

    runForEachBlock();
  }

  private boolean hasLeafBlocks() {
    return !config.getLeafMaterials().isEmpty();
  }

  private BlockData getLeafBlock() {
    List<Material> list = config.getLeafMaterials();

    if (list.isEmpty()) {
      return null;
    }

    return list.get(random.nextInt(list.size())).createBlockData();
  }

  @Override
  public void accept(int x, int y, int z) {
    if (isSkyAbove(x, y, z) || isVoidBelow(x, y, z) || !isAir(x, y, z)) {
      return;
    }
    if (!testNoise(x, y, z)) {
      return;
    }
    if (!hasLeafBlocks()) {
      return;
    }
    if (!hasSupport(x, y + 1, z, BlockFace.DOWN)) {
      return;
    }

    lastXMove = 0;
    lastZMove = 0;

    if (hasSupport(x, y - 1, z, BlockFace.UP) && config.isFloorSnakeAllowed()) {
      groundSnakeLeaves(x, y, z);
      return;
    }

    if (!config.isCeilingSnakeAllowed()) {
      return;
    }

    ceilingSnakeLeaves(x, y, z);
  }

  private boolean isLeafReplaceable(int x, int y, int z) {
    if (isAir(x, y, z)) {
      return true;
    }

    Material type = getBlockType(x, y, z);
    return config.getLeavesCanReplace().contains(type);
  }

  private void groundSnakeLeaves(int x, int y, int z) {
    int maxLength = random.nextInt(config.getMaxLength());
    int len = 0;

    Vector2i move = new Vector2i();

    while (len < maxLength) {
      setBlock(x, y, z, getLeafBlock());
      mossify(x, y, z);

      len++;

      if (isLeafReplaceable(x, y - 1, z)) {
        y--;
        continue;
      }

      if (!findNextValidMove(x, y, z, move)) {
        break;
      }

      x = move.x;
      z = move.y;
    }
  }

  private boolean findNextValidMove(int x, int y, int z, Vector2i out) {
    IntArrays.shuffle(xMoves, random);

    for (int mx : xMoves) {
      IntArrays.shuffle(zMoves, random);

      for (int mz : zMoves) {
        int bx = mx + x;
        int bz = mz + z;

        if (!isLeafReplaceable(bx, y, bz)) {
          continue;
        }
        if (mx != lastXMove && mz != lastZMove) {
          continue;
        }

        lastXMove = mx;
        lastZMove = mz;

        out.x = bx;
        out.y = bz;

        return true;
      }
    }

    return false;
  }

  private void ceilingSnakeLeaves(int x, int y, int z) {
    int upperBound = config.getMaxLength();
    int length = random.nextInt(upperBound);

    if (length < 1) {
      return;
    }

    int dripLength = random.nextInt(length);
    int snakeLength = length - dripLength;

    int currentLength = 0;

    Vector2i nextPosition = new Vector2i();
    org.joml.Vector3i nextPos3i = new org.joml.Vector3i();

    int dropStartX = 0;
    int dropStartZ = 0;

    while (currentLength < length) {
      currentLength++;

      setBlock(x, y, z, getLeafBlock());
      mossify(x, y, z);

      if (currentLength >= length) {
        continue;
      }

      if (currentLength == snakeLength) {
        dropStartX = x;
        dropStartZ = z;
      }

      if (currentLength > snakeLength) {
        x = dropStartX;
        z = dropStartZ;

        if (!isLeafReplaceable(x, y - 1, z)
            || (random.nextBoolean() && currentLength < (length - 1))
        ) {
          int by = y - 1;

          if (!findNextValidMove(x, by, z, nextPosition)) {
            break;
          }

          x = nextPosition.x;
          z = nextPosition.y;
        }

        y--;
        continue;
      }

      if (!findCeilingSnakeNextMove(x, y, z, nextPos3i)) {
        break;
      }

      x = nextPos3i.x;
      y = nextPos3i.y;
      z = nextPos3i.z;
    }
  }

  private boolean findCeilingSnakeNextMove(int x, int y, int z, org.joml.Vector3i out) {
    IntArrays.shuffle(yMoves, random);

    for (int yo : yMoves) {
      IntArrays.shuffle(xMoves, random);

      for (int xo : xMoves) {
        IntArrays.shuffle(zMoves, random);

        for (int zo : zMoves) {
          int bx = xo + x;
          int by = yo + y;
          int bz = zo + z;

          if (!isLeafReplaceable(bx, by, bz)) {
            continue;
          }
          if (!isSupportedByAnyFace(bx, by, bz)) {
            continue;
          }

          if (xo != lastXMove && zo != lastZMove) {
            continue;
          }

          out.x = bx;
          out.y = by;
          out.z = bz;

          lastXMove = xo;
          lastZMove = zo;

          return true;
        }
      }
    }

    return false;
  }

  @Getter @Setter
  public static class LeafConfig implements NoiseSettingHolder {

    static final List<Material> DEFAULT_LEAVES = List.of(
        OAK_LEAVES,
        SPRUCE_LEAVES,
        BIRCH_LEAVES,
        DARK_OAK_LEAVES,
        AZALEA_LEAVES,
        MANGROVE_LEAVES
    );

    static final List<BlockFilterArgument.Result> DEFAULT_REPLACABLE = List.of(
        BlockFilters.create(GLOW_LICHEN),
        BlockFilters.create(SHORT_GRASS),
        BlockFilters.create(TALL_GRASS)
    );

    static final Codec<LeafConfig> CODEC = ExistingObjectCodec.createCodec(
        LeafConfig::new,
        builder -> {
          builder.optional("noise", NoiseParameter.CODEC)
              .setter(LeafConfig::setNoise)
              .getter(LeafConfig::getNoise);

          builder.optional("leaf-materials", ExtraCodecs.MATERIAL_CODEC.listOf(1, Integer.MAX_VALUE))
              .setter(LeafConfig::setLeafMaterials)
              .getter(LeafConfig::getLeafMaterials);

          builder.optional("can-replace", BlockFilters.CODEC.listOf())
              .setter(LeafConfig::setLeavesCanReplace)
              .getter(LeafConfig::getLeavesCanReplace);

          builder.optional("ceiling-leaves-allowed", Codec.BOOL)
              .setter(LeafConfig::setCeilingSnakeAllowed)
              .getter(LeafConfig::isCeilingSnakeAllowed);

          builder.optional("floor-leaves-allowed", Codec.BOOL)
              .setter(LeafConfig::setFloorSnakeAllowed)
              .getter(LeafConfig::isFloorSnakeAllowed);

          builder.optional("max-length", Codec.intRange(1, Integer.MAX_VALUE))
              .setter(LeafConfig::setMaxLength)
              .getter(LeafConfig::getMaxLength);
        }
    );

    private NoiseParameter noise = new NoiseParameter();
    private List<Material> leafMaterials = DEFAULT_LEAVES;
    private List<BlockFilterArgument.Result> leavesCanReplace = DEFAULT_REPLACABLE;

    private boolean ceilingSnakeAllowed = true;
    private boolean floorSnakeAllowed = true;

    private int maxLength = 4;

    @Override
    public NoiseParameter noiseParameters() {
      return noise;
    }
  }
}
