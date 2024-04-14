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
  prefixedName("Kingmaker")
  main = "net.arcadiusmc.kingmaker.KingmakerPlugin"
}