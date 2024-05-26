package net.arcadiusmc.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.arcadiusmc.ui.math.RayScan;
import net.arcadiusmc.ui.math.Screen;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.joml.Vector2f;
import org.joml.Vector3f;

@RequiredArgsConstructor
public class PlayerSession {

  public static final float MAX_INTERACTION_DISTANCE = 10;

  @Getter
  private final Player player;

  private final List<PageView> views = new ArrayList<>();

  @Getter @Setter
  private PageView selected;

  @Getter
  private PageView target;

  @Getter
  private final Vector2f screenPos = new Vector2f();

  @Getter
  private final Vector3f targetPos = new Vector3f();

  public List<PageView> getViews() {
    return Collections.unmodifiableList(views);
  }

  public void addView(PageView view) {
    views.add(view);

    view.setSession(this);
    view.setPlayer(player);
  }

  public void kill() {
    for (PageView view : views) {
      view.kill();
    }

    views.clear();
  }

  public void tick() {
    recalculateTarget();
    switchSelection();
  }

  private void recalculateTarget() {
    if (views.isEmpty()) {
      target = null;
      targetPos.set(0, 0, 0);
      screenPos.set(0, 0);

      return;
    }

    World world = player.getWorld();
    Location location = player.getEyeLocation();
    Vector dir = location.getDirection();

    RayScan scan = new RayScan(
        new Vector3f((float) location.x(), (float) location.y(), (float) location.z()),
        new Vector3f((float) dir.getX(), (float) dir.getY(), (float) dir.getZ()),
        MAX_INTERACTION_DISTANCE
    );

    for (PageView view : views) {
      if (view.getWorld() == null || !Objects.equals(view.getWorld(), world)) {
        continue;
      }

      Screen bounds = view.getBounds();

      if (bounds == null) {
        continue;
      }

      boolean wasHit = bounds.castRay(scan, targetPos, screenPos);

      if (!wasHit) {
        continue;
      }

      target = view;
      return;
    }

    target = null;
    targetPos.set(0, 0, 0);
    screenPos.set(0, 0);
  }

  private void switchSelection() {
    PageView selected = getSelected();

    if (target == null) {
      if (selected == null) {
        return;
      }

      selected.onUnselect();
      setSelected(null);

      return;
    }

    if (Objects.equals(target, selected)) {
      target.cursorMoveTo(screenPos, targetPos);
      return;
    }

    if (selected != null) {
      selected.onUnselect();
    }

    target.onSelect(screenPos, targetPos);
    setSelected(target);
  }
}
