package net.arcadiusmc.waypoints.menu;

import com.google.common.base.Strings;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.arcadiusmc.menu.ClickContext;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.page.ListPage;
import net.arcadiusmc.menu.page.MenuPage;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.UserProperty;
import net.arcadiusmc.utils.context.Context;
import net.arcadiusmc.utils.inventory.ItemStacks;
import net.arcadiusmc.waypoints.Waypoint;
import net.arcadiusmc.waypoints.WaypointManager;
import net.arcadiusmc.waypoints.WaypointPrefs;
import net.arcadiusmc.waypoints.visit.WaypointVisit;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WaypointListPage extends ListPage<Waypoint> {

  public WaypointListPage(MenuPage parent) {
    super(parent, WaypointMenus.PAGE);

    setSortingOptions(new SortingOptions<WaypointOrder, Waypoint>() {
      @Override
      public UserProperty<WaypointOrder> getProperty() {
        return WaypointPrefs.MENU_ORDER;
      }

      @Override
      public UserProperty<Boolean> inversionProperty() {
        return WaypointPrefs.MENU_ORDER_INVERTED;
      }

      @Override
      public WaypointOrder[] values() {
        return WaypointOrder.values();
      }

      @Override
      public String displayName(WaypointOrder order) {
        return order.displayName();
      }

      @Override
      public String categoryName() {
        return "waypoints";
      }
    });

    initMenu(Menus.builder(Menus.MAX_INV_SIZE, "Waypoint List"), true);
  }

  @Override
  public @Nullable ItemStack createItem(@NotNull User user, @NotNull Context context) {
    return ItemStacks.builder(Material.KNOWLEDGE_BOOK)
        .setName("&eWaypoint List")
        .build();
  }

  @Override
  protected List<Waypoint> getList(User user, Context context) {
    return getList(user);
  }

  public static List<Waypoint> getList(User user) {
    WaypointManager manager = WaypointManager.getInstance();

    Stream<Waypoint> stream = manager.getWaypoints()
        .stream()
        .filter(waypoint -> !Strings.isNullOrEmpty(waypoint.getEffectiveName()));

    ArrayList<Waypoint> list = new ArrayList<>(stream.toList());

    WaypointOrder order = user.get(WaypointPrefs.MENU_ORDER);
    boolean inverted = user.get(WaypointPrefs.MENU_ORDER_INVERTED);

    if (inverted) {
      list.sort(order.reversed());
    } else {
      list.sort(order);
    }

    return list;
  }

  @Override
  protected ItemStack getItem(User user, Waypoint entry, Context context) {
    return entry.createDisplayItem(user)
        .addLoreRaw(Component.empty())
        .addLore("&6Click to visit!")
        .build();
  }

  @Override
  protected void onClick(User user, Waypoint entry, Context context, ClickContext click)
      throws CommandSyntaxException
  {
    WaypointVisit.visit(user, entry);
    click.shouldClose(true);
  }
}
