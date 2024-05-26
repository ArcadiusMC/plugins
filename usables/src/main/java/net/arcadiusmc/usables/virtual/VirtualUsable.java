package net.arcadiusmc.usables.virtual;

import static net.arcadiusmc.usables.objects.InWorldUsable.CANCEL_VANILLA;

import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.usables.objects.Usable;
import net.arcadiusmc.usables.objects.VanillaCancelState;
import net.arcadiusmc.usables.objects.VanillaCancellable;
import net.forthecrown.nbt.CompoundTag;
import net.kyori.adventure.text.Component;

@Getter
public class VirtualUsable extends Usable implements VanillaCancellable {

  static final String TAG_TRIGGERS = "triggers";

  private String name;

  private VanillaCancelState cancelVanilla = VanillaCancelState.FALSE;

  VirtualUsableManager manager;

  final TriggerList triggers;

  public VirtualUsable(String name) {
    Objects.requireNonNull(name, "Null name");
    this.name = name;
    this.triggers = new TriggerList(this);
  }

  public void setCancelVanilla(VanillaCancelState cancelVanilla) {
    Objects.requireNonNull(cancelVanilla, "Null state");
    this.cancelVanilla = cancelVanilla;
  }

  public void setName(String name) {
    Objects.requireNonNull(name, "Null name");

    if (manager != null && manager.containsUsable(name)) {
      throw new IllegalStateException("Name '" + name + "' is already in use");
    }

    VirtualUsableManager manager = this.manager;

    if (manager != null) {
      manager.internalRemove(name);
    }

    this.name = name;

    if (manager != null) {
      manager.internalAdd(this);
    }
  }

  @Override
  public void save(CompoundTag tag) {
    super.save(tag);
    tag.put(CANCEL_VANILLA, cancelVanilla.save());
    save(triggers, tag, TAG_TRIGGERS);
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    this.cancelVanilla = VanillaCancelState.load(tag.get(CANCEL_VANILLA));
    load(tag.get(TAG_TRIGGERS), triggers);
  }

  @Override
  public void fillContext(Map<String, Object> context) {
    super.fillContext(context);
    context.put(CANCEL_VANILLA, cancelVanilla);
  }

  @Override
  public void write(TextWriter writer) {
    writer.field("Cancel-Vanilla", cancelVanilla);
    super.write(writer);
    triggers.write(writer, getCommandPrefix());
  }

  @Override
  public String getCommandPrefix() {
    return "/virtual-usable " + getName();
  }

  @Override
  public Component name() {
    return Component.text(getName());
  }
}
