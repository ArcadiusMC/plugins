plugins {
  java
}

repositories {
  mavenCentral()
  maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
  compileOnly(project(":commons"))
  testImplementation(project(":commons"))
  //compileOnly(project(":cosmetics"))
  compileOnly(project(":punishments"))
  compileOnly(project(":menus"))
}

pluginYml {
  name = "Waypoints"
  main = "net.arcadiusmc.waypoints.WaypointsPlugin"

  loadAfter {
    regular("project:vanilla-hook")
  }
}