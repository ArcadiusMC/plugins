package net.arcadiusmc.cosmetics;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Permissions;
import net.arcadiusmc.menu.Menus;
import net.arcadiusmc.menu.Slot;
import net.arcadiusmc.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

@Getter @Setter
public class CosmeticType<T> {

  private Component name;
  private final List<Component> description = new ArrayList<>();

  private final Map<String, Cosmetic<T>> cosmetics = new HashMap<>();
  private TypeMenuCallback<T> menuCallbacks = MenuCallbacks.defaultCallbacks();

  private int menuSize = Menus.sizeFromRows(5);
  private Slot menuSlot = Slot.ZERO;
  private Material menuItem;
  private Component menuTitle;

  String key;
  int id = -1;

  public void description(Component line) {
    description.add(line);
  }

  public void register(String key, Cosmetic<T> cosmetic) {
    Objects.requireNonNull(key, "Null key");
    Objects.requireNonNull(cosmetic, "Null cosmetic");

    cosmetics.put(key, cosmetic);
    cosmetic.type = this;
    cosmetic.key = key;
  }

  void initPermissions() {
    for (Cosmetic<T> value : cosmetics.values()) {
      String permission;

      if (!Strings.isNullOrEmpty(value.permission)) {
        permission = value.permission;
      } else {
        permission = "arcadius.cosmetics." + key + "." + value.getKey();
        value.permission = permission;
      }

      Permissions.register(permission);
    }
  }

  public CosmeticData<T> getUserData(User user) {
    return new CosmeticData<>(user, this, CosmeticsPlugin.plugin().getActiveMap());
  }

  public Component baseName() {
    return Objects.requireNonNullElseGet(name, () -> Component.text(key));
  }

  public Component displayName() {
    Component base = baseName();

    if (description.isEmpty()) {
      return base;
    }

    var builder = Component.text();
    Iterator<Component> it = description.iterator();
    while (it.hasNext()) {

      builder.append(
          Component.text()
              .append(it.next())
              .color(NamedTextColor.GRAY)
              .build()
      );

      if (it.hasNext()) {
        builder.appendNewline();
      }
    }

    return base.hoverEvent(builder.build());
  }
}
