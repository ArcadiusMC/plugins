package net.arcadiusmc.holograms.placeholders;

import com.google.common.base.Strings;
import net.arcadiusmc.holograms.BoardImpl;
import net.arcadiusmc.holograms.Holograms;
import net.arcadiusmc.text.placeholder.PlaceholderContext;
import net.arcadiusmc.text.placeholder.PlaceholderList;
import net.arcadiusmc.text.placeholder.PlaceholderService;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.text.placeholder.TextPlaceholder;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class HologramPlaceholders {

  static final String LB = "lb";

  public static void registerAll() {
    PlaceholderService service = Placeholders.getService();
    PlaceholderList defaults = service.getDefaults();
    defaults.add(LB, LeaderboardPlaceholder.PLACEHOLDER);
  }

  public static void unregister() {
    PlaceholderService service = Placeholders.getService();
    PlaceholderList defaults = service.getDefaults();
    defaults.remove(LB);
  }

  private enum LeaderboardPlaceholder
      implements TextPlaceholder
  {
    PLACEHOLDER;

    @Override
    public @Nullable Component render(String match, PlaceholderContext render) {
      if (Strings.isNullOrEmpty(match)) {
        return null;
      }

      return Holograms.getService().getLeaderboard(match)
          .map(leaderboard -> {
            return (BoardImpl) leaderboard;
          })
          .map(board -> board.renderText(render.viewer()))
          .orElse(null);
    }
  }
}
