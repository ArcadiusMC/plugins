plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":scripting"))
}

pluginYml {
  prefixedName("Dungeons")
  main = "net.arcadiusmc.dungeons.DungeonsPlugin"
}