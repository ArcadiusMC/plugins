package net.arcadiusmc.scripts;

import com.google.gson.JsonElement;
import java.nio.file.Path;
import java.util.Map;
import lombok.Getter;
import net.arcadiusmc.scripts.commands.CommandJs;
import net.arcadiusmc.scripts.commands.ScriptingCommand;
import net.arcadiusmc.scripts.listeners.ScriptListeners;
import net.arcadiusmc.BukkitServices;
import net.arcadiusmc.command.Commands;
import net.arcadiusmc.text.Messages;
import net.arcadiusmc.text.loader.MessageList;
import net.arcadiusmc.text.loader.MessageLoader;
import net.forthecrown.grenadier.annotations.AnnotatedCommandContext;
import net.arcadiusmc.scripts.pack.PackManager;
import net.arcadiusmc.scripts.preprocessor.PreProcessor;
import net.arcadiusmc.utils.io.JsonWrapper;
import net.arcadiusmc.utils.io.PathUtil;
import net.arcadiusmc.utils.io.PluginJar;
import net.arcadiusmc.utils.io.SerializationHelper;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class ScriptingPlugin extends JavaPlugin {

  private final MessageList messageList = MessageList.create();

  private ScriptManager service;
  private PackManager packs;

  @Override
  public void onEnable() {
    Path pluginDir = PathUtil.pluginPath();
    Path scriptsDir = pluginDir.resolve("scripts");
    Path packsDir = pluginDir.resolve("packs");

    service = new ScriptManager(scriptsDir, this);
    packs = new PackManager(service, packsDir);

    Scripts.setService(service);
    BukkitServices.register(ScriptService.class, service);

    ScriptPlaceholders.registerAll();
    Messages.MESSAGE_LIST.addChild("scripts", messageList);

    // Commands
    AnnotatedCommandContext ctx = Commands.createAnnotationContext();
    ctx.registerCommand(new ScriptingCommand(this));
    new CommandJs();

    // Events
    ScriptListeners.registerAll(this);

    reload();
  }

  @Override
  public void onDisable() {
    if (packs != null) {
      packs.close();
    }

    if (service != null) {
      service.close();
    }

    ScriptPlaceholders.removeAll();
    Messages.MESSAGE_LIST.removeChild("scripts");
  }

  public void reload() {
    reloadConfig();
    packs.reload();
  }

  @Override
  public void reloadConfig() {
    PluginJar.saveResources(this, "config.toml");
    PreProcessor.setImportPlaceholders(null);

    SerializationHelper.readAsJson(
        getDataFolder().toPath().resolve("config.toml"),
        this::loadConfigFrom
    );

    MessageLoader.loadPluginMessages(this, messageList);
  }

  private void loadConfigFrom(JsonWrapper json) {
    if (json.has("importPlaceholders")) {
      Map<String, String> importPlaceholders = json.getMap(
          "importPlaceholders",
          s -> s,
          JsonElement::getAsString
      );

      PreProcessor.setImportPlaceholders(importPlaceholders);
    } else {
      PreProcessor.setImportPlaceholders(null);
    }
  }
}