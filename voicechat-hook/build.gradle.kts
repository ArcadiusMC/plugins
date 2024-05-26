plugins {
  java
}

repositories {
  mavenCentral()
  maven("https://maven.maxhenkel.de/repository/public")
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly("de.maxhenkel.voicechat:voicechat-api:2.5.0")
}

pluginYml {
  prefixedName("Voicechat-Hook")
  main = "net.arcadiusmc.voicechat.HookPlugin"

  depends {
    required("voicechat")
  }
}