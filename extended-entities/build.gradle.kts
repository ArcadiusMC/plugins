plugins {
  `java-library`
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

val ashley = "com.badlogicgames.ashley:ashley:1.7.4"

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":commons"))
  implementation(project(":class-loader-tools"))
  api(ashley)
}

pluginYml {
  prefixedName("Entities")
  main = "net.arcadiusmc.entity.EntityPlugin"
  loader = "net.arcadiusmc.entity.EntityPluginLoader"
}

tasks {
  processResources {
    filesMatching("runtime_libraries.json") {
      expand("ashley" to ashley)
    }
  }

  buildAndCopyToRoot {
    dependsOn(shadowJar)
  }
}