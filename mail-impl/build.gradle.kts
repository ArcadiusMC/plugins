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
  compileOnly("com.discordsrv:discordsrv:1.27.1-SNAPSHOT")
}

pluginYml {
  name = "Mail"
  main = "net.arcadiusmc.mail.MailPlugin"

  depends {
    optional("project:discord")
  }
}

tasks {
  buildAndCopyToRoot {
    dependsOn(shadowJar)
  }
}