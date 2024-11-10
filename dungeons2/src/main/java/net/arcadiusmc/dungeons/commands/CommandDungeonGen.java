package net.arcadiusmc.dungeons.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

import com.google.gson.JsonElement;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.dungeons.Doorway;
import net.arcadiusmc.dungeons.DungeonConfig;
import net.arcadiusmc.dungeons.DungeonPiece;
import net.arcadiusmc.dungeons.DungeonWorld;
import net.arcadiusmc.dungeons.GenerationParameters;
import net.arcadiusmc.dungeons.Opening;
import net.arcadiusmc.dungeons.PieceType;
import net.arcadiusmc.dungeons.gen.DungeonGenerator;
import net.arcadiusmc.dungeons.gen.PieceGenerator;
import net.arcadiusmc.dungeons.gen.StairQuadrants;
import net.arcadiusmc.dungeons.gen.StepResult;
import net.arcadiusmc.dungeons.gen.StructureGenerator;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.structure.BlockProcessors;
import net.arcadiusmc.structure.BlockStructure;
import net.arcadiusmc.structure.StructurePlaceConfig;
import net.arcadiusmc.structure.buffer.BlockBuffer;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.utils.Particles;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Direction;
import net.arcadiusmc.utils.math.Transform;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.noise.NoiseGenerator;
import org.bukkit.util.noise.PerlinNoiseGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3d;

public class CommandDungeonGen extends BaseCommand {

  private static final Logger LOGGER = Loggers.getLogger();
  private static boolean drawDoorways = false;
  private static boolean drawRooms = true;

  public static NoiseGenerator perlin = new PerlinNoiseGenerator(new Random());
  private static int octaves = 1;
  private static float frequency = 1f;
  private static float noiseScale = 1f;
  private static float noiseGate = 0f;
  private static float amplitude = 1f;

  private StructureGenerator currentGen;
  private BukkitTask renderTask;
  private DungeonPiece rootPiece;

  public CommandDungeonGen() {
    super("dungeon-gen");
    register();
  }

  private void ensureGenerator() throws CommandSyntaxException {
    if (currentGen != null) {
      return;
    }

    throw Exceptions.create("No generator created");
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("reset-dungeon-world")
            .executes(c -> {
              worldReset();
              c.getSource().sendSuccess(Component.text("Reset dungeon world", NamedTextColor.GRAY));
              return SINGLE_SUCCESS;
            })
        )

        .then(literal("toggle-doorway-rendering")
            .executes(c -> {
              drawDoorways = !drawDoorways;
              c.getSource().sendSuccess(Component.text("Toggled doorway rendering"));
              return SINGLE_SUCCESS;
            })
        )
        .then(literal("toggle-piece-rendering")
            .executes(c -> {
              drawRooms = !drawRooms;
              c.getSource().sendSuccess(Component.text("Toggled piece rendering"));
              return SINGLE_SUCCESS;
            })
        )

        .then(literal("test-stairs")
            .executes(c -> {
              Player player = c.getSource().asPlayer();
              Location location = player.getLocation();

              World world = player.getWorld();

              int x = location.getBlockX();
              int y = location.getBlockY();
              int z = location.getBlockZ();

              for (int i = 0; i < 16; i++) {
                BlockData data = StairQuadrants.createStairs(Material.STONE_STAIRS, i);
                if (data == null) {
                  data = Material.RED_CONCRETE.createBlockData();
                }

                x += 2;
                Block b = world.getBlockAt(x, y, z);
                b.setBlockData(data, false);

                Block above = world.getBlockAt(x, y + 1, z);
                above.setType(Material.OAK_SIGN, false);

                Sign sign = (Sign) above.getState();
                SignSide side = sign.getSide(Side.FRONT);

                String[] names = StairQuadrants.quadrantName(i).split(" \\| ");
                for (int i1 = 0; i1 < names.length; i1++) {
                  side.line(i1, Component.text(names[i1]));
                }

                side.setGlowingText(true);
                sign.update(true, false);
              }

              return SINGLE_SUCCESS;
            })
        )

        .then(literal("perlin-noise-map")
            .then(literal("give")
                .executes(c -> {
                  Player player = c.getSource().asPlayer();
                  PlayerInventory inventory = player.getInventory();

                  ItemStack item = new ItemStack(Material.FILLED_MAP, 1);
                  item.editMeta(MapMeta.class, mapMeta -> {
                    MapView view = Bukkit.createMap(player.getWorld());
                    view.addRenderer(PerlinMapRenderer.RENDERER);
                    mapMeta.setMapView(view);
                  });

                  player.sendMessage(
                      Text.format("Giving perlin map: octaves={0} frequency={1} amplitude={2} noise-scale={3}",
                          octaves,
                          frequency,
                          amplitude,
                          noiseScale
                      )
                  );

                  inventory.addItem(item);
                  return SINGLE_SUCCESS;
                })
            )

            .then(literal("set-octaves")
                .then(argument("octaves", IntegerArgumentType.integer(1))
                    .executes(c -> {
                      octaves = c.getArgument("octaves", Integer.class);
                      return SINGLE_SUCCESS;
                    })
                )
            )
            .then(literal("set-frequency")
                .then(argument("frequency", FloatArgumentType.floatArg())
                    .executes(c -> {
                      frequency = c.getArgument("frequency", Float.class);
                      return SINGLE_SUCCESS;
                    })
                )
            )
            .then(literal("set-amplitude")
                .then(argument("value", FloatArgumentType.floatArg())
                    .executes(c -> {
                      amplitude = c.getArgument("value", Float.class);
                      return SINGLE_SUCCESS;
                    })
                )
            )
            .then(literal("set-noise-scale")
                .then(argument("value", FloatArgumentType.floatArg())
                    .executes(c -> {
                      noiseScale = c.getArgument("value", Float.class);
                      return SINGLE_SUCCESS;
                    })
                )
            )
            .then(literal("set-noise-gate")
                .then(argument("value", FloatArgumentType.floatArg())
                    .executes(c -> {
                      noiseGate = c.getArgument("value", Float.class);
                      return SINGLE_SUCCESS;
                    })
                )
            )
        )

        .then(literal("generate-with-decorators")
            .executes(this::placeWithHeightMap)
        )

        .then(literal("generate-async")
            .executes(c -> {
              DungeonConfig cfg = loadConfig();
              Random random = cfg.createRandom();

              Tasks.runAsync(() -> {
                DungeonPiece rootPiece = DungeonGenerator.generateLevel(cfg, random);

                LOGGER.debug("Finished async level gen");
                this.rootPiece = rootPiece;
                startRenderTask();

                Tasks.runSync(() -> {
                  c.getSource().sendSuccess(Component.text("Generated async level"));
                });
              });

              return SINGLE_SUCCESS;
            })
        )

        .then(literal("generate-fully")
            .executes(c -> {
              ensureGenerator();

              try {
                while (!currentGen.isFinished()) {
                  StepResult r = currentGen.genStep();
                  int steps = currentGen.getSteps();

                  if (r == null) {
                    LOGGER.debug("after genStep(), result=NORESULT, step-num={}",
                        currentGen.getSteps()
                    );

                    if (steps >= 2000) {
                      throw new RuntimeException("Above 2K gen steps, stopping generation");
                    }

                    continue;
                  }

                  LOGGER.debug("after genStep(), result={}, step-num={}",
                      StepResult.codeToString(r.code()), currentGen.getSteps()
                  );

                  if (steps >= 2000) {
                    throw new RuntimeException("Above 2K gen steps, stopping generation");
                  }
                }
              } catch (Throwable t) {
                LOGGER.error("Generation error: ", t);
                throw Exceptions.create("Generation error, check console");
              }

              c.getSource().sendSuccess(Component.text("Generated fully"));
              return SINGLE_SUCCESS;
            })
        )

        .then(literal("new-gen")
            .executes(c -> {
              DungeonConfig config = loadConfig();
              Random random = config.createRandom();

              StringBuilder builder = new StringBuilder();
              printConfig(config, builder);
              LOGGER.debug("Generator config:{}", builder);

              World world = DungeonWorld.get();
              if (world == null) {
                world = worldReset();
              }

              if (!c.getSource().getWorld().equals(world)) {
                c.getSource().asPlayer().teleport(world.getSpawnLocation());
              }

              StructureGenerator generator = new StructureGenerator(config, random);
              currentGen = generator;

              generator.initialize();
              rootPiece = generator.getRootPiece();

              startRenderTask();

              c.getSource().sendSuccess(
                  Component.text("Reset generator state")
              );

              return SINGLE_SUCCESS;
            })
        )

        .then(literal("place-current")
            .executes(c -> {
              if (rootPiece == null) {
                throw Exceptions.create("No generator");
              }

              World world = DungeonWorld.get();

              rootPiece.forEachDescendant(piece -> {
                Holder<BlockStructure> holder = piece.getStructure();
                if (holder == null) {
                  return;
                }

                BlockStructure value = holder.getValue();

                StructurePlaceConfig cfg = StructurePlaceConfig.builder()
                    .addNonNullProcessor()
                    .addRotationProcessor()
                    .addProcessor(BlockProcessors.IGNORE_AIR)
                    .pos(piece.getPivotPoint())
                    .paletteName(piece.getPaletteName())
                    .transform(Transform.rotation(piece.getRotation()))
                    .world(world)
                    .build();

                value.place(cfg);
              });


              return SINGLE_SUCCESS;
            })
        )

        .then(literal("gen-step")
            .executes(this::genStep)

            .then(argument("steps", IntegerArgumentType.integer(1))
                .executes(c -> {
                  int steps = IntegerArgumentType.getInteger(c, "steps");

                  for (int i = 0; i < steps; i++) {
                    genStep(c);
                  }

                  return SINGLE_SUCCESS;
                })
            )
        )

        .then(literal("post-gen")
            .executes(context -> {
              ensureGenerator();
              currentGen.postGeneration();

              context.getSource().sendSuccess(Component.text("Ran post gen"));
              return SINGLE_SUCCESS;
            })

            .then(postGenOp("1-add-boss-room", () -> currentGen.attachBossRoom()))
            .then(postGenOp("2-remove-dead-ends", () -> currentGen.removeDeadEnds()))
            .then(postGenOp("3-close-ending-gates", () -> currentGen.closeEndingGates()))
            .then(postGenOp("4-add-decorated-gates", () -> currentGen.decorateGates()))
        );
  }

  private int placeWithHeightMap(CommandContext<CommandSource> context)
      throws CommandSyntaxException
  {
    DungeonConfig cfg = loadConfig();
    Random random = cfg.createRandom();
    CommandSource source = context.getSource();

    World world = DungeonWorld.get();
    if (world == null) {
      world = worldReset();
    }

    World finalWorld = world;
    Tasks.runAsync(() -> {
      DungeonPiece rootPiece = DungeonGenerator.generateLevel(cfg, random);

      Tasks.runSync(() -> {
        source.sendSuccess(Component.text("Generated dungeon, generating buffers..."));
      });

      LOGGER.debug("Generated async level");

      DungeonGenerator generator = new DungeonGenerator(rootPiece, random, cfg);
      BlockBuffer dungeonBuffer = generator.generateDungeon();

      LOGGER.debug("Starting dungeon placement");

      dungeonBuffer.place(finalWorld)
          .whenComplete((unused, throwable) -> {
            if (throwable != null) {
              LOGGER.error("Failed to place dungeon buffer in world", throwable);
              return;
            }

            Tasks.runSync(() -> {
              source.sendSuccess(Component.text("Placed dungeon, placing heightmap..."));
            });
          });
    });

    return SINGLE_SUCCESS;
  }

  private void startRenderTask() {
    if (renderTask != null) {
      return;
    }

    renderTask = Tasks.runTimer(new RenderTask(), 3, 3);
  }

  private LiteralArgumentBuilder<CommandSource> postGenOp(String name, Callable<?> run) {
    return literal(name)
        .executes(context -> {
          ensureGenerator();

          Object res;

          try {
            res = run.call();
          } catch (Exception e) {
            LOGGER.error("idk", e);
            throw Exceptions.create("Failed to run " + name + " operation, check console");
          }

          CommandSource s = context.getSource();

          if (res == null) {
            s.sendSuccess(Component.text("Ran " + name + " post gen operation"));
          } else {
            s.sendSuccess(Component.text("Ran " + name + " post gen operation, result: " + res));
          }

          return SINGLE_SUCCESS;
        });
  }

  private int genStep(CommandContext<CommandSource> c) throws CommandSyntaxException {
    ensureGenerator();
    StepResult result = currentGen.genStep();

    if (result == null) {
      c.getSource().sendSuccess(
          Component.text("Generation finished")
      );

      return SINGLE_SUCCESS;
    }

    String codeString = StepResult.codeToString(result.code());

    int steps = currentGen.getSteps();
    boolean finished = currentGen.isFinished();

    if (result.entrance() != null) {
      Vector3d center = result.entrance().getFrom().getBoundingBox().center();
      Player player = c.getSource().asPlayer();
      var eyeLoc = player.getEyeLocation();
      var dir = eyeLoc.getDirection();

      Vector3d playerInfront = new Vector3d(
          eyeLoc.getX() + (dir.getX() * 2.0),
          eyeLoc.getY() + (dir.getY() * 2.0),
          eyeLoc.getZ() + (dir.getZ() * 2.0)
      );

      Particles.line(player.getWorld(), playerInfront, center, Color.AQUA);
    }

    c.getSource().sendSuccess(
        Text.format("generator-step: total-steps=&e{0}&r finished=&e{1}&r step-result=&e{2}&r.",
            NamedTextColor.GRAY,
            steps,
            finished,
            codeString
        )
    );

    return SINGLE_SUCCESS;
  }

  private static World worldReset() {
    World world = DungeonWorld.reset();
    world.setDifficulty(Difficulty.HARD);
    world.setTime(6000L);

    world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    world.setGameRule(GameRule.DO_FIRE_TICK, false);
    world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
    world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);

    return world;
  }

  private StringBuilder nlIndent(int ident, StringBuilder builder) {
    return builder.append("\n").append("  ".repeat(ident));
  }

  private StringBuilder field(StringBuilder builder, int ident, String field, Object val) {
    return nlIndent(ident, builder).append(field).append(": ").append(val);
  }

  private void printConfig(DungeonConfig config, StringBuilder builder) {
    int ident = 0;

    nlIndent(ident, builder).append("location: ").append(config.getLocation());
    nlIndent(ident, builder).append("potential-levels: ").append(config.getPotentialLevels());

    nlIndent(ident, builder).append("gen-params: {");
    ident++;
    GenerationParameters params = config.getParameters();
    field(builder, ident, "depthRange", params.getDepthRange());
    field(builder, ident, "roomDepthRange", params.getRoomDepth());
    field(builder, ident, "connectorDepthRange", params.getConnectorDepth());
    field(builder, ident, "maxRoomExits", params.getMaxRoomExits());
    field(builder, ident, "maxConnectorExits", params.getMaxConnectorExits());
    field(builder, ident, "requiredRooms", params.getRequiredRooms());
    field(builder, ident, "roomOpenChance", params.getRoomOpenChance());
    field(builder, ident, "decoratedGateChance", params.getDecoratedGateChance());
    field(builder, ident, "seed", config.getSeed());
    ident--;
    nlIndent(ident, builder).append("}");

    nlIndent(ident, builder).append("piece-types: [");
    ident++;

    for (PieceType pieceType : config.getPieceTypes()) {
      nlIndent(ident, builder).append("{");
      ident++;
      field(builder, ident, "holder", pieceType.getHolder());
      field(builder, ident, "palette", pieceType.getPaletteName());
      field(builder, ident, "kind", pieceType.getKind());
      ident--;
      nlIndent(ident, builder).append("}");
    }

    ident--;
    nlIndent(ident, builder).append("]");
  }

  private DungeonConfig loadConfig() throws CommandSyntaxException {
    Path p = PluginJar.saveResources("gen-config.yml");

    if (!Files.exists(p)) {
      throw Exceptions.create("Failed to create " + p);
    }

    JsonElement el;
    try {
      el = SerializationHelper.readAsJson(p);
    } catch (IOException exc) {
      LOGGER.error("Error reading gen config file at {}", p, exc);
      throw Exceptions.create("IO Error reading generator config, check console");
    }

    return DungeonConfig.CODEC.parse(JsonOps.INSTANCE, el)
        .getOrThrow(s -> {
          LOGGER.error("Failed to parse gen config file at {}: {}", p, s);
          return Exceptions.create("Failed to parse generator config: " + s);
        });
  }
  
  static class PerlinMapRenderer extends MapRenderer {
    
    static final int MAP_SIZE = 127;

    static final PerlinMapRenderer RENDERER = new PerlinMapRenderer();
    
    @Override
    public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
      double px = player.getX();
      double py = Math.floor(player.getY());
      double pz = player.getZ();

      for (int x = 0; x <= MAP_SIZE; x++) {
        for (int z = 0; z <= MAP_SIZE; z++) {
          double mx = x + px - ((double) MAP_SIZE / 2);
          double mz = z + pz - ((double) MAP_SIZE / 2);
          double my = py;

          double xdif = Math.abs(px - mx);
          double zdif = Math.abs(pz - mz);

          if (xdif < 1 && zdif < 1) {
            canvas.setPixelColor(x, z, java.awt.Color.GREEN);
            continue;
          }

          mx *= noiseScale;
          my *= noiseScale;
          mz *= noiseScale;

          double noise = perlin.noise(mx, my, mz, octaves, frequency, amplitude, true);
          noise += 1.0;
          noise /= 2.0;

          if (noise < noiseGate) {
            canvas.setPixelColor(x, z, java.awt.Color.BLACK);
            continue;
          }

          int c = (int) (255 * ((noise + 1.0) / 2.0));
          canvas.setPixelColor(x, z, new java.awt.Color(c, c, c));
        }
      }
    }
  }

  class RenderTask implements Runnable {

    static final Color ROOM_ROOT = Color.RED;
    static final Color ROOM_REG = Color.GREEN;
    static final Color DOORWAY = Color.BLUE;
    static final Color NON_ATTACHED_COLOR = Color.GRAY;

    final Set<DungeonPiece> alreadyDrawn = new ObjectOpenHashSet<>();

    @Override
    public void run() {
      if (currentGen == null) {
        return;
      }

      World world = DungeonWorld.get();
      if (world == null) {
        return;
      }

      if (rootPiece == null) {
        return;
      }

      alreadyDrawn.clear();

      rootPiece.forEachDescendant(piece -> {
        render(world, piece, piece == rootPiece ? ROOM_ROOT : ROOM_REG);
      });

      if (currentGen != null) {
        for (PieceGenerator generator : currentGen.getGenQueue()) {
          render(world, generator.getOriginGate().getFrom(), NON_ATTACHED_COLOR);
        }
      }
    }

    void render(World world, DungeonPiece piece, Color color) {
      if (!alreadyDrawn.add(piece)) {
        return;
      }

      if (drawRooms) {
        Bounds3i boundingBox = piece.getBoundingBox();
        Particles.drawBounds(world, boundingBox, color);
      }

      if (!drawDoorways) {
        return;
      }

      Doorway[] doorways = piece.getDoorways();

      for (Doorway doorway : doorways) {
        Opening opening = doorway.getOpening();
        Direction direction = doorway.getDirection();
        Vector3d center = doorway.getCenter()
            .toDouble()
            .add(0.5, 0.5, 0.5);

        Direction right = direction.right();
        int halfWidth = opening.width() / 2;
        Vector3d dirMod = right.getMod().mul(halfWidth).toDouble();

        Vector3d loRight = center.add(dirMod);
        Vector3d hiRight = loRight.add(0, opening.height(), 0);
        Vector3d loLeft = center.sub(dirMod);
        Vector3d hiLeft = loLeft.add(0, opening.height(), 0);

        Vector3d normalOrigin = center.add(0, (float) opening.height() / 2f, 0);
        Vector3d normalEnd = normalOrigin.add(direction.getMod().toDouble());

        Particles.line(world, loRight, hiRight, DOORWAY);
        Particles.line(world, hiRight, hiLeft, DOORWAY);
        Particles.line(world, loRight, loLeft, DOORWAY);
        Particles.line(world, hiLeft, loLeft, DOORWAY);

        Particles.line(world, normalOrigin, normalEnd, DOORWAY);
      }
    }
  }
}
