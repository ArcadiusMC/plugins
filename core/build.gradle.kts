import net.arcadiusmc.gradle.*

plugins {
  java
  id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {

}

dependencies {
  compileOnly("net.luckperms:api:5.4")
  compileOnly(project(":commons"))

  implementation(project(":commons", "reobf"))
  implementation(project(":class-loader-tools"))
}

pluginYml {
  name = "ArcadiusCore"
  main = "net.arcadiusmc.core.CorePlugin"
  loader = "net.arcadiusmc.core.CoreLoader"

  depends {
    required("LuckPerms")
    required("FastAsyncWorldEdit")
    required("WorldGuard")
  }

  loadBefore {
    regular("GriefPrevention")
    regular("OpenInv")
  }
}

tasks {
  buildAndCopyToRoot {
    dependsOn(shadowJar)
  }

  processResources {
    filesMatching("runtime_libraries.json") {
      expand(
          "grenadier"             to GRENADIER,
          "grenadierAnnotations"  to GRENADIER_ANNOT,
          "toml"                  to TOML,
          "mathLib"               to MATH_LIB,
          "configurate"           to CONFIGURATE,
          "nbtlib"                to NBT_LIB,
      )
    }
  }
}

