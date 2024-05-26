
import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import net.arcadiusmc.gradle.DependencyLoad
import net.arcadiusmc.gradle.MC_VERSION
import net.arcadiusmc.gradle.NMS_DEPENDENCY

plugins {
  java
  id("io.papermc.paperweight.userdev") version "1.7.1"
}

version = MC_VERSION

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  paperweight.paperDevBundle(NMS_DEPENDENCY)
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

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