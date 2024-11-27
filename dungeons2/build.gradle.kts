plugins {
  java
  kotlin("jvm") version "2.0.0"
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":structures"))

  testImplementation(project(":structures"))
  testImplementation(project(":commons"))

  implementation(kotlin("stdlib"))
}

pluginYml {
  prefixedName("Dungeons")
  main = "net.arcadiusmc.dungeons.DungeonsPlugin"
}