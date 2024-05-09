package net.arcadiusmc.ui;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.ui.math.ScreenBounds;
import net.arcadiusmc.ui.render.DocumentRender;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.w3c.dom.Document;

@Getter @Setter
public class PageView {

  private static final Logger LOGGER = Loggers.getLogger();

  private final List<Display> entities = new ArrayList<>();

  private Document document;
  private DocumentRender render;
  private ScreenBounds bounds;
  private World world;
  private Player player;
  private PlayerSession session;

  boolean selected = false;

  private Vector2f cursorPos = new Vector2f(-1);

  public PageView() {
    this.render = new DocumentRender(this);
  }

  public void onInteract(PageInteraction interaction) {
    selected = true;
    Vector3f hitPos = interaction.worldPos();

    Particle.DUST.builder()
        .color(Color.GREEN, 5f)
        .location(world, hitPos.x, hitPos.y, hitPos.z)
        .receivers(player)
        .spawn();
  }

  public void onSelect(Vector2f screenPoint, Vector3f worldPoint) {
    selected = true;
    cursorPos.set(screenPoint);
  }

  public void cursorMoveTo(Vector2f screenPoint, Vector3f worldPoint) {
    Vector2f old = new Vector2f(cursorPos);
    Vector2f dif = new Vector2f(cursorPos).sub(screenPoint);



    cursorPos.set(screenPoint);
  }

  public void onUnselect() {
    selected = false;
    cursorPos.set(-1);
  }

  public void addEntity(Display display) {
    display.setPersistent(false);
    entities.add(display);
  }

  public void kill() {
    for (Display entity : entities) {
      entity.remove();
    }

    document = null;
    render = null;
    world = null;
  }
}
