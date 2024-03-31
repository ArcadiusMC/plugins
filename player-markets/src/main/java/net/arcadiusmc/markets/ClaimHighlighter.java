package net.arcadiusmc.markets;

import com.destroystokyo.paper.ParticleBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.utils.Particles;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.ExtraCodecs;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.math.Vectors;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.BlockArgument.Result;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

public class ClaimHighlighter {

  private static final Logger LOGGER = Loggers.getLogger();

  static final int LINES_PER_POINT = 3;

  private final Path configFile;

  private final Map<UUID, Highlight> highlightMap = new HashMap<>();
  private final Set<UUID> playersToRemove = new HashSet<>(25);

  private Config config = Config.EMPTY;

  private BukkitTask ticker;

  public ClaimHighlighter(MarketsPlugin plugin) {
    this.configFile = plugin.getDataFolder().toPath().resolve("bounds-render.yml");
  }

  void load() {
    PluginJar.saveResources("bounds-render.yml", configFile);

    SerializationHelper.readAsJson(configFile, object -> {
      Config.CODEC.parse(JsonOps.INSTANCE, object)
          .mapError(string -> "Failed to load bounds render config: " + string)
          .resultOrPartial(LOGGER::error)
          .ifPresent(config -> this.config = config);
    });
  }

  void schedule() {
    ticker = Tasks.runTimer(this::tick, 1, 1);
  }

  void cancel() {
    ticker = Tasks.cancel(ticker);
  }

  public boolean show(User user, Market market) {
    if (highlightMap.containsKey(user.getUniqueId())) {
      return false;
    }

    Optional<ProtectedRegion> opt = market.getRegion();
    if (opt.isEmpty()) {
      return false;
    }

    ProtectedRegion region = opt.get();
    Line[] shape = createShape(region);

    ClaimRender render;

    if (config.preferred == RenderType.PARTICLES || config.blocks.isEmpty()) {
      render = RenderType.PARTICLES.createRender(shape);
    } else {
      render = RenderType.BLOCKS.createRender(shape);
    }

    Highlight highlight = new Highlight(market.getWorld(), render);
    highlightMap.put(user.getUniqueId(), highlight);

    render.onBegin(market.getWorld(), user, config);

    return true;
  }

  private void tick() {
    if (highlightMap.isEmpty()) {
      return;
    }

    playersToRemove.clear();

    for (Entry<UUID, Highlight> entry : highlightMap.entrySet()) {
      UUID playerId = entry.getKey();
      User user = Users.get(playerId);

      if (!user.isOnline()) {
        continue;
      }

      Highlight highlight = entry.getValue();
      highlight.render.tick(highlight.world, user, config);

      highlight.visibleTicks++;

      long maxVisTicks = Tasks.toTicks(config.visibilityPeriod);

      if (highlight.visibleTicks >= maxVisTicks) {
        playersToRemove.add(playerId);
        highlight.render.onEnd(highlight.world, user, config);
      }
    }

    for (UUID playerId : playersToRemove) {
      highlightMap.remove(playerId);
    }
  }

  private Line[] createShape(ProtectedRegion region) {
    if (region.getType() == RegionType.GLOBAL) {
      return new Line[0];
    }

    if (region.getType() == RegionType.CUBOID) {
      BlockVector3 weMin = region.getMinimumPoint();
      BlockVector3 weMax = region.getMaximumPoint();

      Vector3d min = Vector3d.from(weMin.getX(), weMin.getY(), weMin.getZ());
      Vector3d max = Vector3d.from(weMax.getX(), weMax.getY(), weMax.getZ()).add(Vector3d.ONE);
      Line[] shape = new Line[12];

      Vector3d[] points = {
          min,
          min.withX(max.x()),
          min.withZ(max.z()),
          max.withY(min.y()),

          max,
          max.withX(min.x()),
          max.withZ(min.z()),
          min.withY(max.y())
      };

      // Bottom
      shape[0]  = line(points[0], points[1]);
      shape[1]  = line(points[0], points[2]);
      shape[2]  = line(points[1], points[3]);
      shape[3]  = line(points[2], points[3]);

      // Top
      shape[4]  = line(points[4], points[5]);
      shape[5]  = line(points[4], points[6]);
      shape[6]  = line(points[5], points[7]);
      shape[7]  = line(points[6], points[7]);

      // Sides
      shape[8]  = line(points[0], points[7]);
      shape[9]  = line(points[1], points[6]);
      shape[10] = line(points[2], points[5]);
      shape[11] = line(points[3], points[4]);

      return shape;
    }

    ProtectedPolygonalRegion polygon = (ProtectedPolygonalRegion) region;

    double minY = polygon.getMinimumPoint().getY();
    double maxY = polygon.getMaximumPoint().getY();

    List<BlockVector2> points = polygon.getPoints();

    // Assume the polygon has 3 points, 2 lines to connect the top
    // and bottom of each point, and an extra line to connect the
    // top to the bottom
    int lineCount = points.size() * LINES_PER_POINT;

    Line[] shape = new Line[lineCount];
    Vector3d halfblock = Vector3d.from(.5);

    for (int i = 0; i < points.size(); i++) {
      BlockVector2 point = points.get(i);
      BlockVector2 next = i == points.size() - 1 ? points.get(0) : points.get(i + 1);

      Vector3d pTop = Vector3d.from(point.getX(), maxY, point.getZ()).add(halfblock);
      Vector3d pBot = Vector3d.from(point.getX(), minY, point.getZ()).add(halfblock);

      Vector3d nTop = Vector3d.from(next.getX(), maxY, next.getZ()).add(halfblock);
      Vector3d nBot = Vector3d.from(next.getX(), minY, next.getZ()).add(halfblock);

      int index = i * LINES_PER_POINT;
      shape[index + 0] = line(pTop, pBot);
      shape[index + 1] = line(pTop, nTop);
      shape[index + 2] = line(pBot, nBot);
    }

    return shape;
  }

  private Line line(Vector3d start, Vector3d end) {
    return new Line(start, end);
  }

  private record Line(Vector3d start, Vector3d end) {

    void draw(World world, ParticleBuilder builder, double particleDist) {
      Particles.line(start, end, particleDist, world, builder);
    }
  }

  record ParticleRender(Line[] lines) implements ClaimRender {

    @Override
    public void tick(World world, User user, Config config) {
      ParticleBuilder builder = config.particle.builder()
          .location(user.getLocation())
          .receivers(user.getPlayer())
          .extra(0d)
          .count(1)
          .force(true);

      for (Line line : lines) {
        line.draw(world, builder, config.particleDistance);
      }
    }
  }

  record BlockRender(List<Vector3i> blocks) implements ClaimRender {

    @Override
    public void onBegin(World world, User user, Config config) {
      Player player = user.getPlayer();
      Map<BlockPosition, BlockData> fakeBlocks = new HashMap<>(blocks.size());

      List<BlockData> blockList = config.blocks;
      int materialPointer = 0;

      for (Vector3i block : blocks) {
        BlockData data = blockList.get(materialPointer);

        materialPointer++;
        materialPointer %= blockList.size();

        fakeBlocks.put(Position.block(block.x(), block.y(), block.z()), data);
      }

      player.sendMultiBlockChange(fakeBlocks);
    }

    @Override
    public void onEnd(World world, User user, Config config) {
      Player player = user.getPlayer();
      List<BlockState> reset = new ArrayList<>(blocks.size());

      for (Vector3i pos : blocks) {
        Block block = Vectors.getBlock(pos, world);
        reset.add(block.getState());
      }

      player.sendBlockChanges(reset);
    }
  }

  interface ClaimRender {
    default void onBegin(World world, User user, Config config) {

    }

    default void tick(World world, User user, Config config) {

    }

    default void onEnd(World world, User user, Config config) {

    }
  }

  private class Highlight {
    final World world;
    final ClaimRender render;

    int visibleTicks = 0;

    public Highlight(World world, ClaimRender render) {
      this.world = world;
      this.render = render;
    }
  }

  private enum RenderType {
    PARTICLES {
      @Override
      ClaimRender createRender(Line[] shape) {
        return new ParticleRender(shape);
      }
    },

    BLOCKS {
      @Override
      ClaimRender createRender(Line[] shape) {
        List<Vector3i> blocks = new ArrayList<>();
        for (Line line : shape) {
          collectBetween(line.start, line.end, blocks);
        }

        return new BlockRender(blocks);
      }

      void collectBetween(Vector3d from, Vector3d to, List<Vector3i> destination) {
        Vector3d dif = to.sub(from);
        double len = dif.length();

        Vector3d step = dif.div(len);

        for (double i = 0; i < len; i++) {
          Vector3d off = step.mul(i);
          Vector3d pos = off.add(from);
          Vector3i intPos = pos.toInt();

          if (destination.contains(intPos)) {
            continue;
          }

          destination.add(intPos);
        }
      }
    };

    abstract ClaimRender createRender(Line[] shape);
  }

  private record Config(
      Duration visibilityPeriod,
      RenderType preferred,
      Particle particle,
      double particleDistance,
      List<BlockData> blocks
  ) {
    static final Config EMPTY = new Config(
        Duration.ofSeconds(30),
        RenderType.PARTICLES,
        Particle.FLAME,
        .25d,
        List.of(
            Material.YELLOW_CONCRETE.createBlockData(),
            Material.BLACK_CONCRETE.createBlockData()
        )
    );

    static final Codec<BlockData> BLOCK_DATA_CODEC = Codec.STRING.comapFlatMap(
        string -> ExtraCodecs.safeParse(string, ArgumentTypes.block()).map(Result::getParsedState),
        BlockData::getAsString
    );

    static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> {
      return instance
          .group(
              ExtraCodecs.DURATION
                  .optionalFieldOf("visibility-period", EMPTY.visibilityPeriod)
                  .forGetter(Config::visibilityPeriod),

              ExtraCodecs.enumCodec(RenderType.class)
                  .optionalFieldOf("preferred-render", EMPTY.preferred)
                  .forGetter(Config::preferred),

              ExtraCodecs.registryCodec(Registry.PARTICLE_TYPE)
                  .optionalFieldOf("particle", EMPTY.particle)
                  .forGetter(Config::particle),

              Codec.DOUBLE.optionalFieldOf("particle-distance", EMPTY.particleDistance)
                  .forGetter(Config::particleDistance),

              ExtraCodecs
                  .strictOptional(
                      BLOCK_DATA_CODEC.listOf(),
                      "block-render-materials",
                      List.of()
                  )
                  .forGetter(Config::blocks)
          )
          .apply(instance, Config::new);
    });
  }
}
