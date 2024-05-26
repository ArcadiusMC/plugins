package net.arcadiusmc.scripts.builtins;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;

public enum TimeFunction implements ProxyExecutable {
  MILLIS (1),
  SECONDS (1000);

  private final double multiplier;

  TimeFunction(double divider) {
    this.multiplier = 1 / divider;
  }

  @Override
  public Object execute(Value... arguments) {
    double time = System.currentTimeMillis();
    return time * multiplier;
  }
}
