dependencies {
  compileOnly(project(":commons"))
}

val thisProj = project;

pluginYml {
  main = "net.arcadiusmc.earlyshutdown.EarlyShutdownPlugin"
  name = "EarlyShutdown"

  loadAfter {
    val root = rootProject;
    root.subprojects {
      if (this.equals(thisProj)) {
        return@subprojects
      }

      regular("project:${this.name}")
    }
  }
}