import net.arcadiusmc.gradle.DependencyLoad
import net.arcadiusmc.gradle.MC_VERSION

plugins {
  java
  id("io.papermc.paperweight.userdev") version "1.5.5"
}

version = MC_VERSION

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  paperweight.paperDevBundle("${MC_VERSION}-R0.1-SNAPSHOT")
}

arcadius {
  autoAddDependencies = false
}

pluginYml {
  name = "Arcadius-Vanilla-Hook"
  main = "net.arcadiusmc.vanilla.VanillaPlugin"

  loadBefore {
    regular("project:commons")
  }

  depends {
    required("project:commons") {
      joinClasspath = true
      load = DependencyLoad.AFTER
    }
  }
}

tasks {
  assemble {
    dependsOn(reobfJar)
  }
}