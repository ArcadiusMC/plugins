plugins {
  java
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  compileOnly(project(":mail"))
  compileOnly(project(":cosmetics"))
}

pluginYml {
  name = "Marriages"
  main = "net.arcadiusmc.marriages.MarriagePlugin"
}