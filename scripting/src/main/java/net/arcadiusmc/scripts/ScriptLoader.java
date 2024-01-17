package net.arcadiusmc.scripts;

import net.arcadiusmc.utils.Result;
import net.arcadiusmc.utils.io.source.Source;

public interface ScriptLoader {

  Script loadScript(Source source);

  default Result<Script> loadCompiled(Source source) {
    Script script = loadScript(source);

    if (script.isCompiled()) {
      return Result.success(script);
    }

    try {
      script.compile();
    } catch (ScriptLoadException exc) {
      return Result.error("Couldn't load script %s: %s", source, exc.getMessage());
    }

    return script.evaluate().toRegularResult().map(o -> script);
  }
}
