plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":sign-shops"))
  compileOnly(project(":mail"))
  compileOnly(project(":menus"))
}

pluginYml {
  name = "Player-Markets"
  main = "net.arcadiusmc.markets.MarketsPlugin"
}