package net.arcadiusmc.items.guns;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.items.ExtendedItem;
import net.arcadiusmc.items.ItemType;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.nbt.BinaryTags;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Getter @Setter
public class GunType<T extends Gun> implements ItemType {

  private Material material;
  private Integer textureId;
  private Component name;

  private Supplier<T> factory;
  private Consumer<T> consumer;

  @Override
  public ItemStack createBaseItem() {
    Objects.requireNonNull(material, "No material set");

    return ItemStacks.builder(material)
        .setModelData(textureId)
        .setName(name)
        .build();
  }

  @Override
  public void addComponents(ExtendedItem item) {
    Objects.requireNonNull(factory, "Null Factory");

    T gunComponent = factory.get();

    // Load default values from an empty tag because
    // this was written for an event not to be permanent,
    // but it's here now so idc
    gunComponent.load(BinaryTags.compoundTag());

    if (consumer != null) {
      consumer.accept(gunComponent);
    }

    item.addComponent(gunComponent);
  }
}
