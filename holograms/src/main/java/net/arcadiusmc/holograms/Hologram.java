package net.arcadiusmc.holograms;

import static net.kyori.adventure.text.Component.text;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.TextWriters;
import net.arcadiusmc.text.parse.ChatParseFlag;
import net.arcadiusmc.utils.Locations;
import net.arcadiusmc.utils.PluginUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

public abstract class Hologram implements HolographicDisplay {

  static final Set<ChatParseFlag> TEXT_FLAGS = Set.of(
      ChatParseFlag.IGNORE_CASE,
      ChatParseFlag.IGNORE_SWEARS,
      ChatParseFlag.COLORS,
      ChatParseFlag.EMOJIS,
      ChatParseFlag.GRADIENTS,
      ChatParseFlag.TAGGING,
      ChatParseFlag.TIMESTAMPS
  );

  public static final NamespacedKey LEADERBOARD_KEY
      = new NamespacedKey("arcadiusmc", "leaderboard");

  public static final NamespacedKey HOLOGRAM_KEY
      = new NamespacedKey("arcadiusmc", "hologram");

  private final NamespacedKey entityKey;

  @Getter
  final String name;

  Location location;
  Reference<TextDisplay> ref;

  @Getter @Setter
  boolean spawned = false;

  @Getter @Setter
  TextDisplayMeta displayMeta = new TextDisplayMeta();

  ServiceImpl service;

  public Hologram(NamespacedKey entityKey, String name) {
    this.entityKey = entityKey;
    this.name = name;
  }

  public void setLocation(Location location) {
    boolean spawned = isSpawned();

    if (spawned) {
      kill();
    }

    if (this.location != null) {
      this.location.getChunk().removePluginChunkTicket(PluginUtil.getPlugin());
    }

    if (service != null) {
      service.getTriggers().onLocationSet(this, location);
    }

    this.location = Locations.clone(location);

    if (this.location != null) {
      this.location.getChunk().addPluginChunkTicket(PluginUtil.getPlugin());

      if (spawned) {
        spawn();

        if (service != null) {
          service.getTriggers().onUpdate(this);
        }
      }
    }
  }

  public Location getLocation() {
    return Locations.clone(location);
  }

  @Override
  public boolean update() {
    if (service == null) {
      return false;
    }

    if (!spawned) {
      return kill();
    }

    if (spawn()) {
      return true;
    }

    var opt = getEntity();
    if (opt.isEmpty()) {
      return false;
    }

    var display = opt.get();
    applyTo(display);

    service.getTriggers().onUpdate(this);
    return true;
  }

  @Override
  public boolean kill() {
    spawned = false;

    var opt = getEntity();
    if (opt.isEmpty()) {
      return false;
    }
    opt.get().remove();
    return true;
  }

  @Override
  public boolean spawn() {
    if (location == null || getEntity().isPresent()) {
      return false;
    }

    TextDisplay textDisplay = location.getWorld().spawn(location, TextDisplay.class);
    applyTo(textDisplay);

    ref = new WeakReference<>(textDisplay);
    spawned = true;

    if (service != null) {
      service.getTriggers().onUpdate(this);
    }

    return true;
  }

  protected void applyTo(TextDisplay display) {
    display.addScoreboardTag(name);
    display.getPersistentDataContainer().set(entityKey, PersistentDataType.STRING, name);
    displayMeta.apply(display);
  }

  Optional<TextDisplay> getEntity() {
    if (ref == null) {
      return locateDisplay();
    }

    return Optional.ofNullable(ref.get())
        .filter(Entity::isValid)
        .or(this::locateDisplay);
  }

  private Optional<TextDisplay> locateDisplay() {
    if (location == null) {
      return Optional.empty();
    }

    Chunk chunk = location.getChunk();

    if (!chunk.isEntitiesLoaded()) {
      // Force-load entities
      Entity[] entityArray = chunk.getEntities();
      TextDisplay fromArray = findFromArray(entityArray);

      if (fromArray != null) {
        ref = new WeakReference<>(fromArray);
        return Optional.of(fromArray);
      }
    }

    Collection<TextDisplay> nearby = location.getNearbyEntitiesByType(TextDisplay.class, .5);
    nearby.removeIf(textDisplay -> !isBoardEntity(textDisplay));

    if (nearby.isEmpty()) {
      return Optional.empty();
    }

    TextDisplay display;

    if (nearby.size() > 1) {
      var it = nearby.iterator();
      display = it.next();

      while (it.hasNext()) {
        var n = it.next();
        n.remove();
      }
    } else {
      display = nearby.iterator().next();
    }

    ref = new WeakReference<>(display);
    return Optional.of(display);
  }

  private boolean isBoardEntity(Entity entity) {
    if (!(entity instanceof TextDisplay)) {
      return false;
    }

    var pdc = entity.getPersistentDataContainer();
    if (!pdc.has(entityKey)) {
      return false;
    }

    String string = pdc.get(entityKey, PersistentDataType.STRING);
    return Objects.equals(string, name);
  }

  private TextDisplay findFromArray(Entity[] entities) {
    for (Entity entity : entities) {
      if (isBoardEntity(entity)) {
        return (TextDisplay) entity;
      }
    }

    return null;
  }

  public Component displayName() {
    return text("[" + name + "]", isSpawned() ? NamedTextColor.GREEN : NamedTextColor.GRAY)
        .hoverEvent(infoText());
  }

  public Component infoText() {
    TextWriter writer = TextWriters.newWriter();
    writer.setFieldStyle(Style.style(NamedTextColor.GRAY));
    writeHover(writer);
    return writer.asComponent();
  }

  protected Component editableTextFormat(String argName, PlayerMessage message) {
    return text()
        .append(text("[", NamedTextColor.AQUA))
        .append(text(message.getMessage()))
        .append(text("]", NamedTextColor.AQUA))

        .hoverEvent(text("Click to edit"))

        .clickEvent(ClickEvent.suggestCommand(
            argName.contains("you-format")
                ? String.format("/lb %s set %s %s", argName, name, message.getMessage())
                : String.format("/lb %s %s %s", argName, name, message.getMessage())
        ))

        .build();
  }

  protected void writeHover(TextWriter writer) {

  }
}
