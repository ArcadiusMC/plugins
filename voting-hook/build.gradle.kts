plugins {
  java
}

repositories {
  mavenCentral()
  maven("https://nexus.bencodez.com/repository/maven-public")
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":core"))

  compileOnly("com.bencodez:votingplugin:6.18")
}

pluginYml {
  name = "VotingHook"
  main = "net.arcadiusmc.votinghook.VotingHookPlugin"

  depends {
    required("VotingPlugin")
    optional("project:holograms") {
      joinClasspath = false
    }
  }
}