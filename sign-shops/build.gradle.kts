plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":menus"))
}

pluginYml {
  name = "SignShops"
  main = "net.arcadiusmc.signshops.SignShopsPlugin"
}