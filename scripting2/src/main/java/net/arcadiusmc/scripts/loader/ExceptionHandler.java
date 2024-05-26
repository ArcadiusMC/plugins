package net.arcadiusmc.scripts.loader;

import java.lang.Thread.UncaughtExceptionHandler;
import net.arcadiusmc.Loggers;
import org.slf4j.Logger;

enum ExceptionHandler implements UncaughtExceptionHandler {
  HANDLER;

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    Logger logger = Loggers.getLogger(t.getName());
    logger.error("Error on file watcher thread {}!", t.getName(), e);
  }
}
