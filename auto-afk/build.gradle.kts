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
  name = "AutoAfk"
  main = "net.arcadiusmc.afk.AfkPlugin"
}