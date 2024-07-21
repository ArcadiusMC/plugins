package net.arcadiusmc.ui.commands;

import com.destroystokyo.paper.ParticleBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.command.Exceptions;
import net.arcadiusmc.ui.PlayerSession;
import net.arcadiusmc.ui.UiPlugin;
import net.arcadiusmc.ui.math.Screen;
import net.arcadiusmc.ui.resource.PageRef;
import net.arcadiusmc.ui.struct.Document;
import net.arcadiusmc.ui.struct.Node;
import net.arcadiusmc.ui.struct.XmlVisitor;
import net.arcadiusmc.ui.style.StylePropertyMap;
import net.arcadiusmc.ui.style.Styles;
import net.arcadiusmc.ui.style.Stylesheet;
import net.arcadiusmc.utils.Particles;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.PathUtil;
import net.forthecrown.grenadier.CommandSource;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.spongepowered.math.vector.Vector3d;

public class CommandEntityUi extends BaseCommand {

  private static final Logger LOGGER = Loggers.getLogger();

  static final ArgumentOption<Vector3f> TRANSLATE
      = Options.argument(NumberBasedParser.VEC_3F, "translation");

  static final ArgumentOption<Vector3f> SCALE
      = Options.argument(NumberBasedParser.VEC_3F, "scale");

  static final ArgumentOption<Quaternionf> LEFT_ROTATION
      = Options.argument(NumberBasedParser.QUATERNION, "left-rotation");

  static final ArgumentOption<Quaternionf> RIGHT_ROTATION
      = Options.argument(NumberBasedParser.QUATERNION, "right-rotation");

  public static final OptionsArgument TRANSFORM_ARGS = OptionsArgument.builder()
      .addOptional(TRANSLATE)
      .addOptional(SCALE)
      .addOptional(LEFT_ROTATION)
      .addOptional(RIGHT_ROTATION)
      .build();

  private final UiPlugin plugin;

  private BukkitTask renderTask;

  private Map<UUID, Document> lastSpawned = new HashMap<>();

  public CommandEntityUi(UiPlugin plugin) {
    super("entity-ui");

    this.plugin = plugin;

    setAliases("entity-pages");
    setDescription("Manages entity UIs");

    register();
  }

  @Override
  public void createCommand(GrenadierCommand command) {
    command
        .then(literal("open")
            .then(argument("path", new ModulePathParser(plugin.getLoader()))
                .executes(this::load)
            )
        )

        .then(literal("dump-xml")
            .executes(c -> {
              Player player = c.getSource().asPlayer();
              Document doc = lastSpawned.get(player.getUniqueId());

              if (doc == null) {
                throw Exceptions.create("None spawned");
              }

              dumpToDebugFile(doc.getBody());

              c.getSource().sendSuccess(Component.text("Dumped"));
              return 0;
            })
        )

        .then(literal("toggle-debug-outlines")
            .executes(c -> {
              Document.DEBUG_OUTLINES = !Document.DEBUG_OUTLINES;
              c.getSource().sendSuccess(Component.text("Toggled"));
              return 0;
            })
        )

        .then(literal("toggle-bounds-draw")
            .executes(c -> {
              if (renderTask == null) {
                renderTask = Tasks.runTimer(new RenderTask(), 1, 1);
                c.getSource().sendSuccess(Component.text("Now rendering outlines"));
              } else {
                renderTask = Tasks.cancel(renderTask);
                c.getSource().sendSuccess(Component.text("No longer rendering outlines"));
              }

              return 0;
            })
        )

        .then(literal("apply-transformation")
            .then(argument("transform", TRANSFORM_ARGS)
                .executes(c -> {
                  ParsedOptions options = ArgumentTypes.getOptions(c, "transform");

                  Transformation transformation = new Transformation(
                      options.getValueOptional(TRANSLATE).orElseGet(Vector3f::new),
                      options.getValueOptional(LEFT_ROTATION).orElseGet(Quaternionf::new),
                      options.getValueOptional(SCALE).orElseGet(() -> new Vector3f(1)),
                      options.getValueOptional(RIGHT_ROTATION).orElseGet(Quaternionf::new)
                  );

                  int rotated = 0;

                  for (PlayerSession session : plugin.getSessions().getSessions()) {
                    for (Document view : session.getViews()) {
                      if (view.getScreen() == null) {
                        continue;
                      }

                      view.transform(transformation);
                      rotated++;
                    }
                  }

                  c.getSource().sendSuccess(
                      Component.text("Applied transformation to " + rotated + " page views")
                  );

                  return 0;
                })
            )
        )

        .then(literal("test-style-parser")
            .then(literal("sheet")
                .then(argument("input", StringArgumentType.greedyString())
                    .executes(c -> runStyleParser(c, true))
                )
            )

            .then(literal("inline")
                .then(argument("input", StringArgumentType.greedyString())
                    .executes(c -> runStyleParser(c, false))
                )
            )
        );
  }

  private int load(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
    CommandSource source = ctx.getSource();
    Player player = source.asPlayer();

    PageRef ref = ctx.getArgument("path", PageRef.class);

    DataResult<Document> res = plugin.getLoader().openDocument(ref, player);

    if (res.isError()) {
      res.ifError(err -> LOGGER.error(err.message()));
      return 0;
    }

    Document doc = res.getOrThrow();
    PlayerSession session =  plugin.getSessions().acquireSession(player);

    session.addView(doc);
    doc.spawn();

    dumpToDebugFile(doc.getBody());

    return 0;
  }

  private void dumpToDebugFile(Node node) {
    Path path = PathUtil.pluginPath("xml-dump.xml");

    XmlVisitor visitor = new XmlVisitor();
    visitor.visit(node);

    try {
      Files.writeString(path, visitor.toString());
    } catch (IOException exc) {
      LOGGER.error("Failed to write dump file to {}", path, exc);
    }
  }

  private int runStyleParser(CommandContext<CommandSource> c, boolean sheet)
      throws CommandSyntaxException
  {
    CommandSource source = c.getSource();
    String input = c.getArgument("input", String.class);

    DataResult<String> result;

    if (sheet) {
      result = Styles.parseStylesheet(input).map(Stylesheet::toString);
    } else {
      result = Styles.parseInlineStyle(input).map(StylePropertyMap::toString);
    }

    if (result.isError()) {
      result.ifError(stringError -> {
        LOGGER.error("Error parsing style: {}", stringError.message());
      });

      throw Exceptions.create("Failed to parse style, check console for proper error log");
    }

    result.ifSuccess(s -> {
      source.sendSuccess(Component.text("Parsed style: " + s));
      LOGGER.info("Parsed style: {}", s);
    });

    return 0;
  }

  private class RenderTask implements Runnable {

    static final double DIST = 0.25;

    @Override
    public void run() {
      for (PlayerSession session : plugin.getSessions().getSessions()) {
        for (Document view : session.getViews()) {
          renderView(view);
        }
      }
    }

    private void renderView(Document view) {
      if (view.getPlayer() == null || view.getWorld() == null || view.getScreen() == null) {
        return;
      }

      Player player = view.getPlayer();
      Screen bounds = view.getScreen();
      World w = player.getWorld();

      Vector3d lowerLeft  = toSponge(bounds.getLowerLeft());
      Vector3d lowerRight = toSponge(bounds.getLowerRight());
      Vector3d upperLeft  = toSponge(bounds.getUpperLeft());
      Vector3d upperRight = toSponge(bounds.getUpperRight());

      Vector3d center = toSponge(bounds.center());
      Vector3d normal = toSponge(bounds.normal());

      ParticleBuilder builder;

      if (view.isSelected()) {
        builder = Particle.FLAME.builder();
      } else {
        builder = Particle.SOUL_FIRE_FLAME.builder();
      }

      builder
          .count(1)
          .extra(0)
          .location(player.getLocation())
          .receivers(player);

      Particles.line(lowerLeft, lowerRight, DIST, w, builder);
      Particles.line(lowerLeft, upperLeft, DIST, w, builder);
      Particles.line(lowerRight, upperRight, DIST, w, builder);
      Particles.line(upperLeft, upperRight, DIST, w, builder);

      Particles.line(center, center.add(normal), DIST, w, builder);
    }

    private Vector3d toSponge(Vector3f joml) {
      return Vector3d.from(joml.x, joml.y, joml.z);
    }
  }

  private static class NumberBasedParser<S> implements ArgumentType<S> {

    static final NumberBasedParser<Vector3f> VEC_3F
        = new NumberBasedParser<>(3, arr -> new Vector3f(arr[0], arr[1], arr[2]));

    static final NumberBasedParser<Quaternionf> QUATERNION = new NumberBasedParser<>(3, arr -> {
      Quaternionf quaternionf = new Quaternionf();
      quaternionf.rotateY(Math.toRadians(arr[0]));
      quaternionf.rotateX(Math.toRadians(arr[1]));
      quaternionf.rotateZ(Math.toRadians(arr[2]));
      return quaternionf;
    });

    private final int requiredAxesCount;
    private final Function<float[], S> ctor;

    public NumberBasedParser(int requiredAxesCount, Function<float[], S> ctor) {
      this.requiredAxesCount = requiredAxesCount;
      this.ctor = ctor;
    }

    @Override
    public S parse(StringReader reader) throws CommandSyntaxException {
      float[] arr = new float[requiredAxesCount];
      int i = 0;

      while (i < arr.length) {
        float f = reader.readFloat();
        arr[i++] = f;

        if (i < arr.length) {
          reader.expect(',');
        }
      }

      return ctor.apply(arr);
    }
  }
}
