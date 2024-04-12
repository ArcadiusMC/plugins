plugins {
  java
}

repositories {
  mavenCentral()
  maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":usables"))
  compileOnly(project(":player-markets"))

  compileOnly(project(":discord"))
  compileOnly("com.discordsrv:discordsrv:1.27.1-SNAPSHOT")
}

pluginYml {
  name = "Arcadius-Factions"
  main = "net.arcadiusmc.factions.FactionsPlugin"

  depends {
    optional("project:player-markets")
    optional("project:discord")
    optional("DiscordSRV")
  }
}