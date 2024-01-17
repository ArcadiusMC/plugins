package net.arcadiusmc.scripts;

import java.nio.file.Path;
import java.util.List;
import net.arcadiusmc.scripts.module.ModuleManager;
import net.arcadiusmc.utils.io.source.Source;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public interface ScriptService extends ScriptLoader {

  Path getScriptsDirectory();

  CachingScriptLoader newLoader();

  CachingScriptLoader newLoader(Path workingDirectory);

  CachingScriptLoader getGlobalLoader();

  @NotNull
  Script newScript(ScriptLoader loader, @NotNull Source source);

  default Script newScript(@NotNull Source source) {
    return newScript(this, source);
  }

  Plugin getScriptPlugin();

  List<Class<?>> getAutoImportedClasses();

  void addAutoImportedClass(Class<?> clazz);

  ModuleManager getModules();
}