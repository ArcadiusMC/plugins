import net.arcadiusmc.gradle.MC_VERSION

plugins {
  java
  id("io.papermc.paperweight.userdev") version "1.5.5"
}

dependencies {
  paperweight.paperDevBundle("${MC_VERSION}-R0.1-SNAPSHOT")
}

arcadius {
  implementedBy = "core"
}

tasks {
  assemble {
    dependsOn(reobfJar)
  }
}