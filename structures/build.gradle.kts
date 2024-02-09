plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
}

pluginYml {
  name = "Structures"
  main = "net.arcadiusmc.structure.StructuresPlugin"
}