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
  name = "FTC-ServerList"
  main = "net.arcadiusmc.serverlist.ServerlistPlugin"
}