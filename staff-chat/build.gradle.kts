plugins {
  java
}

repositories {
  maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":discord"))
}

pluginYml {
  name = "StaffChat"
  main = "net.arcadiusmc.staffchat.StaffChatPlugin"

  depends {
    optional("project:discord")
  }
}