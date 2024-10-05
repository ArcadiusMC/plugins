package net.arcadiusmc.dungeons.gen;

import static org.bukkit.Material.BROWN_CANDLE;
import static org.bukkit.Material.CANDLE;
import static org.bukkit.Material.COBBLESTONE_SLAB;
import static org.bukkit.Material.GRAVEL;
import static org.bukkit.Material.GRAY_CANDLE;
import static org.bukkit.Material.GREEN_CANDLE;
import static org.bukkit.Material.LIGHT_GRAY_CANDLE;
import static org.bukkit.Material.ORANGE_CANDLE;
import static org.bukkit.Material.STONE_BRICK_WALL;
import static org.bukkit.Material.WHITE_CANDLE;

import com.google.common.base.Stopwatch;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dungeons.DungeonConfig;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.arcadiusmc.dungeons.gen.BlockIterations.BlockIteration;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.structure.BlockProcessors;
import net.arcadiusmc.structure.BlockRotProcessor;
import net.arcadiusmc.structure.BlockRotProcessor.IntegrityProvider;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.FunctionInfo;
import net.arcadiusmc.structure.StructurePlaceConfig;
import net.arcadiusmc.structure.buffer.BlockBuffer;
import net.arcadiusmc.structure.buffer.BlockBuffers;
import net.arcadiusmc.structure.buffer.BufferBlock;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Transform;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Chain;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Stairs.Shape;
import org.bukkit.block.data.type.Wall;
import org.bukkit.block.data.type.Wall.Height;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.PerlinNoiseGenerator;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3i;

@Getter @Setter
public class DungeonGenerator {

  private static final Logger LOGGER = Loggers.getLogger();

  private final DungeonConfig config;
  private final Random random;
  private final DungeonPiece rootPiece;

  private final BoundsSet boundsSet;
  private final BlockBuffer buffer;
  private final Map<String, List<GeneratorFunction>> functions = new Object2ObjectOpenHashMap<>();
  private final NoiseGenerator noiseGen;

  private Vector3i position = Vector3i.ZERO;

  public DungeonGenerator(DungeonPiece rootPiece, Random random, DungeonConfig config) {
    this.rootPiece = rootPiece;
    this.random = random;
    this.config = config;

    this.boundsSet = new BoundsSet();
    this.boundsSet.set(rootPiece);

    Bounds3i combined = this.boundsSet.combine();
    this.buffer = BlockBuffers.allocate(combined);

    noiseGen = new PerlinNoiseGenerator(random);
  }

  public static DungeonPiece generateLevel(DungeonConfig config, Random random) {
    List<LevelGenResult> levels = new ObjectArrayList<>();

    for (int i = 0; i < config.getPotentialLevels(); i++) {
      StructureGenerator gen = new StructureGenerator(config, random);
      gen.generate();

      LevelGenResult result = new LevelGenResult(gen.getRootPiece());
      result.getRootPiece().forEachDescendant(piece -> {
        var k = piece.getKind();

        int mdepth = Math.max(piece.getDepth(), result.getGreatestDepth());
        result.setGreatestDepth(mdepth);

        switch (k) {
          case MOB_ROOM:
          case BOSS_ROOM:
            result.setNonConnectorRooms(result.getNonConnectorRooms() + 1);
            break;

          default:
            break;
        }
      });

      levels.add(result);
    }

    levels.sort(LevelGenResult.COMPARATOR);

    // Drop invalid levels
    int preSize = levels.size();
    int minDepth = config.getParameters().getDepthRange().getMinimum();

    ObjectArrayList<LevelGenResult> filtered = levels.stream()
        .filter(r -> r.getGreatestDepth() >= minDepth)
        .collect(ObjectArrayList.toList());

    int afterSize = filtered.size();
    if (afterSize < preSize) {
      int dif = preSize - afterSize;
      float rate = Math.round((((float) (preSize - afterSize)) / preSize) * 10000.0f) / 100.0f;

      LOGGER.debug("Dropped {} levels ({}% drop rate)", dif, rate);
    }

    if (filtered.isEmpty()) {
      return levels.getFirst().getRootPiece();
    }

    return filtered.getFirst().getRootPiece();
  }

  public List<GeneratorFunction> getFunctions(String functionType) {
    List<GeneratorFunction> list = functions.get(functionType);

    if (list == null) {
      return ObjectLists.emptyList();
    }

    return list;
  }

  public BlockBuffer generateDungeon() {
    if (!position.equals(Vector3i.ZERO)) {
      rootPiece.transform(Transform.offset(position));
    }

    PlacingVisitor visitor = new PlacingVisitor();
    rootPiece.forEachDescendant(visitor);

    //runPass("vegetation", this::vegetationPass);
    runPass("puddles", this::puddlePass);
    runPass("hanging-lights", this::hangingLightsPass);
    runPass("sitting-lights", this::sittingLightPass);
    runPass("fire-pass", this::firePass);
    runPass("edge-rot", this::edgeRotPass);

    return buffer;
  }

  private void runPass(String name, Runnable runnable) {
    Stopwatch stopWatch = Stopwatch.createStarted();

    LOGGER.debug("Starting pass {}", name);

    try {
      runnable.run();
    } catch (Exception exc) {
      LOGGER.error("Failed to run {} pass", name, exc);
    } finally {
      stopWatch.stop();

      Duration elapsed = stopWatch.elapsed();

      long millis = elapsed.toMillis();
      float seconds = ((float) millis) / 1000f;

      LOGGER.info("Finished {} pass, took {}ms or {}sec", name, millis, seconds);
    }
  }

  /* --------------------------- Sitting light pass ---------------------------- */

  public void sittingLightPass() {
    CandlePass pass = new CandlePass();
    boundsSet.forEachBlock(pass);
  }

  /* --------------------------- Vegetation pass ---------------------------- */

  public void vegetationPass() {
    VegetationPass pass = new VegetationPass();
    boundsSet.forEachBlock(pass);
  }

  public void puddlePass() {
    PuddlePass puddlePass = new PuddlePass();
    boundsSet.forEachBlock(puddlePass);
  }

  /* --------------------------- Edge rot pass ---------------------------- */

  public void edgeRotPass() {
    EdgeRotPass func = new EdgeRotPass();
    boundsSet.forEachBlock(func);
  }

  /* --------------------------- Fire pass ---------------------------- */

  public void firePass() {
    List<GeneratorFunction> list = getFunctions(LevelFunctions.FIRE);

    for (GeneratorFunction generatorFunction : list) {
      float chance = generatorFunction.getData().getFloat("chance", 1f);

      if (chance < 1f) {
        float rnd = random.nextFloat();

        if (rnd >= chance) {
          continue;
        }
      }

      buffer.setBlock(generatorFunction.getPosition(), Material.FIRE.createBlockData(), null);
    }
  }

  /* --------------------------- Hanging lights pass ---------------------------- */

  public void hangingLightsPass() {
    List<GeneratorFunction> functionList = functions.get(LevelFunctions.HANGING_LIGHT);
    if (functionList == null || functionList.isEmpty()) {
      return;
    }

    for (GeneratorFunction func : functionList) {
      CompoundTag data = func.getData();

      float spawnChance = data.getFloat("spawn_chance", 1f);
      if (spawnChance < 1f) {
        float rnd = random.nextFloat();

        if (rnd >= spawnChance) {
          continue;
        }
      }

      generateHangingLight(func.getPosition());
    }
  }

  private void generateHangingLight(Vector3i originPoint) {
    int x = originPoint.x();
    int z = originPoint.z();

    int freeSpace = 0;
    int fy = originPoint.y();

    while (freeSpace < 20) {
      if (!isAir(x, fy, z)) {
        break;
      }

      freeSpace++;
      fy--;
    }

    int maxLen = config.getDecoration().getMaxHangingLightLength();
    int chainLen = freeSpace <= 0 ? 0 : Math.min(maxLen, random.nextInt(freeSpace / 4));

    int by = originPoint.y();

    if (isAir(x, by + 1, z)) {
      addHangingTop(x, by + 1, z);
    }

    BlockData chainData = createChainData();

    for (int i = 0; i < chainLen; i++) {
      buffer.setBlock(x, by, z, chainData);
      by--;
    }

    Lantern lantern = (Lantern) Material.LANTERN.createBlockData();
    lantern.setHanging(true);

    buffer.setBlock(x, by, z, lantern);
  }

  private boolean hasSupport(int x, int y, int z, BlockFace face) {
    BufferBlock block = buffer.getBlock(x, y, z);
    if (block == null) {
      return false;
    }

    BlockData data = block.data();
    return data.isFaceSturdy(face, BlockSupport.FULL);
  }

  private void addHangingTop(int x, int y, int z) {
    Wall wall = (Wall) STONE_BRICK_WALL.createBlockData();

    if (hasSupport(x, y, z + 1, BlockFace.NORTH)) {
      wall.setHeight(BlockFace.SOUTH, Height.LOW);
    }
    if (hasSupport(x, y, z - 1, BlockFace.SOUTH)) {
      wall.setHeight(BlockFace.NORTH, Height.LOW);
    }
    if (hasSupport(x + 1, y, z, BlockFace.WEST)) {
      wall.setHeight(BlockFace.EAST, Height.LOW);
    }
    if (hasSupport(x + 1, y, z, BlockFace.EAST)) {
      wall.setHeight(BlockFace.WEST, Height.LOW);
    }

    buffer.setBlock(x, y, z, wall);
  }

  private BlockData createChainData() {
    Chain data = (Chain) Material.CHAIN.createBlockData();
    data.setAxis(Axis.Y);
    return data;
  }

  /* ------------------------------------------------------- */

  private boolean isVoidBelow(int x, int y, int z) {
    return isVoidInDirection(x, y, z, -1);
  }

  private boolean isSkyAbove(int x, int y, int z) {
    return isVoidInDirection(x, y, z, 1);
  }

  private boolean isVoidInDirection(int x, int y, int z, int yDir) {
    y += yDir;

    int min = boundsSet.combine().minY();
    int max = boundsSet.combine().maxY();

    while (y >= min && y <= max) {
      if (!isAir(x, y, z)) {
        return false;
      }

      y += yDir;
    }

    return true;
  }

  private boolean isAir(int x, int y, int z) {
    BufferBlock block = buffer.getBlock(x, y, z);
    if (block == null) {
      return true;
    }

    return block.data().getMaterial().isAir();
  }

  private boolean isGroundBlock(int x, int y, int z) {
    if (!isAir(x, y + 1, z)) {
      return false;
    }

    BufferBlock block = buffer.getBlock(x, y, z);
    if (block == null) {
      return false;
    }

    return block.data().isFaceSturdy(BlockFace.UP, BlockSupport.FULL);
  }

  public void collectFunctions(Holder<BlockStructure> structure, StructurePlaceConfig cfg) {
    for (FunctionInfo function : structure.getValue().getFunctions()) {
      GeneratorFunction func = new GeneratorFunction(function.getFunctionKey());
      if (function.getTag() != null) {
        func.getData().putAll(function.getTag().copy());
      }

      Vector3i position = cfg.getTransform().apply(function.getOffset());
      Direction direction = function.getFacing();

      if (direction.isRotatable()) {
        direction = direction.rotate(cfg.getTransform().getRotation());
      }

      func.setPosition(position);
      func.setFacing(direction);

      List<GeneratorFunction> functionsList = functions.computeIfAbsent(
          function.getFunctionKey(),
          s -> new ObjectArrayList<>()
      );

      functionsList.add(func);
    }
  }

  // Assume specified coordinates are the air block above the floor
  Direction wallDirection(int x, int y, int z) {
    if (isWallBlock(x + 1, y, z)) {
      return Direction.EAST;
    }
    if (isWallBlock(x - 1, y, z)) {
      return Direction.WEST;
    }
    if (isWallBlock(x, y, z + 1)) {
      return Direction.SOUTH;
    }
    if (isWallBlock(x, y, z - 1)) {
      return Direction.NORTH;
    }

    return null;
  }

  Direction edgeDirection(int x, int y, int z) {
    if (isEdge(x + 1, y, z)) {
      return Direction.EAST;
    }
    if (isEdge(x - 1, y, z)) {
      return Direction.WEST;
    }
    if (isEdge(x, y, z + 1)) {
      return Direction.SOUTH;
    }
    if (isEdge(x, y, z - 1)) {
      return Direction.NORTH;
    }

    return null;
  }

  boolean isWallBlock(int x, int y, int z) {
    BufferBlock block = buffer.getBlock(x, y, z);
    if (block == null) {
      return false;
    }

    BlockData data = block.data();

    return data.isFaceSturdy(BlockFace.NORTH, BlockSupport.FULL)
        && data.isFaceSturdy(BlockFace.SOUTH, BlockSupport.FULL)
        && data.isFaceSturdy(BlockFace.WEST, BlockSupport.FULL)
        && data.isFaceSturdy(BlockFace.EAST, BlockSupport.FULL);
  }

  boolean isEdge(int x, int y, int z) {
    return isAir(x, y, z) && isAir(x, y - 1, z);
  }

  class PlacingVisitor implements Consumer<DungeonPiece>, IntegrityProvider {

    @Override
    public void accept(DungeonPiece piece) {
      Holder<BlockStructure> structure = piece.getStructure();
      if (structure == null) {
        return;
      }

      StructurePlaceConfig cfg = StructurePlaceConfig.builder()
          .buffer(buffer)
          .pos(piece.getPivotPoint())
          .paletteName(piece.getPaletteName())
          .transform(Transform.rotation(piece.getRotation()))
          .addNonNullProcessor()
          .addRotationProcessor()
          .addProcessor(BlockProcessors.IGNORE_AIR)
          .addProcessor(new BlockRotProcessor(this, random))
          .addProcessor(new StairRuinProcessor(noiseGen, random))
          .addFunction(
              LevelFunctions.POOL,
              new PoolFunctionProcessor(buffer, random, DungeonGenerator.this)
          )
          .build();

      structure.getValue().place(cfg);
      collectFunctions(structure, cfg);
    }

    @Override
    public double getIntegrity(Vector3i worldPosition) {
      float freq = config.getDecoration().getBlockRotFrequency();

      float x = worldPosition.x() * freq;
      float y = worldPosition.y() * freq;
      float z = worldPosition.z() * freq;

      double n = noiseGen.noise(x, y, z);
      return (n + 1.0d) / 2.0d;
    }
  }

  class EdgeRotPass implements XyzFunction {

    Direction[] arr2 = new Direction[2];
    Direction[] arr3 = new Direction[3];

    static BlockIteration getMaterial(Material base) {
      return BlockIterations.getIteration(base);
    }

    @Override
    public void accept(int x, int y, int z) {
      if (!isGroundBlock(x, y, z) || isAir(x, y - 1, z) || isSkyAbove(x, y, z)) {
        return;
      }

      int ey = y + 1;
      Direction edgeDir = edgeDirection(x, ey, z);

      if (edgeDir == null) {
        return;
      }

      float freq = config.getDecoration().getEdgeRotFrequency();
      double noise = (noiseGen.noise(x * freq, y * freq, z * freq) + 1.0d) / 2.0d;
      float rnd = random.nextFloat();

      if (rnd >= noise) {
        return;
      }

      BufferBlock block = buffer.getBlock(x, y, z);
      if (block == null) {
        return;
      }

      BlockIteration mat = getMaterial(block.data().getMaterial());
      int size = random.nextBoolean() ? 1 : 2;

      if (mat == null) {
        return;
      }

      Direction[] potentialDirections;

      if (random.nextBoolean()) {
        placeStair(x, y, z, mat, edgeDir.opposite());

        potentialDirections = arr2;
        arr2[0] = edgeDir.left();
        arr2[1] = edgeDir.right();
      } else {
        buffer.setBlock(x, y, z, mat.slab().createBlockData());

        potentialDirections = arr3;
        arr3[0] = edgeDir.left();
        arr3[1] = edgeDir.right();
        arr3[2] = edgeDir.opposite();
      }

      if (size == 1) {
        return;
      }

      if (random.nextFloat() < 0.1f) {
        BlockData data = COBBLESTONE_SLAB.createBlockData();
        int slabX = x - edgeDir.getMod().x();
        int slabZ = z - edgeDir.getMod().z();

        if (random.nextFloat() < 0.25) {
          if (random.nextBoolean()) {
            slabX = 0;
          } else  {
            slabZ = 0;
          }
        }

        if (isGroundBlock(slabX, y, slabZ)) {
          buffer.setBlock(slabX, y + 1, slabZ, data);
        }
      }

      for (int i = 0; i < potentialDirections.length; i++) {
        Direction dir = potentialDirections[i];
        int nx = dir.getMod().x() + x;
        int nz = dir.getMod().z() + z;

        if (!isGroundBlock(nx, y, nz)) {
          continue;
        }

        BufferBlock b = buffer.getBlock(x, y, z);
        BlockIteration dirMat;

        if (b == null) {
          dirMat = mat;
        } else {
          dirMat = getMaterial(b.data().getMaterial());
          if (dirMat == null) {
            dirMat = mat;
          }
        }

        if (i == 2 || random.nextBoolean()) {
          if (random.nextBoolean()) {
            dir = edgeDir.opposite();
          } else if (i < 2 && potentialDirections.length == 2) {
            if (i == 0) {
              placeStair(nx, y, nz, dirMat, dir, Shape.INNER_LEFT);
            } else {
              placeStair(nx, y, nz, dirMat, dir, Shape.INNER_RIGHT);
            }

            continue;
          }

          placeStair(nx, y, nz, dirMat, dir);
        } else {
          buffer.setBlock(nx, y, nz, dirMat.slab().createBlockData());
        }
      }
    }

    void placeStair(int x, int y, int z, BlockIteration mat, Direction direction) {
      placeStair(x, y, z, mat, direction, Shape.STRAIGHT);
    }

    void placeStair(int x, int y, int z, BlockIteration mat, Direction direction, Shape shape) {
      Stairs stair = (Stairs) mat.stairs().createBlockData();
      stair.setFacing(direction.asBlockFace());
      stair.setWaterlogged(false);
      stair.setShape(shape);

      BufferBlock block = new BufferBlock(stair, null);
      buffer.setBlock(x, y, z, block);
    }
  }

  abstract class NoisePass implements XyzFunction {

    private final NoiseGenerator noiseGen;

    public NoisePass() {
      this.noiseGen = new PerlinNoiseGenerator(random);
    }

    float frequency() {
      return 0.08f;
    }

    float gate() {
      return 0.65f;
    }

    double getNoise(int x, int y, int z) {
      float freq = frequency();
      return (noiseGen.noise(x * freq, y * freq, z * freq) + 1.0) / 2.0;
    }

    boolean testNoise(int x, int y, int z) {
      return getNoise(x, y, z) >= gate();
    }
  }

  class CandlePass extends NoisePass {

    static final Material[] CANDLES = {
        CANDLE,
        WHITE_CANDLE,
        ORANGE_CANDLE,
        GRAY_CANDLE,
        LIGHT_GRAY_CANDLE,
        BROWN_CANDLE,
        GREEN_CANDLE,
    };

    @Override
    float frequency() {
      return config.getDecoration().getCandleFrequency();
    }

    @Override
    float gate() {
      return config.getDecoration().getCandleNoiseGate();
    }

    @Override
    public void accept(int x, int y, int z) {
      if (isSkyAbove(x, y, z) || !isGroundBlock(x, y - 1, z)) {
        return;
      }
      if (!canSupportCandle(x, y - 1, z)) {
        return;
      }

      if (config.getDecoration().isBindCandleToEdge()) {
        Direction edge = edgeDirection(x, y, z);
        Direction wallEdge = wallDirection(x, y, z);

        if (edge == null && wallEdge == null) {
          return;
        }
      }

      if (!testNoise(x, y, z)) {
        return;
      }

      Material candleMaterial = CANDLES[random.nextInt(CANDLES.length)];
      Candle data = (Candle) candleMaterial.createBlockData();

      int candles = random.nextInt(data.getMinimumCandles(), data.getMaximumCandles() + 1);

      data.setLit(random.nextFloat() < 0.75);
      data.setCandles(candles);

      buffer.setBlock(x, y, z, data);
    }

    boolean canSupportCandle(int x, int y, int z) {
      BufferBlock block = buffer.getBlock(x, y, z);
      if (block == null) {
        return false;
      }

      Material material = block.data().getMaterial();
      return material != GRAVEL;
    }
  }

  class PuddlePass extends NoisePass {

    @Override
    float gate() {
      return config.getDecoration().getPuddleNoiseGate();
    }

    @Override
    float frequency() {
      return config.getDecoration().getPuddleFrequency();
    }

    @Override
    public void accept(int x, int y, int z) {
      if (!isPuddleBlock(x, y, z) || isSkyAbove(x, y, z)) {
        return;
      }

      BufferBlock block = buffer.getBlock(x, y, z);
      if (block == null) {
        return;
      }

      Material mat = block.data().getMaterial();
      BlockIteration iteration = BlockIterations.getIteration(mat);
      if (iteration == null) {
        return;
      }

      if (iteration.block() != mat) {
        return;
      }

      double noise = getNoise(x, y, z);

      buffer.setBlock(x, y, z, iteration.slab().createBlockData());
    }

    boolean isPuddleBlock(int x, int y, int z) {
      if (!isGroundBlock(x, y, z)) {
        return false;
      }

//      if (isSkyAbove(x, y, z)) {
//        return false;
//      }

      BufferBlock block = buffer.getBlock(x, y, z);
      if (block != null) {
        BlockIteration iter = BlockIterations.getIteration(block.data().getMaterial());

        if (iter == null) {
          return false;
        }
      }

      return testNoise(x, y, z);
    }
  }

  class VegetationPass extends NoisePass {

    final int[] moves = {-1, 0, 1};

    @Override
    float frequency() {
      return 1f;
    }

    @Override
    public void accept(int x, int y, int z) {
      if (isSkyAbove(x, y, z) || isVoidBelow(x, y, z) || !isAir(x, y, z)) {
        return;
      }
      if (isAir(x, y + 1, z) || isAir(x, y - 1, z)) {
        return;
      }
      if (!testNoise(x, y, z)) {
        return;
      }

      int maxLength = config.getDecoration().getMaxLeafLength();
      int length = random.nextInt(maxLength);


    }
  }
}
