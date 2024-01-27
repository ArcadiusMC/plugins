plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
}

pluginYml {
  name = "Arcadius-Menus"
  main = "net.arcadiusmc.menu.internal.MenusPlugin"
}