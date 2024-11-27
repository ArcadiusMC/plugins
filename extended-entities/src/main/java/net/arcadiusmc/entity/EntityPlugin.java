package net.arcadiusmc.entity;

import com.badlogic.ashley.core.Engine;
import lombok.Getter;
import net.arcadiusmc.entity.commands.CommandCustomEntity;
import net.arcadiusmc.entity.dungeons.GuardianBeam;
import net.arcadiusmc.entity.dungeons.ShulkerGuardian;
import net.arcadiusmc.entity.persistence.PersistentTypes;
import net.arcadiusmc.entity.system.HandleSystem;
import net.arcadiusmc.entity.system.IdSystem;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

@Getter
public class EntityPlugin extends JavaPlugin {

  private Engine engine;
  private BukkitTask ticker;
  private EntityStorage storage;

  private boolean loadFailed = false;

  @Override
  public void onEnable() {
    engine = new Engine();
    storage = new EntityStorage(this);

    Entities.engine = engine;
    PersistentTypes.registerAll();

    engine.addSystem(new IdSystem());
    engine.addSystem(new HandleSystem());
    engine.addSystem(new ShulkerGuardian());
    engine.addSystem(new GuardianBeam());

    EntityTemplates.registerAll();

    new CommandCustomEntity(this);

    BukkitScheduler scheduler = getServer().getScheduler();
    ticker = scheduler.runTaskTimer(this, new TickTask(engine), 1, 1);

    try {
      storage.loadEntities(engine);
      loadFailed = false;
    } catch (Throwable t) {
      loadFailed = true;
      throw t;
    }
  }

  @Override
  public void onDisable() {
    ticker = Tasks.cancel(ticker);

    if (!loadFailed) {
      storage.saveEntities(engine);
    }

    Entities.engine = null;
  }
}

class TickTask implements Runnable {

  final Server server;
  final Engine engine;

  public TickTask(Engine engine) {
    this.engine = engine;
    this.server = Bukkit.getServer();
  }

  @Override
  public void run() {
    float rate = server.getServerTickManager().getTickRate();
    float deltaTime = (1.0f / rate) * Entities.timeScale;

    engine.update(deltaTime);
  }
}
