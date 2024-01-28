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
  name = "ServerList"
  main = "net.arcadiusmc.serverlist.ServerlistPlugin"
}