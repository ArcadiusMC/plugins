plugins {
  java
}

repositories {
  mavenCentral()
  maven("https://jitpack.io")
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly("com.github.MilkBowl:VaultAPI:1.7")
}

pluginYml {
  name = "Vault-Hook"
  main = "net.arcadiusmc.vault.VaultPlugin"

  depends {
    optional("Vault")
  }
}