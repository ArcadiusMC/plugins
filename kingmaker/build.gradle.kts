plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":usables"))
}

pluginYml {
  prefixedName("Kingmaker")
  main = "net.arcadiusmc.kingmaker.KingmakerPlugin"
}