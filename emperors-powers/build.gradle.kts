plugins {
  java
  kotlin("jvm") version "2.0.0"
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":menus"))
  compileOnly(project(":player-markets"))
  implementation(kotlin("stdlib"))
}

pluginYml {
  prefixedName("Emperors-Powers")
  main = "net.arcadiusmc.emperor.EmperorPlugin"

  depends {
    required("MCKotlin-Paper")
  }
}