package net.arcadiusmc.structure;

import java.time.Duration;
import lombok.Getter;
import net.arcadiusmc.structure.commands.StructureCommands;
import net.arcadiusmc.text.loader.MessageLoader;
import net.arcadiusmc.utils.PeriodicalSaver;
import org.bukkit.plugin.java.JavaPlugin;

public class StructuresPlugin extends JavaPlugin {

  private PeriodicalSaver saver;

  @Getter
  private Structures structures;

  public static StructuresPlugin getPlugin() {
    return JavaPlugin.getPlugin(StructuresPlugin.class);
  }

  public static Structures getManager() {
    return getPlugin().getStructures();
  }

  @Override
  public void onEnable() {
    StructPluginUpdater.run(this);

    structures = new Structures(this);
    structures.load();

    reloadConfig();

    StructureCommands.createCommands(this);

    saver = PeriodicalSaver.create(this::save, () -> Duration.ofMinutes(30));
    saver.start();
  }

  @Override
  public void onDisable() {
    structures.save();
    saver.stop();
  }

  void save() {
    structures.save();
  }

  @Override
  public void reloadConfig() {
    MessageLoader.loadPluginMessages(this);
  }
}