plugins {
  java
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
  mavenCentral()
  maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":scripting"))

  implementation(project(":mail"))

  compileOnly(project(":discord"))
  compileOnly("com.discordsrv:discordsrv:1.27.0-SNAPSHOT")
}

pluginYml {
  name = "Mail"
  main = "net.arcadiusmc.mail.MailPlugin"

  loadAfter {
    regular("project:guilds")
  }
}

tasks {
  buildAndCopyToRoot {
    dependsOn(shadowJar)
  }
}