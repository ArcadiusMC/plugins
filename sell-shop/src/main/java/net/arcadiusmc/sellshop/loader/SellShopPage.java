package net.arcadiusmc.sellshop.loader;

import java.util.List;
import java.util.Stack;
import net.arcadiusmc.menu.CommonItems;
import net.arcadiusmc.menu.MenuBuilder;
import net.arcadiusmc.menu.MenuNode;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.sellshop.SellShop;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SellShopPage extends MenuPage {

  static final MenuNode PARENT_BUTTON = MenuNode.builder()
      .setItem((user, context) -> {
        Stack<SellShopPage> stack = context.get(SellShop.PAGE_STACK);

        if (stack == null || stack.isEmpty()) {
          return null;
        }

        return CommonItems.goBack();
      })
      .setRunnable((user, context, click) -> {
        Stack<SellShopPage> stack = context.get(SellShop.PAGE_STACK);

        if (stack == null || stack.isEmpty()) {
          return;
        }

        SellShopPage parent = stack.peek();

        click.shouldReloadMenu(false);

        stack.pop();
        parent.onClick(user, context, click);
      })
      .build();

  int size;
  Component title;
  Component[] desc;
  Material headerItem;
  ItemStack border;
  MenuNode[] nodes;
  Style nameStyle;
  List<String> tags;

  public SellShopPage() {

  }

  public void initialize() {
    var builder = Menus.builder(size, title);
    initMenu(builder, false);
  }

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    if (headerItem == null) {
      return null;
    }
    var builder = ItemStacks.builder(headerItem);
    builder.setName(title.applyFallbackStyle(nameStyle));

    if (desc != null) {
      for (Component component : desc) {
        if (component == null) {
          continue;
        }
        builder.addLore(component);
      }
    }

    return builder.build();
  }

  @Override
  protected MenuNode createHeader() {
    if (headerItem == null || headerItem.isAir()) {
      return null;
    }
    return super.createHeader();
  }

  @Override
  protected void addBorder(MenuBuilder builder) {
    if (ItemStacks.isEmpty(border)) {
      return;
    }

    builder.addBorder(border);
  }

  @Override
  protected void createMenu(MenuBuilder builder) {
    builder.add(Slot.ZERO, PARENT_BUTTON);

    for (int i = 0; i < nodes.length; i++) {
      MenuNode node = nodes[i];

      if (node == null) {
        continue;
      }

      builder.add(i, node);
    }
  }
}
