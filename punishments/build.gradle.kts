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
  name = "Punishments"
  main = "net.arcadiusmc.punish.PunishPlugin"
}