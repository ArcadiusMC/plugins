package net.arcadiusmc.dungeons.gen;

import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_ALL;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_NE;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_NONE;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_NW;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_SE;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.Q_SW;
import static net.arcadiusmc.dungeons.gen.StairQuadrants.createStairs;
import static org.bukkit.Material.ANDESITE;
import static org.bukkit.Material.ANDESITE_STAIRS;
import static org.bukkit.Material.BROWN_CANDLE;
import static org.bukkit.Material.CANDLE;
import static org.bukkit.Material.CHAIN;
import static org.bukkit.Material.COBBLESTONE;
import static org.bukkit.Material.COBBLESTONE_SLAB;
import static org.bukkit.Material.COBBLESTONE_STAIRS;
import static org.bukkit.Material.DEEPSLATE;
import static org.bukkit.Material.DIRT;
import static org.bukkit.Material.FIRE;
import static org.bukkit.Material.GLOW_LICHEN;
import static org.bukkit.Material.GRASS_BLOCK;
import static org.bukkit.Material.GRAVEL;
import static org.bukkit.Material.GRAY_CANDLE;
import static org.bukkit.Material.GREEN_CANDLE;
import static org.bukkit.Material.LANTERN;
import static org.bukkit.Material.LIGHT_GRAY_CANDLE;
import static org.bukkit.Material.MOSS_BLOCK;
import static org.bukkit.Material.MOSS_CARPET;
import static org.bukkit.Material.ORANGE_CANDLE;
import static org.bukkit.Material.STONE;
import static org.bukkit.Material.STONE_BRICKS;
import static org.bukkit.Material.STONE_BRICK_WALL;
import static org.bukkit.Material.WHITE_CANDLE;

import com.google.common.base.Stopwatch;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.dungeons.DungeonConfig;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.arcadiusmc.dungeons.NoiseParameter;
import net.arcadiusmc.dungeons.commands.CommandDungeonGen;
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
import net.arcadiusmc.utils.VanillaAccess;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Transform;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.Axis;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.CaveVinesPlant;
import org.bukkit.block.data.type.Chain;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Stairs.Shape;
import org.bukkit.block.data.type.Wall;
import org.bukkit.block.data.type.Wall.Height;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.PerlinNoiseGenerator;
import org.joml.Vector2i;
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
  private final NoiseGenerator vegitationNoise;

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
    vegitationNoise = new PerlinNoiseGenerator(random);
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
          case BOSS_ROOM:
            result.setBossRoom(piece);
          case MOB_ROOM:
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

    Stopwatch genTimer = Stopwatch.createStarted();

    try {
      performGeneration();
    } finally {
      genTimer.stop();
      LOGGER.info("Generated dungeon, took {}", stopwatchElapsed(genTimer));
    }

    return buffer;
  }

  private void performGeneration() {
    PlacingVisitor visitor = new PlacingVisitor();
    rootPiece.forEachDescendant(visitor);

    runPass("cave-vines",     this::vinePass);
    runPass("leaves",         this::leafPass);
    runPass("moss-and-grass", this::mossGrassPass);
    runPass("foliage",        this::foliagePass);
    runPass("puddles",        this::puddlePass);
    runPass("hanging-lights", this::hangingLightsPass);
    runPass("candles",        this::candlesPass);
    runPass("fire-pass",      this::firePass);
    runPass("edge-rot",       this::edgeRotPass);
  }

  private void runPass(String name, Runnable runnable) {
    if (config.getDecoration().getDisabledDecorators().contains(name)) {
      LOGGER.info("Decorator {} is disabled. Skipping...", name);
      return;
    }

    Stopwatch stopWatch = Stopwatch.createStarted();

    try {
      runnable.run();
    } catch (Exception exc) {
      LOGGER.error("Failed to run {} pass", name, exc);
    } finally {
      stopWatch.stop();
      LOGGER.info("Finished {} pass, took {}", name, stopwatchElapsed(stopWatch));
    }
  }

  static String stopwatchElapsed(Stopwatch stopwatch) {
    Duration elapsed = stopwatch.elapsed();
    long millis = elapsed.toMillis();
    float seconds = ((float) millis) / 1000f;
    return String.format("%sms or %ssec", millis, seconds);
  }

  /* --------------------------- Sitting light pass ---------------------------- */

  public void candlesPass() {
    boundsSet.forEachBlock(new CandlePass());
  }

  /* --------------------------- Vegetation pass ---------------------------- */

  public void vinePass() {
    boundsSet.forEachBlock(new CaveVinePass(vegitationNoise));
  }

  public void leafPass() {
    boundsSet.forEachBlock(new LeafPass(vegitationNoise));
  }

  public void mossGrassPass() {
    boundsSet.forEachBlock(new MossGrassPass(vegitationNoise));
  }

  public void foliagePass() {
    boundsSet.forEachBlock(new FoliagePass());
  }

  /* --------------------------- Indent pass ---------------------------- */

  public void puddlePass() {
    boundsSet.forEachBlock(new PuddlePass());
  }

  /* --------------------------- Edge rot pass ---------------------------- */

  public void edgeRotPass() {
    boundsSet.forEachBlock(new EdgeRotPass());
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

      var pos = generatorFunction.getPosition();
      setBlock(pos.x(), pos.y(), pos.z(), FIRE.createBlockData());
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
    int y = originPoint.y();
    int z = originPoint.z();

    int freeSpace = freeSpaceDown(x, y, z);

    int maxLen = config.getDecoration().getMaxHangingLightLength();
    int chainLen = freeSpace <= 0 ? 0 : Math.min(maxLen, random.nextInt(freeSpace / 4));

    int by = y;

    if (isAir(x, by + 1, z)) {
      addHangingTop(x, by + 1, z);
    }

    BlockData chainData = createChainData();

    for (int i = 0; i < chainLen; i++) {
      setBlock(x, by, z, chainData);
      by--;
    }

    Lantern lantern = (Lantern) LANTERN.createBlockData();
    lantern.setHanging(true);

    setBlock(x, by, z, lantern);
  }

  private int freeSpaceDown(int x, int y, int z) {
    int freeSpace = 0;
    int fy = y;

    while (freeSpace < 20) {
      if (!isAir(x, fy, z)) {
        break;
      }

      freeSpace++;
      fy--;
    }

    return freeSpace;
  }

  private boolean hasSupport(int x, int y, int z, BlockFace face) {
    BufferBlock block = getBlock(x, y, z);
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

    setBlock(x, y, z, wall);
  }

  private BlockData createChainData() {
    Chain data = (Chain) CHAIN.createBlockData();
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
    BufferBlock block = getBlock(x, y, z);
    if (block == null) {
      return true;
    }

    return block.data().getMaterial().isAir();
  }

  private boolean isGroundBlock(int x, int y, int z) {
    if (!isAir(x, y + 1, z)) {
      return false;
    }

    BufferBlock block = getBlock(x, y, z);
    if (block == null) {
      return false;
    }

    return block.data().isFaceSturdy(BlockFace.UP, BlockSupport.FULL);
  }

  public BufferBlock getBlock(int x, int y, int z) {
    return buffer.getBlock(x, y, z);
  }

  public BlockData getBlockData(int x, int y, int z) {
    var b = getBlock(x, y, z);
    if (b == null) {
      return null;
    }
    return b.data();
  }

  public Material getBlockType(int x, int y, int z) {
    var b = getBlock(x, y, z);
    if (b == null) {
      return null;
    }
    return b.data().getMaterial();
  }

  public boolean isTagged(int x, int y, int z, Tag<Material> tag) {
    Material type = getBlockType(x, y, z);

    if (type == null) {
      return tag == Tag.AIR;
    }

    return tag.isTagged(type);
  }

  public boolean matchesBlock(int x, int y, int z, Material... anyOf) {
    Material mat = getBlockType(x, y, z);

    if (mat == null) {
      for (Material material1 : anyOf) {
        if (material1.isAir()) {
          return true;
        }
      }

      return false;
    }

    for (Material material1 : anyOf) {
      if (material1 == mat) {
        return true;
      }
    }

    return false;
  }

  public void setBlock(int x, int y, int z, BlockData data) {
    buffer.setBlock(x, y, z, data);
  }

  public void setBlock(int x, int y, int z, BufferBlock block) {
    buffer.setBlock(x, y, z, block);
  }

  public void clearBlock(int x, int y, int z) {
    buffer.clearBlock(x, y, z);
  }

  public void mossify(int x, int y, int z) {
    final int radius = config.getDecoration().getMossRadius();

    if (radius < 1) {
      return;
    }

    final int dropOffAfter = config.getDecoration().getMossDropOffAfter();
    final int maxMhDist = radius * 3;

    for (int ox = -radius; ox <= radius; ox++) {
      for (int oy = -radius; oy <= radius; oy++) {
        for (int oz = -radius; oz <= radius; oz++) {
          int bx = x + ox;
          int by = y + oy;
          int bz = z + oz;

          if (isAir(bx, by, bz)) {
            if (isSupportedByAnyFace(bx, by, bz)) {
              mossifyAir(bx, by, bz, ox, oy, oz, maxMhDist, dropOffAfter);
            }

            continue;
          }

          mossifyBlock(bx, by, bz, ox, oy, oz, maxMhDist, dropOffAfter);
        }
      }
    }
  }

  private boolean isSupportedByAnyFace(int x, int y, int z) {
    return hasSupport(x, y + 1, z, BlockFace.DOWN)
        || hasSupport(x, y - 1, z, BlockFace.UP)
        || hasSupport(x + 1, y, z, BlockFace.WEST)
        || hasSupport(x - 1, y, z, BlockFace.EAST)
        || hasSupport(x, y, z + 1, BlockFace.NORTH)
        || hasSupport(x, y, z - 1, BlockFace.SOUTH);
  }

  private boolean testMossDistance(int ox, int oy, int oz, int maxMhDist, int dropOffAfter) {
    // Blocks directly next to ox=0, oy=0, oz=0 should always have moss,
    // but other blocks should randomly drop off their mossy-ness
    // mhDist is the manhattan distance from ox=0, oy=0, oz=0
    int mhDist = Math.abs(ox) + Math.abs(oy) + Math.abs(oz);

    if (mhDist <= dropOffAfter) {
      return true;
    }

    float spawnRate = (float) mhDist / ((float) maxMhDist);
    float rand = random.nextFloat();

    return rand <= spawnRate;
  }

  private void mossifyAir(
      int x, int y, int z,
      int ox, int oy, int oz,
      int maxMhDist,
      int dropOffAfter
  ) {
    if (!testMossDistance(ox, oy, oz, maxMhDist, dropOffAfter)) {
      return;
    }

    float rate = config.getDecoration().getGlowLichenInsteadLeavesRate();
    if (random.nextFloat() >= rate) {
      return;
    }

    placeGlowLichen(x, y, z);
  }

  private void mossifyBlock(
      int x, int y, int z,
      int ox, int oy, int oz,
      int maxMhDist,
      int dropOffAfter
  ) {
    BlockData data = getBlockData(x, y, z);
    if (data == null) {
      return;
    }

    Material material = data.getMaterial();
    BlockIteration iteration = BlockIterations.getIteration(material);

    if (iteration == null) {
      return;
    }

    Material mossy;
    if (material == iteration.getBlock()) {
      mossy = iteration.getMossyBlock();
    } else if (material == iteration.getSlab()) {
      mossy = iteration.getMossySlab();
    } else if (material == iteration.getStairs()) {
      mossy = iteration.getMossyStairs();
    } else {
      mossy = iteration.getMossyWall();
    }

    if (mossy == null) {
      return;
    }
    if (!testMossDistance(ox, oy, oz, maxMhDist, dropOffAfter)) {
      return;
    }

    BlockData mossyData = mossy.createBlockData();
    mossyData = VanillaAccess.merge(mossyData, data);

    setBlock(x, y, z, mossyData);
  }

  public void placeGlowLichen(int x, int y, int z) {
    MultipleFacing facing = (MultipleFacing) GLOW_LICHEN.createBlockData();
    Set<BlockFace> allowed = facing.getAllowedFaces();

    if (allowed.contains(BlockFace.UP)) {
      facing.setFace(BlockFace.UP, hasSupport(x, y + 1, z, BlockFace.DOWN));
    }
    if (allowed.contains(BlockFace.DOWN)) {
      facing.setFace(BlockFace.DOWN, hasSupport(x, y - 1, z, BlockFace.UP));
    }
    if (allowed.contains(BlockFace.WEST)) {
      facing.setFace(BlockFace.WEST, hasSupport(x - 1, y, z, BlockFace.EAST));
    }
    if (allowed.contains(BlockFace.EAST)) {
      facing.setFace(BlockFace.EAST, hasSupport(x + 1, y, z, BlockFace.WEST));
    }
    if (allowed.contains(BlockFace.NORTH)) {
      facing.setFace(BlockFace.NORTH, hasSupport(x, y, z - 1, BlockFace.SOUTH));
    }
    if (allowed.contains(BlockFace.SOUTH)) {
      facing.setFace(BlockFace.SOUTH, hasSupport(x, y, z + 1, BlockFace.NORTH));
    }

    setBlock(x, y, z, facing);
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
    BlockData data = getBlockData(x, y, z);

    if (data == null) {
      return false;
    }

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
      NoiseParameter parameter = config.getDecoration().getBlockRot();
      return parameter.sample(noiseGen, worldPosition.x(), worldPosition.y(), worldPosition.z());
    }
  }

  class EdgeRotPass extends NoisePass {

    Direction[] arr2 = new Direction[2];
    Direction[] arr3 = new Direction[3];

    static BlockIteration getMaterial(Material base) {
      return BlockIterations.getIteration(base);
    }

    @Override
    NoiseParameter noiseParameters() {
      return config.getDecoration().getEdgeRot();
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

      double noise = getNoise(x, y, z);
      float rnd = random.nextFloat();

      if (rnd >= noise) {
        return;
      }

      Material type = getBlockType(x, y, z);
      if (type == null) {
        return;
      }

      BlockIteration mat = getMaterial(type);
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
        setBlock(x, y, z, mat.getSlab().createBlockData());

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
          setBlock(slabX, y + 1, slabZ, data);
        }
      }

      for (int i = 0; i < potentialDirections.length; i++) {
        Direction dir = potentialDirections[i];
        int nx = dir.getMod().x() + x;
        int nz = dir.getMod().z() + z;

        if (!isGroundBlock(nx, y, nz)) {
          continue;
        }

        Material b = getBlockType(x, y, z);
        BlockIteration dirMat;

        if (b == null) {
          dirMat = mat;
        } else {
          dirMat = getMaterial(b);
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
          setBlock(nx, y, nz, dirMat.getSlab().createBlockData());
        }
      }
    }

    void placeStair(int x, int y, int z, BlockIteration mat, Direction direction) {
      placeStair(x, y, z, mat, direction, Shape.STRAIGHT);
    }

    void placeStair(int x, int y, int z, BlockIteration mat, Direction direction, Shape shape) {
      Stairs stair = (Stairs) mat.getStairs().createBlockData();
      stair.setFacing(direction.asBlockFace());
      stair.setWaterlogged(false);
      stair.setShape(shape);

      BufferBlock block = new BufferBlock(stair, null);
      setBlock(x, y, z, block);
    }
  }

  abstract class NoisePass implements XyzFunction {
    private static final NoiseParameter DEFAULT = new NoiseParameter();

    private final NoiseGenerator noiseGen;

    public NoisePass(NoiseGenerator noiseGen) {
      this.noiseGen = noiseGen;
    }

    public NoisePass() {
      this(new PerlinNoiseGenerator(random));
    }

    NoiseParameter noiseParameters() {
      return DEFAULT;
    }

    double getNoise(double x, double y, double z) {
      NoiseParameter param = noiseParameters();
      return param.sample(noiseGen, x, y, z);
    }

    boolean testNoise(double noise) {
      return noiseParameters().getNoiseGate() <= noise;
    }

    boolean testNoise(double x, double y, double z) {
      return testNoise(getNoise(x, y, z));
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
    NoiseParameter noiseParameters() {
      return config.getDecoration().getCandles();
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

      data.setLit(random.nextFloat() < config.getDecoration().getLitCandleRate());
      data.setCandles(candles);

      setBlock(x, y, z, data);
    }

    boolean canSupportCandle(int x, int y, int z) {
      Material material = getBlockType(x, y, z);
      return material != GRAVEL;
    }
  }

  class PuddlePass extends NoisePass {

    public PuddlePass() {
      super();
      CommandDungeonGen.perlin = noiseGen;
    }

    @Override
    NoiseParameter noiseParameters() {
      return config.getDecoration().getPuddles();
    }

    @Override
    public void accept(int x, int y, int z) {
      if (!isPuddleBlock(x, y, z) || isSkyAbove(x, y, z)) {
        return;
      }

      Material mat = getBlockType(x, y, z);
      if (mat == null) {
        return;
      }

      BlockIteration iteration = BlockIterations.getIteration(mat);
      if (iteration == null) {
        return;
      }

      if (iteration.getBlock() != mat) {
        return;
      }

      int mask = sampleMask(x, y, z);

      if (mask == Q_NONE) {
        return;
      }
      if (mask == Q_ALL) {
        setPuddleBlock(x, y, z, iteration.getSlab().createBlockData());
        return;
      }

      BlockData data = createStairs(iteration.getStairs(), mask);
      if (data == null) {
        data = iteration.getSlab().createBlockData();
      }

      setPuddleBlock(x, y, z, data);
    }

    void setPuddleBlock(int x, int y, int z, BlockData data) {
      boolean waterlogged = false;

      if (data instanceof Waterlogged logged) {
        waterlogged = config.getDecoration().isWaterlogPuddles();
        logged.setWaterlogged(waterlogged);
      }

      setBlock(x, y, z, data);

      if (waterlogged) {
        mossify(x, y, z);
      }
    }

    int sampleMask(int x, int y, int z) {
      double minX = x + 0.25;
      double minZ = z + 0.25;
      double maxX = minX + 0.5;
      double maxZ = minZ + 0.5;
      double ny = y + 0.75d;

      int mask = blockQuadrantsMask(
          getNoise(minX, ny, minZ),
          getNoise(maxX, ny, minZ),
          getNoise(minX, ny, maxZ),
          getNoise(maxX, ny, maxZ)
      );

      boolean westAir = isAir(x - 1, y, z /*, BlockFace.WEST*/);
      boolean eastAir = isAir(x + 1, y, z /*, BlockFace.EAST*/);
      boolean northAir = isAir(x, y, z - 1 /*, BlockFace.NORTH*/);
      boolean southAir = isAir(x, y, z + 1 /*, BlockFace.SOUTH*/);

      if (westAir | northAir) {
        mask &= ~Q_NW;
      }
      if (westAir | southAir) {
        mask &= ~Q_SW;
      }
      if (eastAir | northAir) {
        mask &= ~Q_NE;
      }
      if (eastAir | southAir) {
        mask &= ~Q_SE;
      }

      return mask;
    }

    int blockQuadrantsMask(double nw, double ne, double sw, double se) {
      return getMask(Q_NW, nw)
          | getMask(Q_NE, ne)
          | getMask(Q_SW, sw)
          | getMask(Q_SE, se);
    }

    int getMask(int mask, double noise) {
      if (!testNoise(noise)) {
        return 0;
      }
      return mask;
    }

    boolean isPuddleBlock(int x, int y, int z) {
      if (!isGroundBlock(x, y, z)) {
        return false;
      }

      if (isSkyAbove(x, y, z)) {
        return false;
      }

      BufferBlock block = getBlock(x, y, z);
      if (block != null) {
        BlockIteration iter = BlockIterations.getIteration(block.data().getMaterial());

        if (iter == null) {
          return false;
        }
      }

      return true;
    }
  }

  class CaveVinePass extends NoisePass {

    public CaveVinePass(NoiseGenerator noiseGen) {
      super(noiseGen);
    }

    @Override
    public void accept(int x, int y, int z) {
      if (isSkyAbove(x, y, z) || isVoidBelow(x, y, z) || !isAir(x, y, z)) {
        return;
      }
      if (!testNoise(x, y, z)) {
        return;
      }
      if (!hasSupport(x, y + 1, z, BlockFace.DOWN)) {
        return;
      }

      caveVineTrail(x, y, z);
    }

    private void caveVineTrail(int x, int y, int z) {
      int freeSpace = freeSpaceDown(x, y, z) / 3;
      int maxLength = config.getDecoration().getMaxLeafLength();

      if (freeSpace < 1) {
        return;
      }

      int length = Math.min(maxLength, random.nextInt(freeSpace));

      for (int i = 0; i < length; i++) {
        if (!isAir(x, y, z)) {
          break;
        }

        BlockData data;

        if (!isAir(x, y - 1, z) || i == (length - 1)) {
          data = config.getDecoration().getCaveVineBottom().createBlockData();

          if (data instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
          }
        } else {
          data = config.getDecoration().getCaveVineBlock().createBlockData();
        }

        if (data instanceof CaveVinesPlant plant) {
          plant.setBerries(random.nextFloat() < config.getDecoration().getBerryRate());
        }

        setBlock(x, y, z, data);
        mossify(x, y, z);

        y--;
      }
    }
  }

  //
  //
  // Leaf gen types:
  //
  //  1. Dangling leaf - Falls straight down, simplest type of leaf to generate.
  //     Move down until resistance is met. These will be better as 'cave_vine'
  //     trails. Ideally with moss underneath or at the root.
  //
  //  2. Ceiling Snake - Snake along the ceiling for as long as possible before
  //     going downwards.
  //
  //  3. Ground Snake - Generates on the ground, snake around on the ground
  //     until ideal length reached.
  //
  //
  //
  //
  //
  //
  //
  class LeafPass extends NoisePass {

    final int[] xMoves = {-1, 0, 1};
    final int[] yMoves = {-1, 0, 1};
    final int[] zMoves = {-1, 0, 1};

    int lastXMove = 0;
    int lastZMove = 0;

    public LeafPass(NoiseGenerator noiseGen) {
      super(noiseGen);
    }

    @Override
    NoiseParameter noiseParameters() {
      return config.getDecoration().getLeaves();
    }

    private boolean hasLeafBlocks() {
      return !config.getDecoration().getLeafMaterials().isEmpty();
    }

    private BlockData getLeafBlock() {
      List<Material> list = config.getDecoration().getLeafMaterials();

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

      if (hasSupport(x, y - 1, z, BlockFace.UP)) {
        groundSnakeLeaves(x, y, z);
        return;
      }

      ceilingSnakeLeaves(x, y, z);
    }

    private boolean isLeafReplaceable(int x, int y, int z) {
      if (isAir(x, y, z)) {
        return true;
      }

      Material type = getBlockType(x, y, z);
      return config.getDecoration().getLeavesCanReplace().contains(type);
    }

    private void groundSnakeLeaves(int x, int y, int z) {
      int maxLength = random.nextInt(config.getDecoration().getMaxLeafLength());
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
      int upperBound = config.getDecoration().getMaxLeafLength();
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
  }

  class MossGrassPass extends NoisePass {

    static final Material[] CAN_REPLACE = {
        STONE,
        ANDESITE,
        COBBLESTONE
    };

    static final Material[] MAYBE_REPLACE = {
        STONE_BRICKS,
        DEEPSLATE,
        ANDESITE_STAIRS,
        COBBLESTONE_STAIRS,
    };

    public MossGrassPass(NoiseGenerator noiseGen) {
      super(noiseGen);
    }

    @Override
    NoiseParameter noiseParameters() {
      return config.getDecoration().getMoss();
    }

    @Override
    public void accept(int x, int y, int z) {
      if (!testNoise(x, y, z)) {
        return;
      }

      if (isAir(x, y, z)) {
        boolean supportUp = hasSupport(x, y - 1, z, BlockFace.UP);
        int by = y - 1;

//        if (matchesBlock(x, by, z, MOSS_BLOCK) || (supportUp && testNoise(x, by, z))) {
//          int below = countMossBelow(x, y, z);
//
//          if (below < 4) {
//            setBlock(x, y, z, (random.nextBoolean() ? MOSS_BLOCK : GRASS_BLOCK).createBlockData());
//            mossify(x, y, z, 1);
//
//            if (matchesBlock(x, by, z, GRASS_BLOCK)) {
//              setBlock(x, by, z, DIRT.createBlockData());
//            }
//          }
//
//          return;
//        }

        if (supportUp && random.nextBoolean()) {
          setBlock(x, y, z, MOSS_CARPET.createBlockData());
          mossify(x, y, z);
        }

        return;
      }

      if (isTagged(x, y, z, Tag.LEAVES)
          && canOverrideLeavesMoss(x, y, z)
          && random.nextFloat() < 0.25
      ) {
        setBlock(x, y, z, MOSS_BLOCK.createBlockData());
        return;
      }

      if (!canReplace(x, y, z)) {
        return;
      }

      setBlock(x, y, z, MOSS_BLOCK.createBlockData());
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

    int countMossBelow(int x, int y, int z) {
      y--;
      int c = 0;

      while (y > boundsSet.combine().minY()) {
        if (!matchesBlock(x, y, z, MOSS_BLOCK, GRASS_BLOCK, DIRT)) {
          break;
        }

        c++;
        y--;
      }

      return c;
    }

    boolean canReplace(int x, int y, int z) {
      Material mat = getBlockType(x, y, z);
      if (mat == null) {
        return false;
      }

      for (Material material : CAN_REPLACE) {
        if (material == mat) {
          return true;
        }
      }

      for (Material material : MAYBE_REPLACE) {
        if (material != mat) {
          continue;
        }

        if (random.nextBoolean()) {
          return true;
        }
      }

      return false;
    }
  }

  class FoliagePass implements XyzFunction {

    @Override
    public void accept(int x, int y, int z) {
      if (!isAir(x, y, z)) {
        return;
      }

      int by = y - 1;
      if (!matchesBlock(x, by, z, MOSS_BLOCK, DIRT, GRASS_BLOCK)) {
        return;
      }

      float rate = config.getDecoration().getFoliageRate();
      if (random.nextFloat() >= rate) {
        return;
      }

      List<Material> foliageList;

      if (random.nextFloat() < config.getDecoration().getFoliageUsesLeaves()) {
        foliageList = config.getDecoration().getLeafMaterials();
        if (foliageList.isEmpty()) {
          foliageList = config.getDecoration().getFlora();
        }
      } else {
        foliageList = config.getDecoration().getFlora();
      }

      if (foliageList.isEmpty()) {
        return;
      }

      Material m = foliageList.get(random.nextInt(foliageList.size()));
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
  }
}
