import net.arcadiusmc.gradle.*
import java.time.LocalDateTime

plugins {
  java
  id("arcadius")
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

val lombokDep = "org.projectlombok:lombok:1.18.38"

subprojects {
  apply(plugin = "java")
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

    for (dep in TEST_DEPENDENCIES) {
      testImplementation(dep)
    }

    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")

    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-engine
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.3")

    compileOnly(lombokDep)
    annotationProcessor(lombokDep)
    testCompileOnly(lombokDep)
    testAnnotationProcessor(lombokDep)
  }

  tasks {
    test {
      useJUnitPlatform()
    }

    javadoc {
      options.encoding = Charsets.UTF_8.name()
    }

    compileJava {
      options.encoding = Charsets.UTF_8.name()
      options.release.set(21)
      options.compilerArgs.add("-Xmaxerrs")
      options.compilerArgs.add("3000")
    }
  }
}