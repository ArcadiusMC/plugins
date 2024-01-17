package net.arcadiusmc.scripts.module;

import net.arcadiusmc.scripts.Script;
import net.arcadiusmc.scripts.ScriptObject;

public class ScriptModule extends ScriptableModule {

  private final Script script;

  public ScriptModule(Script script) {
    super(new ScriptObject(script));
    this.script = script;
  }

  @Override
  public void close() {
    script.close();
  }
}
