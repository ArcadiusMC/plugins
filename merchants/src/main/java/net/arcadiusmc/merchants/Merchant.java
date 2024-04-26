package net.arcadiusmc.merchants;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import lombok.Getter;
import net.arcadiusmc.Loggers;
import net.arcadiusmc.menu.Menu;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import net.forthecrown.nbt.CompoundTag;
import org.slf4j.Logger;

public abstract class Merchant {

  private static final Logger LOGGER = Loggers.getLogger();

  @Getter
  protected Menu menu;

  protected final MerchantsPlugin plugin;
  protected final Path dataFile;
  protected final Path configFile;

  public Merchant(MerchantsPlugin plugin, String filePrefix) {
    this.plugin = plugin;

    Path dir = plugin.getDataFolder().toPath();
    dataFile = dir.resolve(filePrefix + "-data.dat");
    configFile = dir.resolve(filePrefix + ".yml");
  }

  public final void load() {
    clearData();
    SerializationHelper.readTagFile(dataFile, this::loadDataFrom);
  }

  public final void save() {
    SerializationHelper.writeTagFile(dataFile, this::saveDataTo);
  }

  public abstract void reloadConfig();

  protected final <T> boolean loadConfig(Codec<T> codec, Consumer<T> consumer) {
    PluginJar.saveResources(configFile.getFileName().toString(), configFile);

    return SerializationHelper.readAsJson(configFile, object -> {
      codec.parse(JsonOps.INSTANCE, object)
          .mapError(s -> "Failed to load config " + configFile + ": " + s)
          .resultOrPartial(LOGGER::error)
          .ifPresent(consumer);
    });
  }

  protected void clearData() {

  }

  protected abstract void loadDataFrom(CompoundTag tag);

  protected abstract void saveDataTo(CompoundTag tag);

  public void onDayChange(ZonedDateTime time) {

  }

  protected void onEnable() {

  }
}
