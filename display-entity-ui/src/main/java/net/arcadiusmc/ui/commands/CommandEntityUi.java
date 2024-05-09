package net.arcadiusmc.ui.commands;

import com.destroystokyo.paper.ParticleBuilder;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.arcadiusmc.command.BaseCommand;
import net.arcadiusmc.ui.PageView;
import net.arcadiusmc.ui.PlayerSession;
import net.arcadiusmc.ui.UiPlugin;
import net.arcadiusmc.ui.math.ScreenBounds;
import net.arcadiusmc.utils.Particles;
import net.arcadiusmc.utils.Tasks;
import net.forthecrown.grenadier.GrenadierCommand;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.spongepowered.math.vector.Vector3d;

public class CommandEntityUi extends BaseCommand {

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

        .then(literal("rotate-all")
            .then(argument("degrees", FloatArgumentType.floatArg())
                .executes(c -> {
                  float degrees = c.getArgument("degrees", Float.class);

                  Matrix3f matrix = new Matrix3f();
                  matrix.rotateY(Math.toRadians(degrees));

                  int rotated = 0;

                  for (PlayerSession session : plugin.getSessions().getSessions()) {
                    for (PageView view : session.getViews()) {
                      if (view.getBounds() == null) {
                        continue;
                      }

                      view.getBounds().apply(matrix);
                      rotated++;
                    }
                  }

                  c.getSource().sendSuccess(
                      Component.text("Rotated " + rotated + " page views")
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

              ScreenBounds bounds = new ScreenBounds();
              bounds.set(pos, 3.0f, 2.0f);

              PageView view = new PageView();
              view.setPlayer(player);
              view.setWorld(player.getWorld());
              view.setBounds(bounds);

              PlayerSession session = plugin.getSessions().acquireSession(player);
              session.addView(view);

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
      ScreenBounds bounds = view.getBounds();
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
}
