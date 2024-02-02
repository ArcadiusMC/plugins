import net.arcadiusmc.gradle.ArcadiusExtension
import net.arcadiusmc.gradle.DEPENDENCIES
import net.arcadiusmc.gradle.REPOSITORIES
import net.arcadiusmc.gradle.TASK_GROUP
import java.time.LocalDateTime

plugins {
  java
  id("arcadius")
  id("io.freefair.lombok") version "8.0.1"
}

version = makeDateVersion()
group = "net.arcadiusmc"

fun makeDateVersion(): String {
  val date = LocalDateTime.now()
  val month = date.monthValue.toString().padStart(2, '0')
  val day = date.dayOfMonth.toString().padStart(2, '0')
  return "${date.year}-$month-$day"
}

repositories {
  mavenCentral()
}

val buildAll = task("build-all-plugins") {
  group = TASK_GROUP
  description = "Builds all plugin modules and moves them into the build directory"
}

subprojects {
  apply(plugin = "java")
  apply(plugin = "io.freefair.lombok")
  apply(plugin = "arcadius")

  group = rootProject.group
  version = rootProject.version

  afterEvaluate {
    val arcExt = this.extensions.findByType(ArcadiusExtension::class.java) ?: return@afterEvaluate

    if (arcExt.skipDependency || !arcExt.implementedBy.isNullOrEmpty()) {
      return@afterEvaluate
    }

    buildAll.dependsOn(":${this.name}:buildAndCopyToRoot")
  }

  repositories {
    mavenCentral()

    for (repository in REPOSITORIES) {
      maven(repository)
    }
  }

  dependencies {
    for (dependency in DEPENDENCIES) {
      compileOnly(dependency)
    }
  }

  java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
  }

  tasks {
    javadoc {
      options.encoding = Charsets.UTF_8.name()
    }

    compileJava {
      options.encoding = Charsets.UTF_8.name()
      options.release.set(17)
      options.compilerArgs.add("-Xmaxerrs")
      options.compilerArgs.add("3000")
    }
  }
}