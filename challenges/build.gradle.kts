plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":scripting"))
  compileOnly(project(":menus"))
  compileOnly(project(":sell-shop"))
}

pluginYml {
  name = "Challenges"
  main = "net.arcadiusmc.challenges.ChallengesPlugin"

  openClassLoader = true

  depends {
    optional("VotingPlugin")
    optional("project:shops")
  }

  loadAfter {
    regular("project:leaderboards")
  }
}