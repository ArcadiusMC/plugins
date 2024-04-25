package net.arcadiusmc.entity;

import com.badlogic.ashley.core.Engine;
import net.arcadiusmc.entity.commands.CommandCustomEntity;
import net.arcadiusmc.entity.dungeons.GuardianBeam;
import net.arcadiusmc.entity.dungeons.ShulkerGuardian;
import net.arcadiusmc.entity.system.HandleSystem;
import net.arcadiusmc.entity.system.IdSystem;
import net.arcadiusmc.utils.Tasks;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class EntityPlugin extends JavaPlugin {

  private Engine engine;
  private BukkitTask ticker;

  @Override
  public void onEnable() {
    engine = new Engine();
    Entities.engine = engine;

    engine.addSystem(new IdSystem());
    engine.addSystem(new HandleSystem());
    engine.addSystem(new ShulkerGuardian());
    engine.addSystem(new GuardianBeam());

    EntityTemplates.registerAll();

    new CommandCustomEntity();

    BukkitScheduler scheduler = getServer().getScheduler();
    ticker = scheduler.runTaskTimer(this, new TickTask(engine), 1, 1);
  }

  @Override
  public void onDisable() {
    ticker = Tasks.cancel(ticker);
    Entities.engine = null;
  }
}

class TickTask implements Runnable {

  static final float NANOS_PER_SECOND = 1e-9f;

  float lastExec;
  float execStart;
  float deltaTime;

  final Engine engine;

  public TickTask(Engine engine) {
    this.engine = engine;
  }

  @Override
  public void run() {
    execStart = System.nanoTime();

    if (lastExec == 0) {
      deltaTime = 1;
    } else {
      deltaTime = (execStart - lastExec) / NANOS_PER_SECOND;
    }

    engine.update(deltaTime);
    lastExec = execStart;
  }
}
