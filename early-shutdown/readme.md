# Early Shutdown

Calls the `EarlyShutdownEvent`. Gradle build script set this module's plugin to
be loaded after all other plugins, so if `onDisable` is called fro this plugin,
it means its the first plugin being disabled in the load order, meaning no other
plugins have yet to be disabled.
  
This allows it to fire the early shutdown event that allows for everything to
be successfully disabled without missing plugin dependencies.