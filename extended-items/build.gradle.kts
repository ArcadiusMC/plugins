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
  prefixedName("Items")
  main = "net.arcadiusmc.items.ItemPlugin"
}