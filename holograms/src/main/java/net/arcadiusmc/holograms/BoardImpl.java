package net.arcadiusmc.holograms;

import static net.kyori.adventure.text.Component.text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.registry.Holder;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.PlayerMessage;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextWriter;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.Audiences;
import net.forthecrown.grenadier.types.IntRangeArgument.IntRange;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Getter @Setter
public class BoardImpl extends Hologram implements Leaderboard {

  private static final Logger LOGGER = Loggers.getLogger();

  static final Comparator<LeaderboardScore> ASCENDING_COMPARATOR
      = Comparator.comparingInt(LeaderboardScore::value);

  static final Comparator<LeaderboardScore> DESCENDING_COMPARATOR
      = ASCENDING_COMPARATOR.reversed();

  static final Component DEFAULT_FORMAT = text("${index}) ${entry}: ${score}");

  static final TextReplacementConfig NEW_LINE_REPLACER = TextReplacementConfig.builder()
      .match("\\\\[nN]")
      .replacement("\n")
      .build();

  static final PlayerMessage DEFAULT_YOU = PlayerMessage.allFlags("You");

  Holder<LeaderboardSource> source;

  PlayerMessage footer;
  PlayerMessage header;
  PlayerMessage format;
  PlayerMessage youFormat;
  PlayerMessage emptyFormat;

  Order order = Order.DESCENDING;
  IntRange filter;

  int maxEntries = DEFAULT_MAX_SIZE;
  boolean fillMissingSlots = false;
  boolean includeYou = true;

  public BoardImpl(String name) {
    super(LEADERBOARD_KEY, name);
  }

  public static PlayerMessage makeTextFieldMessage(String text) {
    return new PlayerMessage(text, TEXT_FLAGS);
  }

  public void writeHover(TextWriter writer) {
    writer.field("State",
        isSpawned()
            ? text("spawned", NamedTextColor.GREEN)
            : text("inactive", NamedTextColor.GRAY)
    );

    writer.newLine();
    writer.newLine();

    if (source != null) {
      writer.field("Source", source.getKey());
    }

    if (location != null) {
      writer.formattedField("Location", "{0, location, -c -w}", location);
      writer.newLine();
      writer.newLine();
    }

    if (header != null) {
      writer.field("Header", editableTextFormat("header", header));
    }
    if (format != null) {
      writer.field("Format", editableTextFormat("format", format));
    }
    if (footer != null) {
      writer.field("Footer", editableTextFormat("footer", footer));
    }
    if (youFormat != null) {
      writer.field("You-format", editableTextFormat("you-format", youFormat));
    }
    if (emptyFormat != null) {
      writer.field("Empty-format", editableTextFormat("empty-format", emptyFormat));
    }

    writer.field("Include-you", includeYou);
    writer.field("Max Entries", maxEntries);
    writer.field("Fill empty slots", fillMissingSlots);

    writer.field("Order", Text.prettyEnumName(order));

    if (filter != null) {
      writer.field("Filter", filter);
    }
  }

  public void setOrder(@NotNull Order order) {
    Objects.requireNonNull(order, "Null order");
    this.order = order;
  }

  @Override
  public boolean fillMissingSlots() {
    return fillMissingSlots;
  }

  public void copyFrom(BoardImpl board) {
    if (board.header != null) {
      this.header = board.header;
    }
    if (board.footer != null) {
      this.footer = board.footer;
    }
    if (board.format != null) {
      this.format = board.format;
    }
    if (board.youFormat != null) {
      this.youFormat = board.youFormat;
    }
    if (board.emptyFormat != null) {
      this.emptyFormat = board.emptyFormat;
    }

    if (board.maxEntries != DEFAULT_MAX_SIZE) {
      this.maxEntries = board.maxEntries;
    }

    this.fillMissingSlots = board.fillMissingSlots;
    this.includeYou = board.includeYou;

    this.displayMeta.copyFrom(board.displayMeta);
  }

  private Component renderPlaceholders(Component component, Audience viewer) {
    PlaceholderRenderer renderer = Placeholders.newRenderer().useDefaults();
    return renderer.render(component.replaceText(NEW_LINE_REPLACER), viewer);
  }

  @Override
  public Component renderText(@Nullable Audience viewer) {
    var builder = text();
    if (header != null) {
      builder.append(renderPlaceholders(header.create(viewer), viewer));
    }

    List<LeaderboardScore> entries = getSortedScores();
    int end = fillMissingSlots ? this.maxEntries : Math.min(this.maxEntries, entries.size());

    if (end == -1) {
      end = entries.size();
    }

    boolean viewerWasShown = false;
    User viewingUser = Audiences.getUser(viewer);

    for (int i = 0; i < end; i++) {
      if (i >= entries.size()) {
        PlaceholderRenderer renderer = Placeholders.newRenderer();
        renderer.useDefaults();
        renderer.add("index", i+1);
        renderer.add("entry", "-");
        renderer.add("score", "-");
        renderer.add("score.raw", "-");
        renderer.add("score.timer", "-");

        Component line = renderLine(viewer, i, null, null, null, emptyFormat);
        builder.appendNewline().append(line);

        continue;
      }

      LeaderboardScore score = entries.get(i);
      UUID playerId = score.playerId();
      int value = score.value();

      if (viewingUser != null && !viewerWasShown) {
        viewerWasShown = Objects.equals(viewingUser.getUniqueId(), playerId);
      }

      Component line = renderLine(viewer, i, score.displayName(viewer), value, playerId, null);
      builder.appendNewline().append(line);
    }

    int vIndex = findViewerIndex(viewingUser, entries);
    if (!viewerWasShown && vIndex != -1 && viewingUser != null && includeYou) {
      source.getValue().getScore(viewingUser.getUniqueId())
          .ifPresent(value -> {
            if (filter != null && !filter.contains(value)) {
              return;
            }

            Component line = renderLine(
                viewer,
                vIndex,
                Messages.renderText("leaderboards.you", viewer),
                value,
                viewingUser.getUniqueId(),
                youFormat
            );

            builder.appendNewline().append(line);
          });
    }

    if (footer != null) {
      builder.appendNewline().append(renderPlaceholders(footer.create(viewer), viewer));
    }

    return builder.build();
  }

  private int findViewerIndex(User user, List<LeaderboardScore> scores) {
    if (user == null) {
      return -1;
    }

    for (int i = 0; i < scores.size(); i++) {
      var score = scores.get(i);

      if (Objects.equals(user.getUniqueId(), score.playerId())) {
        return i;
      }
    }

    return -1;
  }

  private Component renderLine(
      Audience viewer,
      int index,
      Component displayName,
      Integer value,
      UUID playerId,
      PlayerMessage formatBase
  ) {
    String timerScore = value == null ? "-" : getTimerCounter(value);

    PlaceholderRenderer renderer = Placeholders.newRenderer();
    renderer.useDefaults();
    renderer.add("index", index+1);
    renderer.add("entry", displayName == null ? text("-") : displayName);
    renderer.add("score", value == null ? text("-") : Text.formatNumber(value));
    renderer.add("score.raw", value);
    renderer.add("score.timer", timerScore);

    Map<String, Object> ctx = new HashMap<>();
    ctx.put("index", index);
    ctx.put("score", value);
    ctx.put("timerScore", timerScore);
    ctx.put("playerId", playerId);

    Component format;

    if (formatBase == null) {
      if (this.format == null) {
        format = DEFAULT_FORMAT;
      } else {
        format = this.format.create(viewer);
      }
    } else {
      format = formatBase.create(viewer);
    }

    return renderer.render(format.replaceText(NEW_LINE_REPLACER), viewer, ctx);
  }

  public static String getTimerCounter(long millis) {
    long minutes      = (millis / 60000);
    long seconds      = (millis /  1000) %  60;
    long milliseconds = (millis /    10) % 100;

    String prefix = "";

    if (minutes >= 60) {
      double hours = Math.floor((double) minutes / 60);
      prefix = String.format("%02.0f:", hours);
      minutes = minutes % 60;
    }

    return prefix + String.format("%02d:%02d.%02d", minutes, seconds, milliseconds);
  }

  private List<LeaderboardScore> getSortedScores() {
    if (source == null) {
      return List.of();
    }

    List<LeaderboardScore> list = new ArrayList<>(source.getValue().getScores());

    Comparator<LeaderboardScore> comparator = getComparator();
    list.sort(comparator);

    if (filter != null) {
      list.removeIf(leaderboardScore -> {
        int v = leaderboardScore.value();
        return !filter.contains(v);
      });
    }

    return list;
  }

  Comparator<LeaderboardScore> getComparator() {
    return switch (order) {
      case ASCENDING -> ASCENDING_COMPARATOR;
      case DESCENDING -> DESCENDING_COMPARATOR;
    };
  }
}
