plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":structures"))

  testImplementation(project(":structures"))
  testImplementation(project(":commons"))
}

pluginYml {
  prefixedName("Dungeons")
  main = "net.arcadiusmc.dungeons.DungeonsPlugin"
}