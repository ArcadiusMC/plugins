package net.arcadiusmc.cosmetics;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.utils.inventory.DefaultItemBuilder;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.Nullable;

@Getter
public class Cosmetic<T> {

  private final T value;
  private final Slot menuSlot;
  private final Component displayName;
  private final ImmutableList<Component> description;

  CosmeticType<T> type;
  String permission;
  String key;

  private MenuNode cachedNode;

  @Builder
  public Cosmetic(
      T value,
      Slot menuSlot,
      Component displayName,
      @Nullable String permission,
      @Singular("description") List<Component> description
  ) {
    Objects.requireNonNull(value, "Null value");
    Objects.requireNonNull(menuSlot, "Null menu slot");
    Objects.requireNonNull(displayName, "Null display name");

    this.value = value;
    this.menuSlot = menuSlot;
    this.displayName = displayName;
    this.permission = permission;

    this.description = description == null
        ? ImmutableList.of()
        : ImmutableList.copyOf(description);
  }

  public Component displayName() {
    if (description.isEmpty()) {
      return displayName;
    }

    TextComponent.Builder builder = Component.text();
    Iterator<Component> it = description.iterator();

    while (it.hasNext()) {
      builder.append(
          Component.text()
              .color(NamedTextColor.GRAY)
              .append(it.next())
              .build()
      );

      if (it.hasNext()) {
        builder.appendNewline();
      }
    }

    return displayName.hoverEvent(builder.build());
  }

  public MenuNode toMenuNode() {
    if (cachedNode != null) {
      return cachedNode;
    }

    return cachedNode = MenuNode.builder()
        .setItem((user, context) -> {
          CosmeticData<T> data = type.getUserData(user);
          boolean has = data.has(this);
          boolean set = Objects.equals(this, data.getActive());

          Material material;

          if (set) {
            material = Material.LIME_DYE;
          } else if (has) {
            material = Material.ORANGE_DYE;
          } else {
            material = Material.GRAY_DYE;
          }

          DefaultItemBuilder builder = ItemStacks.builder(material)
              .setName(displayName);

          for (Component component : description) {
            builder.addLore(
                Component.text()
                    .color(NamedTextColor.GRAY)
                    .append(component)
                    .build()
            );
          }

          if (set) {
            builder.addEnchant(Enchantment.BINDING_CURSE, 1)
                .addFlags(ItemFlag.HIDE_ENCHANTS);

            builder.addLore(
                Messages.render("cosmetics.status.active")
                    .addValue("type", type.displayName())
                    .create(user)
            );
          }

          if (type.getMenuCallbacks() != null) {
            type.getMenuCallbacks().appendInfo(builder, user, this, data);
          }

          return builder.build();
        })
        .setRunnable((user, context, click) -> {
          TypeMenuCallback<T> menuCallbacks = type.getMenuCallbacks();

          if (menuCallbacks == null) {
            return;
          }

          CosmeticData<T> data = type.getUserData(user);

          if (data.has(this)) {
            menuCallbacks.onOwnedClick(user, this, data);
          } else {
            menuCallbacks.onUnownedClick(user, this, data);
          }

          click.shouldReloadMenu(true);
        })
        .build();
  }

  public static <T> CosmeticBuilder<T> builder(T value) {
    CosmeticBuilder<T> builder = new CosmeticBuilder<>();
    builder.value = value;
    return builder;
  }
}
