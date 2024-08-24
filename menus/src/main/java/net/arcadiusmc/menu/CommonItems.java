package net.arcadiusmc.menu;

import net.arcadiusmc.utils.inventory.DefaultItemBuilder;
import net.arcadiusmc.utils.inventory.ItemStacks;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class CommonItems {
  private CommonItems() {}

  public static final int ID_GO_BACK = 10010001;
  public static final int ID_NEXT_PAGE = 10010002;
  public static final int ID_PREVIOUS_PAGE = 10010003;
  public static final int ID_GREEN_TICK = 10010004;
  public static final int ID_RED_CROSS = 10010005;
  public static final int ID_GRAYED_RED_CROSS = 10010006;
  public static final int ID_GRAYED_GREEN_TICK = 10010007;

  public static final Material TYPE_GO_BACK = Material.PAPER;
  public static final Material TYPE_NEXT_PAGE = Material.PAPER;
  public static final Material TYPE_PREVIOUS_PAGE = Material.PAPER;
  public static final Material TYPE_GREEN_TICK = Material.GREEN_STAINED_GLASS_PANE;
  public static final Material TYPE_RED_CROSS = Material.RED_STAINED_GLASS_PANE;
  public static final Material TYPE_GRAYED_RED_CROSS = Material.RED_STAINED_GLASS_PANE;
  public static final Material TYPE_GRAYED_GREEN_TICK = Material.GREEN_STAINED_GLASS_PANE;

  public static ItemStack goBack() {
    return goBackBuilder().build();
  }

  public static DefaultItemBuilder goBackBuilder() {
    return ItemStacks.builder(TYPE_GO_BACK)
        .setName("&e< Go back")
        .addLore("&7Go back to the previous page")
        .setModelData(ID_GO_BACK);
  }

  public static ItemStack nextPage() {
    return nextPageBuilder().build();
  }

  public static DefaultItemBuilder nextPageBuilder() {
    return ItemStacks.builder(TYPE_NEXT_PAGE)
        .setName("&e>> Next page >>")
        .setModelData(ID_NEXT_PAGE);
  }

  public static ItemStack previousPage() {
    return previousPageBuilder().build();
  }

  public static DefaultItemBuilder previousPageBuilder() {
    return ItemStacks.builder(TYPE_PREVIOUS_PAGE)
        .setName("&e<< Previous page <<")
        .setModelData(ID_PREVIOUS_PAGE);
  }

  public static DefaultItemBuilder greenTick() {
    return ItemStacks.builder(TYPE_GREEN_TICK)
        .setModelData(ID_GREEN_TICK);
  }

  public static DefaultItemBuilder redCross() {
    return ItemStacks.builder(TYPE_RED_CROSS)
        .setModelData(ID_RED_CROSS);
  }

  public static DefaultItemBuilder grayedGreenTick() {
    return ItemStacks.builder(TYPE_GRAYED_GREEN_TICK)
        .setModelData(ID_GRAYED_GREEN_TICK);
  }

  public static DefaultItemBuilder grayedRedCross() {
    return ItemStacks.builder(TYPE_GRAYED_RED_CROSS)
        .setModelData(ID_GRAYED_RED_CROSS);
  }
}
