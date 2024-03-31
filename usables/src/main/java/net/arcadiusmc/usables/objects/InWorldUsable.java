package net.arcadiusmc.usables.objects;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.forthecrown.nbt.BinaryTags;
import net.forthecrown.nbt.CompoundTag;
import net.forthecrown.nbt.paper.PaperNbt;
import org.bukkit.persistence.PersistentDataContainer;

@Getter @Setter
public abstract class InWorldUsable extends Usable implements VanillaCancellable {

  public static final String CANCEL_VANILLA = "cancelVanilla";

  private VanillaCancelState cancelVanilla = VanillaCancelState.FALSE;

  @Override
  public void setCancelVanilla(VanillaCancelState cancelVanilla) {
    Objects.requireNonNull(cancelVanilla, "Null state");
    this.cancelVanilla = cancelVanilla;
  }

  @Override
  public void fillContext(Map<String, Object> context) {
    super.fillContext(context);
    context.put(CANCEL_VANILLA, cancelVanilla);
  }

  public void save() {
    executeOnContainer(true, pdc -> {
      CompoundTag tag = BinaryTags.compoundTag();
      save(tag);

      PaperNbt.mergeToContainer(pdc, tag);
    });
  }

  @Override
  public void save(CompoundTag tag) {
    super.save(tag);
    tag.put(CANCEL_VANILLA, cancelVanilla.save());
  }

  public void load() {
    clear();
    executeOnContainer(false, pdc -> {
      CompoundTag tag = PaperNbt.fromDataContainer(pdc);
      load(tag);
    });
  }

  @Override
  public void load(CompoundTag tag) {
    super.load(tag);
    this.cancelVanilla = VanillaCancelState.load(tag.get(CANCEL_VANILLA));
  }

  protected abstract void executeOnContainer(
      boolean saveIntent,
      Consumer<PersistentDataContainer> consumer
  );
}
