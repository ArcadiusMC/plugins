package net.arcadiusmc.items;

import static net.arcadiusmc.items.ExtendedItem.TAG_CONTAINER;
import static net.arcadiusmc.items.ExtendedItem.TAG_DATA;
import static net.arcadiusmc.items.ExtendedItem.TAG_TYPE_KEY;

import java.util.Optional;
import java.util.UUID;
import net.arcadiusmc.items.wreath.WreathType;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.forthecrown.nbt.CompoundTag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public final class ItemTypes {
  private ItemTypes() {}

  public static final Registry<ItemType> REGISTRY = Registries.newRegistry();

  static void registerAll() {
    REGISTRY.register("emperors_wreath", new WreathType());
  }

  public static ExtendedItem createItem(Holder<ItemType> type, @Nullable UUID ownerId) {
    ItemType itemType = type.getValue();
    ItemStack base = itemType.createBaseItem();
    ExtendedItem item = new ExtendedItem(type, base);

    itemType.addComponents(item);

    item.getComponent(Owner.class).ifPresent(owner -> owner.setPlayerId(ownerId));

    item.init();
    item.update();

    return item;
  }

  public static Optional<ExtendedItem> getItem(ItemStack stack) {
    if (ItemStacks.isEmpty(stack)) {
      return Optional.empty();
    }

    ItemMeta meta = stack.getItemMeta();
    CompoundTag containerTag = ItemStacks.getTagElement(meta, TAG_CONTAINER);

    String str = containerTag.getString(TAG_TYPE_KEY);

    if (!Registries.isValidKey(str)) {
      return Optional.empty();
    }

    return REGISTRY.getHolder(str)
        .map(holder -> {
          ExtendedItem item = new ExtendedItem(holder, stack);
          holder.getValue().addComponents(item);

          item.init();
          item.load(containerTag.getCompound(TAG_DATA));

          return item;
        });
  }
}
