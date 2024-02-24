plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
}

pluginYml {
  name = "Arcadius-Holograms"
  main = "net.arcadiusmc.holograms.HologramPlugin"

  loadAfter {
    regular("project:vanilla-hook")
  }
}