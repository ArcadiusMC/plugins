package net.arcadiusmc.gradle

import org.gradle.api.Project

class ArcadiusExtension(private val project: Project) {
  val apiVersion: String get() = MC_VERSION
  var implementedBy: String? = null;
  var skipDependency: Boolean = false;
  var autoAddDependencies: Boolean = true;
}