package net.arcadiusmc.bank;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import net.arcadiusmc.delphi.Delphi;
import net.arcadiusmc.delphi.DelphiProvider;
import net.arcadiusmc.delphi.DocumentView;
import net.arcadiusmc.delphi.resource.JarResourceModule;
import net.arcadiusmc.events.Events;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.user.User;
import net.arcadiusmc.utils.PluginUtil;
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

  public static BankPlugin getPlugin() {
    return JavaPlugin.getPlugin(BankPlugin.class);
  }

  @Override
  public void onEnable() {
    debug = new VaultDebug(this);

    reloadConfig();
    new CommandBankVault(this);
    Events.register(new BankListener(this));

    if (PluginUtil.isEnabled("Delphi")) {
      JarResourceModule module = new JarResourceModule(getClassLoader(), "pages");
      module.setFilePaths(List.of("exit.xml", "enter.xml"));

      Delphi delphi = DelphiProvider.get();
      delphi.getResources().registerModule("bank-page", module);
    }
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

    killOpenPages();
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

  private void killOpenPages() {
    if (!PluginUtil.isEnabled("Delphi")) {
      return;
    }

    Delphi delphi = DelphiProvider.get();

    delphi.getAllViews()
        .stream()
        .filter(view -> view.getPath().toString().startsWith("bank-page:enter.xml"))
        .forEach(DocumentView::close);
  }
}
