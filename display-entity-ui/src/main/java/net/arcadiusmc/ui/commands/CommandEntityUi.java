package net.arcadiusmc.ui.commands;

import com.destroystokyo.paper.ParticleBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Function;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.ui.PageView;
import net.arcadiusmc.ui.PlayerSession;
import net.arcadiusmc.ui.UiPlugin;
import net.arcadiusmc.ui.math.Screen;
import net.arcadiusmc.utils.Particles;
import net.arcadiusmc.utils.Tasks;
import net.forthecrown.grenadier.GrenadierCommand;
import net.forthecrown.grenadier.types.ArgumentTypes;
import net.forthecrown.grenadier.types.options.ArgumentOption;
import net.forthecrown.grenadier.types.options.Options;
import net.forthecrown.grenadier.types.options.OptionsArgument;
import net.forthecrown.grenadier.types.options.ParsedOptions;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.math.vector.Vector3d;

public class CommandEntityUi extends BaseCommand {

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
                    for (PageView view : session.getViews()) {
                      if (view.getBounds() == null) {
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

        .then(literal("spawn-empty-test")
            .executes(c -> {
              Player player = c.getSource().asPlayer();
              Location l = player.getLocation();
              Vector3f pos = new Vector3f((float) l.x(), (float) l.y() + 1, (float) l.z());

              Screen bounds = new Screen();
              bounds.set(pos, 3.0f, 2.0f);

              PageView view = new PageView(l.getWorld());
              view.setPlayer(player);
              view.setWorld(player.getWorld());
              view.setBounds(bounds);

              PlayerSession session = plugin.getSessions().acquireSession(player);
              session.addView(view);

              view.spawnBlank();

//              view.createBlank();
//              view.createTestTooltip();

              c.getSource().sendSuccess(Component.text("Added empty test page"));
              return 0;
            })
        );
  }

  private class RenderTask implements Runnable {

    static final double DIST = 0.25;

    @Override
    public void run() {
      for (PlayerSession session : plugin.getSessions().getSessions()) {
        for (PageView view : session.getViews()) {
          renderView(view);
        }
      }
    }

    private void renderView(PageView view) {
      if (view.getPlayer() == null || view.getWorld() == null || view.getBounds() == null) {
        return;
      }

      Player player = view.getPlayer();
      Screen bounds = view.getBounds();
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
