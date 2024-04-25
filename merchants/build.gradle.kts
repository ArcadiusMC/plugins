plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":menus"))
}

pluginYml {
  prefixedName("Merchants")
  main = "net.arcadiusmc.merchants.MerchantsPlugin"
}