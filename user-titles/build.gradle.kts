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
  main = "net.arcadiusmc.titles.TitlesPlugin"
  name = "Ranks"
}