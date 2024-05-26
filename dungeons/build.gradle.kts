plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":scripting"))
  compileOnly(project(":structures"))
}

pluginYml {
  prefixedName("Dungeons")
  main = "net.arcadiusmc.dungeons.DungeonsPlugin"
}