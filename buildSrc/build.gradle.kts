import org.gradle.api.JavaVersion

plugins {
  java
  `java-gradle-plugin`
  kotlin("jvm") version "2.1.21"
}

repositories {
  mavenCentral()
}

dependencies {

}

gradlePlugin {
  plugins {
    register("arcadius") {
      id = "arcadius"
      implementationClass = "net.arcadiusmc.gradle.ArcadiusGradlePlugin"
    }
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_21 // or VERSION_11, VERSION_1_8, etc.
  targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "21" // must match Java version
  }
}