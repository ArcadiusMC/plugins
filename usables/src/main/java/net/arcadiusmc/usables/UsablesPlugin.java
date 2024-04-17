package net.arcadiusmc.usables;

import java.time.Duration;
import lombok.Getter;
import net.arcadiusmc.registry.Registries;
import net.arcadiusmc.registry.Registry;
import net.arcadiusmc.usables.actions.Actions;
import net.arcadiusmc.usables.commands.UsablesCommands;
import net.arcadiusmc.usables.conditions.Conditions;
import net.arcadiusmc.usables.listeners.UsablesListeners;
import net.arcadiusmc.usables.objects.Kit;
import net.arcadiusmc.usables.objects.Warp;
import net.arcadiusmc.usables.trigger.TriggerManager;
import net.arcadiusmc.usables.virtual.VirtualUsableManager;
import net.arcadiusmc.utils.PeriodicalSaver;
import net.arcadiusmc.utils.TomlConfigs;
import net.arcadiusmc.utils.io.PathUtil;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class UsablesPlugin extends JavaPlugin {

  private final Registry<ObjectType<? extends Action>> actions = Registries.newFreezable();
  private final Registry<ObjectType<? extends Condition>> conditions = Registries.newFreezable();

  private CmdUsables<Warp> warps;
  private CmdUsables<Kit> kits;

  private TriggerManager triggers;
  private VirtualUsableManager virtuals;

  private PeriodicalSaver saver;

  private UsablesConfig usablesConfig;

  public static UsablesPlugin get() {
    return JavaPlugin.getPlugin(UsablesPlugin.class);
  }

  @Override
  public void onEnable() {
    reloadConfig();

    var dir = PathUtil.pluginPath();
    kits = new CmdUsables<>(dir.resolve("kits.dat"), Kit::new);
    warps = new CmdUsables<>(dir.resolve("warps.dat"), Warp::new);

    triggers = new TriggerManager(dir.resolve("triggers.dat"));
    virtuals = new VirtualUsableManager(dir.resolve("virtuals.dat"));

    UsablesListeners.registerAll(this);

    saver = PeriodicalSaver.create(this::save, () -> Duration.ofMinutes(30));
    saver.start();

    UsablesCommands.createCommands(this);
  }

  public void save() {
    warps.save();
    kits.save();
    triggers.save();
    virtuals.save();
  }

  @Override
  public void onDisable() {
    save();
    saver.stop();
  }

  public void reload() {
    reloadConfig();

    warps.load();
    kits.load();
    triggers.load();
    virtuals.load();
  }

  @Override
  public void reloadConfig() {
    usablesConfig = TomlConfigs.loadPluginConfig(this, UsablesConfig.class);
  }

  public void registerDefaults() {
    Actions.registerAll(actions);
    Conditions.registerAll(conditions);
  }

  public void freezeRegistries() {
    actions.freeze();
    conditions.freeze();

    virtuals.initialize();
    virtuals.lockSystems();
    virtuals.getTriggerTypes().freeze();
  }
}
