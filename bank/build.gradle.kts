plugins {
  java
}

repositories {
  mavenCentral()
  maven("https://libraries.arcadiusmc.net/")
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":core"))
  compileOnly("net.arcadiusmc:delphi:1.0.0-SNAPSHOT")
}

pluginYml {
  name = "BankVaults"
  main = "net.arcadiusmc.bank.BankPlugin"

  depends {
    optional("Delphi")
  }
}