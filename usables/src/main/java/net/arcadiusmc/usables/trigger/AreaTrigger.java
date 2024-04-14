package net.arcadiusmc.usables.trigger;

import com.mojang.serialization.Dynamic;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.usables.objects.Usable;
import net.arcadiusmc.usables.virtual.RegionAction;
import net.arcadiusmc.usables.virtual.TriggerMap;
import net.arcadiusmc.utils.io.TagOps;
import net.arcadiusmc.utils.io.TagUtil;
import net.arcadiusmc.utils.math.WorldBounds3i;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.spongepowered.math.vector.Vector3d;
import org.spongepowered.math.vector.Vector3i;

@Getter
public class AreaTrigger extends Usable {

  private String name;

  private WorldBounds3i area;

  @Setter
  private Type type;

  TriggerManager manager;

  final TriggerMap<RegionAction> externalTriggers;

  public AreaTrigger() {
    this.externalTriggers = new TriggerMap<>(RegionAction.CODEC);
    this.type = Type.ENTER;
  }

  @Override
  public Component name() {
    return Component.text(getName());
  }

  @Override
  public void write(TextWriter writer) {
    writer.field("Type", type.name().toLowerCase());
    writer.field("Region", clickableRegion());
    super.write(writer);
  }

  Component clickableRegion() {
    Vector3i min = area.min();
    Vector3i max = area.max();

    return Text.format("({0, vector} -> {1, vector})", NamedTextColor.GRAY, min, max)
        .clickEvent(ClickEvent.runCommand(getCommandPrefix() + " select"));
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);

    externalTriggers.clear();
    if (tag.contains("external_triggers")) {
      externalTriggers.load(new Dynamic<>(TagOps.OPS, tag.get("external_triggers")))
          .mapError(s -> "Error loading '" + name + "' external triggers: " + s)
          .resultOrPartial(LOGGER::error);
    }

    this.type = TagUtil.readEnum(Type.class, tag.get("type"));
    this.area = WorldBounds3i.of(tag.getCompound("area"));
  }

  @Override
  public String getCommandPrefix() {
    return "/triggers " + getName();
  }

  @Override
  public void save(CompoundTag tag) {
    super.save(tag);
    tag.put("type", TagUtil.writeEnum(type));
    tag.put("area", area.save());

    if (!externalTriggers.isEmpty()) {
      externalTriggers.save(TagOps.OPS)
          .mapError(s -> "Failed to save external triggers inside '" + name + "': " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(binaryTag -> tag.put("external_triggers", binaryTag));
    }
  }

  @Override
  public void fillContext(Map<String, Object> context) {
    super.fillContext(context);
    context.put("area", area);
    context.put("triggerName", name);

    Vector3d center = area.center();
    Location location = new Location(area.getWorld(), center.x(), center.y(), center.z());

    context.put("location", location);
    context.put("world", area.getWorld());
  }

  public void setArea(WorldBounds3i area) {
    Objects.requireNonNull(area);

    TriggerManager manager = this.manager;

    if (manager != null) {
      manager.remove(this);
    }

    this.area = area;

    if (manager != null) {
      manager.add(this);
    }
  }

  public void setName(String name) {
    Objects.requireNonNull(name);

    TriggerManager manager = this.manager;

    if (manager != null) {
      manager.remove(this);
    }

    this.name = name;

    if (manager != null) {
      manager.add(this);
    }
  }

  public enum Type {
    ENTER, EXIT, EITHER, MOVE
  }
}
