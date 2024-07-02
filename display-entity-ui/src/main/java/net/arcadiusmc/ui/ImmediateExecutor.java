package net.arcadiusmc.ui;

import java.util.concurrent.Executor;
import net.arcadiusmc.Loggers;
import org.jetbrains.annotations.NotNull;

public enum ImmediateExecutor implements Executor {
  EXECUTOR;

  @Override
  public void execute(@NotNull Runnable command) {
    try {
      command.run();
    } catch (Throwable t) {
      Loggers.getLogger().error("Error executing event task", t);
    }
  }
}
