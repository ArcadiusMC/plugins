plugins {
  java
}

repositories {
  mavenCentral()
  maven("https://mvn.lumine.io/repository/maven-public/")

  // WorldGuard
  maven("https://maven.enginehub.org/repo/")
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":cosmetics"))
  compileOnly(project(":anti-grief"))
  compileOnly(project(":menus"))

  compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.7")
}

pluginYml {
  name = "Waypoints"
  main = "net.arcadiusmc.waypoints.WaypointsPlugin"

  depends {
    required("dynmap")
  }

  loadAfter {
    regular("project:vanilla-hook")
  }
}