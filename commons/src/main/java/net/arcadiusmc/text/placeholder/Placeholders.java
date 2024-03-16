package net.arcadiusmc.text.placeholder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.arcadiusmc.BukkitServices;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Placeholder-related utility class
 */
public final class Placeholders {
  private Placeholders() {}

  private static PlaceholderService service;

  public static PlaceholderService getService() {
    return service == null
        ? (service = BukkitServices.loadOrThrow(PlaceholderService.class))
        : service;
  }

  public static PlaceholderList newList() {
    return getService().newList();
  }

  public static PlaceholderRenderer newRenderer() {
    return getService().newRenderer();
  }


  public static void replaceItemPlaceholders(
      PlaceholderRenderer renderer,
      ItemMeta meta,
      User viewer
  ) {
    replaceItemPlaceholders(renderer, meta, viewer, null);
  }

  public static void replaceItemPlaceholders(
      PlaceholderRenderer renderer,
      ItemMeta meta,
      User viewer,
      Map<String, Object> context
  ) {
    if (meta.hasLore()) {
      List<Component> lore = meta.lore();
      assert lore != null;

      List<Component> renderedLore = lore.stream()
          .map(component -> renderer.render(component, viewer, context))
          .toList();

      meta.lore(renderedLore);
    }

    if (meta.hasDisplayName()) {
      var baseName = meta.displayName();
      assert baseName != null;
      meta.displayName(renderer.render(baseName, viewer, context));
    }
  }

  public static Component renderString(String str, Audience viewer) {
    var message = PlayerMessage.allFlags(str).create(viewer);
    var renderer = newRenderer().useDefaults();
    return renderer.render(message, viewer);
  }

  public static void addDefault(String name, TextPlaceholder placeholder) {
    getService().getDefaults().add(name, placeholder);
  }

  public static void removeDefault(String name) {
    getService().getDefaults().remove(name);
  }

  public static void createPlayerPlaceholders(PlaceholderRenderer list, String prefix, Player player) {
    createPlayerPlaceholders(list, prefix, Users.get(player));
  }

  public static void createPlayerPlaceholders(PlaceholderRenderer list, String prefix, UUID playerId) {
    var service = Users.getService();
    var entry = service.getLookup().getEntry(playerId);

    if (entry == null) {
      return;
    }

    createPlayerPlaceholders(list, prefix, Users.get(entry));
  }

  public static void createPlayerPlaceholders(PlaceholderRenderer list, String prefix, User player) {
    PlayerPlaceholders placeholders = new PlayerPlaceholders(prefix, player);
    list.addSource(placeholders);
  }

  public static Component render(Component text) {
    return newRenderer().useDefaults().render(text);
  }

  public static Component render(Component text, Audience viewer) {
    return newRenderer().useDefaults().render(text, viewer);
  }
}
