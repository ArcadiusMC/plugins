package net.arcadiusmc.core.tab;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.core.CorePlugin;
import net.arcadiusmc.core.tab.TabConfig.DynamicBorderConfig;
import net.arcadiusmc.text.Text;
import net.arcadiusmc.text.TextInfo;
import net.arcadiusmc.text.placeholder.PlaceholderRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.user.NameRenderFlags;
import net.arcadiusmc.user.User;
import net.arcadiusmc.user.Users;
import net.arcadiusmc.user.name.DisplayIntent;
import net.arcadiusmc.utils.Tasks;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.slf4j.Logger;

public class TabMenu {

  static final int DIR_FORWARD = 1;
  static final int DIR_BACKWARD = -1;

  private static final Logger LOGGER = Loggers.getLogger();

  private TabConfig config;

  private final CorePlugin plugin;
  private final Path path;

  private BukkitTask updateTask;
  private int frameIndex;
  private int frameDirection = DIR_FORWARD;

  private TabText lastDisplayed = TabText.EMPTY;

  public TabMenu(CorePlugin plugin) {
    this.plugin = plugin;
    this.path = PathUtil.pluginPath(plugin, "tab.yml");
    this.config = TabConfig.defaultConfig();
  }

  public void load() {
    PluginJar.saveResources(plugin, "tab.yml", path);
    SerializationHelper.readAsJson(path, wrapper -> loadFrom(wrapper.getSource()));
  }

  private void loadFrom(JsonObject obj) {
    TabConfig.CODEC.parse(JsonOps.INSTANCE, obj)
        .resultOrPartial(LOGGER::error)
        .ifPresent(config -> {
          this.config = config;
          displayTab(config.base());
          startAnimation();
        });
  }

  public void startAnimation() {
    closeAnimation();

    if (config == null || !config.isAnimated()) {
      return;
    }

    Duration speed = config.animationSpeed();

    // Direction and index are reset in killTask()
    this.updateTask = Tasks.runTimer(this::displayNextFrame, speed, speed);
  }

  public void closeAnimation() {
    updateTask = Tasks.cancel(updateTask);
    frameIndex = 0;
    frameDirection = DIR_FORWARD;
  }

  private void displayNextFrame() {
    if (config == null) {
      return;
    }

    List<TabText> frames = config.frames();

    if (frames == null || frames.isEmpty()) {
      return;
    }

    if (!Bukkit.getOnlinePlayers().isEmpty()) {
      TabText text = frames.get(frameIndex);
      displayTab(text);
    }

    int newIndex = frameIndex + frameDirection;

    if (newIndex >= frames.size()) {
      if (config.reverseAnimationAtEnd()) {
        frameDirection = DIR_BACKWARD;
      } else {
        frameIndex = 0;
        frameDirection = DIR_FORWARD;
      }

      return;
    }

    if (newIndex < 0) {
      frameIndex = 0;
      frameDirection = DIR_FORWARD;
      return;
    }

    frameIndex = newIndex;
  }

  public void update() {
    displayTab(lastDisplayed);
  }

  private void internalShowUser(User user, TabText styled, PlaceholderRenderer renderer) {
    DynamicBorderConfig config = getBorderConfig();
    int borderWidth = findBorderWidth(user, config);

    renderer.add("tabBorder",         new BorderPlaceholder(borderWidth    , config));
    renderer.add("tabBorder.half",    new BorderPlaceholder(borderWidth / 2, config));
    renderer.add("tabBorder.third",   new BorderPlaceholder(borderWidth / 3, config));
    renderer.add("tabBorder.quarter", new BorderPlaceholder(borderWidth / 4, config));

    var displayObj = Bukkit.getScoreboardManager()
        .getMainScoreboard()
        .getObjective(DisplaySlot.PLAYER_LIST);

    if (displayObj != null) {
      renderer.add("score", displayObj.displayName());
    } else {
      renderer.add("score", "none");
    }

    Component header = renderer.render(styled.header(), user);
    Component footer = renderer.render(styled.footer(), user);

    user.sendPlayerListHeaderAndFooter(header, footer);
  }

  private void displayTab(TabText text) {
    Objects.requireNonNull(text, "Null tab text");

    this.lastDisplayed = text;

    TabText styled = addScoreText(text);
    PlaceholderRenderer renderer = Placeholders.newRenderer().useDefaults();

    for (User user : Users.getOnline()) {
      internalShowUser(user, styled, renderer);
    }
  }

  private TabText addScoreText(TabText text) {
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    Objective displayObjective = scoreboard.getObjective(DisplaySlot.PLAYER_LIST);

    if (displayObjective == null) {
      return text;
    }

    TabText result = text;

    if (config.scorePrepend() != null) {
      TabText prepend = config.scorePrepend();
      result = prepend.combine(result);
    }

    if (config.scoreAppend() != null) {
      result = result.combine(config.scoreAppend());
    }

    return result;
  }

  private int measureScore(Score score) {
    Objective objective = score.getObjective();

    if (objective.getRenderType() == RenderType.HEARTS) {
      return getBorderConfig().heartsWidth();
    }

    // TODO: When number format API is added, refactor this
    // At time of writing (27 January 2024) there's no Spigot
    // or PaperAPI for the number formats that objectives now
    // use, as such, we have to basically hope that any measured
    // score value is even remotely accurate

    int scoreValue = score.getScore();
    String str = String.valueOf(scoreValue);

    return TextInfo.getPxWidth(str);
  }

  private DynamicBorderConfig getBorderConfig() {
    if (config == null || config.borderConfig() == null) {
      return DynamicBorderConfig.DEFAULT;
    } else {
      return config.borderConfig();
    }
  }

  private int findBorderWidth(User viewer, DynamicBorderConfig borderConfig) {
    int largestPlayerName = 0;

    Objective displayObj = Bukkit.getScoreboardManager()
        .getMainScoreboard()
        .getObjective(DisplaySlot.PLAYER_LIST);

    for (User u : Users.getOnline()) {
      int playerNameSize = 0;

      // Don't measure users if they're vanished or otherwise hidden
      if (!viewer.canSee(u)) {
        continue;
      }

      if (displayObj != null) {
        Score score = displayObj.getScore(u.getPlayer());
        playerNameSize += measureScore(score);
      }

      Component displayName = u.displayName(
          null,
          EnumSet.allOf(NameRenderFlags.class),
          DisplayIntent.TABLIST
      );

      playerNameSize += TextInfo.getPxWidth(Text.plain(displayName));
      largestPlayerName = Math.max(largestPlayerName, playerNameSize);
    }

    // ensure it doesn't drop below the minimum width
    return Math.max(
        borderConfig.extraPixels() + largestPlayerName + borderConfig.overReach(),
        borderConfig.minWidth()
    );
  }
}
