import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import net.arcadiusmc.gradle.NMS_DEPENDENCY

plugins {
  java
  id("io.papermc.paperweight.userdev") version "1.6.0"
}

dependencies {
  paperweight.paperDevBundle(NMS_DEPENDENCY)
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

arcadius {
  implementedBy = "core"
}