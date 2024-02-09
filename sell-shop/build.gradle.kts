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
  name = "SellShop"
  main = "net.arcadiusmc.sellshop.SellShopPlugin"
}