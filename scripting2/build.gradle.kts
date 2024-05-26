plugins {
  `java-library`
}

val graalVersion = "24.0.1"
val graalPrefix = "org.graalvm.polyglot"
val polyglot = "$graalPrefix:polyglot:$graalVersion"
val polyglotJs = "$graalPrefix:js:$graalVersion"

repositories {
  mavenCentral()
}

dependencies {
  api(polyglot)
  api(polyglotJs)

  compileOnly(project(":commons"))
}

arcadius {
  implementedBy = "scripting2-impl"
}