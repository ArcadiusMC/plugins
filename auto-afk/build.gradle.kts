plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":punishments"))
}

pluginYml {
  name = "AutoAfk"
  main = "net.arcadiusmc.afk.AfkPlugin"
}