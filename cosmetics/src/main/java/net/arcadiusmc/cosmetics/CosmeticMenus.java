package net.arcadiusmc.cosmetics;

import net.arcadiusmc.Loggers;
import net.arcadiusmc.menu.MenuBuilder;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.inventory.DefaultItemBuilder;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class CosmeticMenus extends MenuPage {

  private static final Logger LOGGER = Loggers.getLogger();

  public CosmeticMenus() {
    initMenu(
        Menus.builder(Menus.sizeFromRows(4), Component.text("Cosmetics")),
        false
    );
  }

  @Override
  protected void createMenu(MenuBuilder builder) {
    for (CosmeticType<?> type : Cosmetics.TYPES) {
      Slot slot = type.getMenuSlot();

      if (slot == null) {
        LOGGER.warn("Cosmetic type {} has no slot set, cannot display in menu", type.getKey());
        continue;
      }

      MenuNode node = createNode(type);
      builder.add(slot, node);
    }
  }

  private <T> MenuNode createNode(CosmeticType<T> type) {
    return new TypePage<>(this, type);
  }

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    return ItemStacks.builder(Material.NETHER_STAR)
        .setName("Cosmetics")
        .addLore(Text.format("You have {0, gems}", NamedTextColor.GOLD, user.getGems()))
        .build();
  }

  class TypePage<T> extends MenuPage {

    private final CosmeticType<T> type;

    public TypePage(MenuPage parent, CosmeticType<T> type) {
      super(parent);
      this.type = type;

      Component title;
      if (type.getMenuTitle() == null) {
        title = type.baseName();
      } else {
        title = type.getMenuTitle();
      }

      initMenu(
          Menus.builder(type.getMenuSize(), title),
          true
      );
    }

    @Override
    protected void createMenu(MenuBuilder builder) {
      for (Cosmetic<T> value : type.getCosmetics().values()) {
        Slot slot = value.getMenuSlot();
        builder.add(slot, value.toMenuNode());
      }
    }

    @Override
    public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
      DefaultItemBuilder builder = ItemStacks.builder(type.getMenuItem())
          .setName(type.baseName());

      for (Component component : type.getDescription()) {
        builder.addLoreRaw(
            Component.text()
                .append(component)
                .color(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)
                .build()
        );
      }

      return builder.build();
    }
  }
}
