plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":core"))
}

pluginYml {
  name = "BankVaults"
  main = "net.arcadiusmc.bank.BankPlugin"
}