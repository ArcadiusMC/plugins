package net.arcadiusmc.holograms;

import java.util.Collection;
import java.util.Optional;
import net.arcadiusmc.utils.Tasks;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.spongepowered.math.vector.Vector3f;

public class HologramTicker {

  static final int DELAY = Ticks.TICKS_PER_SECOND;

  private final ServiceImpl service;
  private BukkitTask task;

  public HologramTicker(ServiceImpl service) {
    this.service = service;
  }

  public void startTicking() {
    stopTicking();
    task = Tasks.runTimer(this::tick, DELAY, DELAY);
  }

  public void stopTicking() {
    task = Tasks.cancel(task);
  }

  private void tick() {
    updatePositions(service.getBoards().values());
    updatePositions(service.getTexts().values());
  }

  private void updatePositions(Collection<? extends Hologram> collection) {
    Location loc = new Location(null, 0, 0, 0);

    for (Hologram hologram : collection) {
      if (!hologram.isSpawned()) {
        continue;
      }

      Optional<TextDisplay> opt = hologram.getEntity();

      if (opt.isEmpty()) {
        continue;
      }

      TextDisplay gotten = opt.get();
      gotten.getLocation(loc);

      if (loc.equals(hologram.getLocation())) {
        continue;
      }

      hologram.setLocation(loc, false);
      updateFromEntity(gotten, hologram);
    }
  }

  private void updateFromEntity(TextDisplay display, Hologram h) {
    TextDisplayMeta meta = h.getDisplayMeta();
    Transformation trans = display.getTransformation();

    meta.setScale(toSponge(trans.getScale()));
    meta.setTranslation(toSponge(trans.getTranslation()));
    meta.setBillboard(display.getBillboard());
    meta.setAlign(display.getAlignment());
    meta.setBrightness(display.getBrightness());
    meta.setBackgroundColor(display.getBackgroundColor());
    meta.setYaw(display.getYaw());
    meta.setPitch(display.getPitch());
    meta.setShadowed(display.isShadowed());
    meta.setSeeThrough(meta.isSeeThrough());
    meta.setLineWidth(meta.getLineWidth());
    meta.setOpacity(display.getTextOpacity());

  }

  private Vector3f toSponge(org.joml.Vector3f joml) {
    return Vector3f.from(joml.x, joml.y, joml.z);
  }
}
