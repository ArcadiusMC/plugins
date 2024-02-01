plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":scripting"))
}

pluginYml {
  prefixedName("CustomItems")
  main = "net.arcadiusmc.customitems.ItemPlugin"
}