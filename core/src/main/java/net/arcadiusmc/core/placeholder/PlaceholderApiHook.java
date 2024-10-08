package net.arcadiusmc.core.placeholder;

import com.google.common.base.Strings;
import it.unimi.dsi.fastutil.objects.ObjectIntPair;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.manager.LocalExpansionManager;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.PlaceholderSource;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Audiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class PlaceholderApiHook implements PlaceholderSource {

  private final LocalExpansionManager manager;

  public PlaceholderApiHook() {
    this.manager = PlaceholderAPIPlugin.getInstance().getLocalExpansionManager();
  }

  private ObjectIntPair<PlaceholderExpansion> findExpansion(String name) {
    int index = name.indexOf('_');

    if (index == -1) {
      return makePair(name, index);
    }

    while (true) {
      String substring = name.substring(0, index);
      PlaceholderExpansion exp = manager.getExpansion(substring);

      if (exp != null) {
        return ObjectIntPair.of(exp, index);
      }

      index = name.substring(index + 1).indexOf(' ');

      if (index == -1) {
        return makePair(name, -1);
      }
    }
  }

  private ObjectIntPair<PlaceholderExpansion> makePair(String name, int index) {
    PlaceholderExpansion exp = manager.getExpansion(name);

    if (exp == null) {
      return null;
    }

    return ObjectIntPair.of(exp, index);
  }

  @Override
  public TextPlaceholder getPlaceholder(String name, PlaceholderContext ctx) {
    ObjectIntPair<PlaceholderExpansion> pair = findExpansion(name);

    if (pair == null) {
      return null;
    }

    String params;

    if (pair.rightInt() == -1) {
      params = "";
    } else {
      params = name.substring(pair.rightInt() + 1);
    }

    return new ApiPlaceholder(pair.left(), params);
  }

  record ApiPlaceholder(PlaceholderHook hook, String params) implements TextPlaceholder {
    @Override
    public @Nullable Component render(String match, PlaceholderContext render) {
      OfflinePlayer player;

      if (render.viewer() == null) {
        player = null;
      } else {
        User user = Audiences.getUser(render.viewer());
        player = user == null ? null : user.getOfflinePlayer();
      }

      String combinedParams;

      if (Strings.isNullOrEmpty(params)) {
        combinedParams = match;
      } else if (Strings.isNullOrEmpty(match)) {
        combinedParams = params;
      } else  {
        combinedParams = params + ":" + match;
      }

      String text = hook.onRequest(player, combinedParams);

      if (text == null) {
        return null;
      }

      return LegacyComponentSerializer.legacySection().deserialize(text);
    }
  }
}
