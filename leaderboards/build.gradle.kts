plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
}

pluginYml {
  name = "Leaderboards"
  main = "net.arcadiusmc.leaderboards.LeaderboardPlugin"

  loadAfter {
    regular("project:vanilla-hook")
  }
}