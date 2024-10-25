package net.arcadiusmc.dungeons.gen;

import static org.bukkit.Material.AIR;
import static org.bukkit.Material.GLOW_LICHEN;
import static org.bukkit.Material.SPAWNER;

import com.google.common.base.Stopwatch;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.dungeons.DungeonConfig;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.LevelFunctions;
import net.arcadiusmc.dungeons.NoiseParameter;
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
import net.arcadiusmc.utils.VanillaAccess;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Transform;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.spawner.SpawnRule;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.Skeleton;
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

  private final Map<String, NoiseGenerator> noiseMaps = new Object2ObjectOpenHashMap<>();
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

    this.noiseGen = new PerlinNoiseGenerator(random);
    this.noiseMaps.put("noise-gen", noiseGen);
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

  public NoiseGenerator getNoiseMap(String name) {
    NoiseGenerator found = noiseMaps.get(name);
    if (found != null) {
      return found;
    }

    found = new PerlinNoiseGenerator(random);
    noiseMaps.put(name, found);

    return found;
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

    for (int i = 0; i < config.getDecorationPasses().size(); i++) {
      DecorationPass pass = config.getDecorationPasses().get(i);
      runPass(pass);
    }
  }

  private void runPass(DecorationPass pass) {
    String name = pass.getName();

    if (pass.isDisabled()) {
      LOGGER.info("Decorator {} is disabled. Skipping...", name);
      return;
    }

    Stopwatch stopWatch = Stopwatch.createStarted();
    pass.getDecorator().bind(this);

    try {
      pass.getDecorator().execute();
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

  /* --------------------------- Spawner pass ---------------------------- */

  public void spawnerPass() {
    List<GeneratorFunction> list = getFunctions(LevelFunctions.SPAWNER);
    World world = Worlds.overworld();
    Location l = new Location(world, 0, 0, 0);

    Skeleton skeleton = world.createEntity(l, Skeleton.class);

    EntitySnapshot snapshot = skeleton.createSnapshot();
    assert snapshot != null;

    for (GeneratorFunction func : list) {
      int x = func.getPosition().x();
      int y = func.getPosition().y();
      int z = func.getPosition().z();

      if (!isAir(x, y, z)) {
        continue;
      }

      CreatureSpawner state = (CreatureSpawner) SPAWNER.createBlockData().createBlockState();
      state.setSpawnCount(3);
      state.setMinSpawnDelay(2 * 20);
      state.setMaxSpawnDelay(10 * 20);
      state.setRequiredPlayerRange(10);

      state.addPotentialSpawn(snapshot, 10, new SpawnRule(0, 15, 0, 15));

      setBlock(x, y, z, state);
    }
  }

  public boolean hasSupport(int x, int y, int z, BlockFace face) {
    BlockState block = getBlock(x, y, z);
    if (block == null) {
      return false;
    }

    BlockData data = block.getBlockData();
    return data.isFaceSturdy(face, BlockSupport.FULL);
  }

  public boolean isVoidBelow(int x, int y, int z) {
    return isVoidInDirection(x, y, z, -1);
  }

  public boolean isSkyAbove(int x, int y, int z) {
    return isVoidInDirection(x, y, z, 1);
  }

  public boolean isVoidInDirection(int x, int y, int z, int yDir) {
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

  public boolean isAir(int x, int y, int z) {
    BlockState block = getBlock(x, y, z);
    if (block == null) {
      return true;
    }

    return block.getBlockData().getMaterial().isAir();
  }

  public boolean isGroundBlock(int x, int y, int z) {
    if (!isAir(x, y + 1, z)) {
      return false;
    }

    BlockState block = getBlock(x, y, z);
    if (block == null) {
      return false;
    }

    return block.getBlockData().isFaceSturdy(BlockFace.UP, BlockSupport.FULL);
  }

  public BlockState getBlock(int x, int y, int z) {
    return buffer.getBlock(x, y, z);
  }

  public BlockData getBlockData(int x, int y, int z) {
    var b = getBlock(x, y, z);
    if (b == null) {
      return null;
    }
    return b.getBlockData();
  }

  public Material getBlockType(int x, int y, int z) {
    var b = getBlock(x, y, z);
    if (b == null) {
      return AIR;
    }
    return b.getType();
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

  public void setBlock(int x, int y, int z, BlockState state) {
    buffer.setBlock(x, y, z, state);
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

  public boolean isSupportedByAnyFace(int x, int y, int z) {
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

  public void collectFunctions(
      DungeonPiece piece,
      Holder<BlockStructure> structure,
      StructurePlaceConfig cfg
  ) {
    for (FunctionInfo function : structure.getValue().getFunctions()) {
      GeneratorFunction func = new GeneratorFunction(function.getFunctionKey(), piece);
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

  public int freeSpaceDown(int x, int y, int z) {
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

  public List<GeneratorFunction> getFunctionsIn(String functionKey, DungeonPiece piece) {
    return getFunctions(functionKey)
        .stream()
        .filter(func -> Objects.equals(piece, func.getContainingPiece()))
        .collect(ObjectArrayList.toList());
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
              new PoolFunctionProcessor(piece, buffer, random, DungeonGenerator.this)
          )
          .build();

      structure.getValue().place(cfg);
      collectFunctions(piece, structure, cfg);
    }

    @Override
    public double getIntegrity(Vector3i worldPosition) {
      NoiseParameter parameter = config.getDecoration().getBlockRot();
      return parameter.sample(noiseGen, worldPosition.x(), worldPosition.y(), worldPosition.z());
    }
  }
}
