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
  name = "Arcadius-DisplayEntityMenus"
  main = "net.arcadiusmc.ui.UiPlugin"
}