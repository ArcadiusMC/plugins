package net.arcadiusmc.scripts;

import net.arcadiusmc.utils.io.source.Source;

public interface CachingScriptLoader extends ScriptLoader {

  Script remove(Source source);

  void close();
}
