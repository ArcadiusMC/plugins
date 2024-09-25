plugins {
  java
  kotlin("jvm") version "2.0.0"
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  implementation(kotlin("stdlib"))
}

pluginYml {
  prefixedName("PiratesQuests")
  main = "net.arcadiusmc.pirates.PiratesQuestPlugin"

  depends {
    required("MCKotlin-Paper")
  }
}