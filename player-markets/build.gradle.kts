plugins {
  java
}

repositories {

}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":sign-shops"))
  compileOnly(project(":mail"))
}

pluginYml {
  name = "Player-Markets"
  main = "net.arcadiusmc.markets.MarketsPlugin"
}