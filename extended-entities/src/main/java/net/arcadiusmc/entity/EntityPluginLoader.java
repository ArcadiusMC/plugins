package net.arcadiusmc.entity;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import net.arcadiusmc.classloader.Libraries;
import org.jetbrains.annotations.NotNull;

public class EntityPluginLoader implements PluginLoader {

  @Override
  public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
    MavenLibraryResolver resolver = Libraries.loadResolver(getClass().getClassLoader());
    classpathBuilder.addLibrary(resolver);
  }
}
