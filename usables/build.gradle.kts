plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":scripting"))
}

pluginYml {
  name = "Usables"
  main = "net.arcadiusmc.usables.UsablesPlugin"

  loadAfter {
    regular("project:guilds")
  }
}