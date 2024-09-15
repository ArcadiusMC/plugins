package net.arcadiusmc.usables.trigger;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;
import net.arcadiusmc.utils.Particles;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.math.Bounds3i;
import net.arcadiusmc.utils.math.Vectors;
import net.arcadiusmc.utils.math.WorldBounds3i;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class TriggerDraw {

  static final int DISTANCE = 30;
  static final double PARTICLE_DIST = 1.5;

  private final TriggerManager manager;

  private final Set<Player> toggledPlayers = new ObjectOpenHashSet<>();
  private BukkitTask task;

  final Set<AreaTrigger> alreadyDrawn = new ObjectOpenHashSet<>();

  public TriggerDraw(TriggerManager manager) {
    this.manager = manager;
  }

  public boolean isToggled(Player player) {
    return toggledPlayers.contains(player);
  }

  public void addPlayer(Player player) {
    if (!toggledPlayers.add(player)) {
      return;
    }

    startDrawing();
  }

  public void removePlayer(Player player) {
    if (!toggledPlayers.remove(player)) {
      return;
    }
    if (!toggledPlayers.isEmpty()) {
      return;
    }

    stopDrawing();
  }

  public void startDrawing() {
    if (isDrawing()) {
      return;
    }

    task = Tasks.runTimer(this::render, 1, 1);
  }

  public void stopDrawing() {
    task = Tasks.cancel(task);
  }

  public boolean isDrawing() {
    return Tasks.isScheduled(task);
  }

  private void render() {
    alreadyDrawn.clear();
    toggledPlayers.removeIf(player -> !player.isOnline());

    if (toggledPlayers.isEmpty()) {
      stopDrawing();
      return;
    }

    for (Player toggledPlayer : toggledPlayers) {
      drawFor(toggledPlayer);
    }
  }

  private void drawFor(Player player) {
    Bounds3i drawRange = Bounds3i.of(Vectors.intFrom(player.getLocation()), DISTANCE);

    Set<AreaTrigger> overlapping = manager.getChunkMap()
        .getOverlapping(player.getWorld(), drawRange);

    if (overlapping.isEmpty()) {
      return;
    }

    for (AreaTrigger areaTrigger : overlapping) {
      draw(areaTrigger);
    }
  }

  private void draw(AreaTrigger trigger) {
    if (!alreadyDrawn.add(trigger)) {
      return;
    }

    WorldBounds3i area = trigger.getArea();

    double pre = Particles.lineDrawDist;
    Particles.lineDrawDist = PARTICLE_DIST;
    Particles.drawBounds(area.getWorld(), area, areaColor(trigger));
    Particles.lineDrawDist = pre;
  }

  private Color areaColor(AreaTrigger trigger) {
    return switch (trigger.getType()) {
      case EXIT -> Color.RED;
      case ENTER -> Color.GREEN;
      case MOVE -> Color.BLUE;
      default -> Color.RED.mixColors(Color.GREEN);
    };
  }
}
