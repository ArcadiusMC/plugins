package net.arcadiusmc.core;

import java.nio.file.Path;
import java.util.Objects;
import net.arcadiusmc.ArcadiusServer;
import net.arcadiusmc.Worlds;
import net.arcadiusmc.command.settings.SettingsBook;
import net.arcadiusmc.core.commands.CommandLeave;
import net.arcadiusmc.text.ViewerAwareMessage;
import net.arcadiusmc.text.channel.ChannelledMessage;
import net.arcadiusmc.text.channel.MessageRenderer;
import net.arcadiusmc.text.placeholder.Placeholders;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.io.JsonUtils;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArcadiusServerImpl implements ArcadiusServer {

  private Location serverSpawn;

  private final SettingsBook<User> globalSettings = new SettingsBook<>();
  private final CorePlugin plugin;

  public ArcadiusServerImpl(CorePlugin plugin) {
    this.plugin = plugin;
  }

  private Path spawnJson() {
    return PathUtil.pluginPath("spawn.json");
  }

  public void load() {
    SerializationHelper.readAsJson(spawnJson(), json -> {
      Location loc = JsonUtils.readLocation(json);
      setServerSpawn(loc);
    });
  }

  public void save() {
    SerializationHelper.writeJsonFile(spawnJson(), jsonWrapper -> {
      var json = JsonUtils.writeLocation(getServerSpawn());
      jsonWrapper.addAll(json);
    });
  }

  @Override
  public @NotNull Location getServerSpawn() {
    if (serverSpawn == null) {
      World overworld = Worlds.overworld();
      return overworld.getSpawnLocation();
    }

    return serverSpawn.clone();
  }

  @Override
  public void setServerSpawn(@NotNull Location serverSpawn) {
    Objects.requireNonNull(serverSpawn);
    this.serverSpawn = serverSpawn.clone();
  }

  @Override
  public @NotNull SettingsBook<User> getGlobalSettingsBook() {
    return globalSettings;
  }

  @Override
  public MessageRenderer getAnnouncementRenderer() {
    return plugin.getAnnouncer().renderer(Placeholders.newRenderer().useDefaults());
  }

  @Override
  public void announce(ViewerAwareMessage message) {
    ChannelledMessage.create(message)
        .setBroadcast()
        .setRenderer(plugin.getAnnouncer().renderer(Placeholders.newRenderer().useDefaults()))
        .send();
  }

  @Override
  public void registerLeaveListener(String id, LeaveCommandListener listener) {
    Objects.requireNonNull(id, "Null ID");
    Objects.requireNonNull(listener, "Null listener");
    CommandLeave.listeners.put(id, listener);
  }

  @Override
  public void unregisterLeaveListener(String id) {
    Objects.requireNonNull(id, "Null ID");
    CommandLeave.listeners.remove(id);
  }

  @Override
  public void spawnCoinPile(Location location, int value, CoinPileSize pileSize) {
    Objects.requireNonNull(pileSize, "Null pile size");

    int modelId = switch (pileSize) {
      case LARGE -> Coinpile.MODEL_LARGE;
      case MEDIUM -> Coinpile.MODEL_MEDIUM;
      case SMALL -> Coinpile.MODEL_SMALL;
    };

    Coinpile.create(location, value, modelId);
  }

  @Override
  public boolean isCoinPile(@Nullable Entity entity) {
    if (entity == null) {
      return false;
    }

    return entity.getScoreboardTags().contains(Coinpile.SCOREBOARD_TAG);
  }
}