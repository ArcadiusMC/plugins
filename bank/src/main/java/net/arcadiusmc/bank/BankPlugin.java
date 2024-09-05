package net.arcadiusmc.bank;

import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class BankPlugin extends JavaPlugin {

  static final int ENTER_SUCCESS = 0;
  static final int ENTER_ALREADY_IN_VAULT = 1;
  static final int ENTER_UNKNOWN_WORLD = 2;

  private final Map<String, BankVault> vaultMap = new HashMap<>();
  private final Map<UUID, BankRun> sessionMap = new HashMap<>();

  private VaultDebug debug;

  @Override
  public void onEnable() {
    debug = new VaultDebug(this);

    reloadConfig();
    new CommandBankVault(this);
    Events.register(new BankListener(this));
  }

  @Override
  public void onDisable() {
    stopAllRuns();
  }

  public void stopAllRuns() {
    if (sessionMap.isEmpty()) {
      return;
    }

    List<BankRun> runs = new ArrayList<>(sessionMap.values());
    for (BankRun run : runs) {
      run.kick(true);
    }
  }

  @Override
  public void reloadConfig() {
    stopAllRuns();

    MessageLoader.loadPluginMessages(this);

    vaultMap.clear();

    Path dir = getDataPath().resolve("vaults");
    PluginJar.saveResources("vaults", dir);

    PathUtil.iterateDirectory(dir, true, true, path -> {
      JsonObject object = SerializationHelper.readAsJson(path);
      String key = PathUtil.getFileKey(dir, path);

      BankVault.CODEC.parse(JsonOps.INSTANCE, object)
          .mapError(s -> "Failed to load vault " + key + ": " + s)
          .resultOrPartial(getSLF4JLogger()::error)
          .ifPresent(bankVault -> {
            if (key.equals("example")) {
              return;
            }

            vaultMap.put(key, bankVault);

            BankVault.CODEC.encodeStart(JsonOps.INSTANCE, bankVault)
                .resultOrPartial()
                .ifPresent(element -> {
                  StringWriter writer = new StringWriter();
                  JsonWriter wr = new JsonWriter(writer);
                  wr.setLenient(true);
                  wr.setIndent("  ");

                  try {
                    TypeAdapters.JSON_ELEMENT.write(wr, element);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }

                  if (debug.isTicking()) {
                    getSLF4JLogger().info("Loaded bank, debug info: {}", writer);
                  }
                });
          });
    });
  }

  public int startRun(User user, BankVault vault, String vaultKey, String variant) {
    BankRun run = sessionMap.get(user.getUniqueId());
    if (run != null) {
      return ENTER_ALREADY_IN_VAULT;
    }

    World world = Bukkit.getWorld(vault.getWorldName());
    if (world == null) {
      return ENTER_UNKNOWN_WORLD;
    }

    run = new BankRun(this, user.getPlayer(), world);
    run.setVault(vault);
    run.setVaultKey(vaultKey);
    run.setVariant(variant);

    sessionMap.put(user.getUniqueId(), run);
    run.start();

    return ENTER_SUCCESS;
  }
}
