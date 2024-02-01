plugins {
  java
}

version = "1.0.0"

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