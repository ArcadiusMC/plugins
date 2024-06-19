package net.arcadiusmc.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

const val CREATE_PLUGIN_YML = "createPluginYml"
const val TASK_GROUP = "Arcadius"

private const val BUILD_ALL_PLUGINS = "build-all-plugins"

class ArcadiusGradlePlugin: Plugin<Project> {

  override fun apply(target: Project) {
    if (target == target.rootProject) {
      return
    }

    addGeneratedToSourceSet(target)

    val yml = PaperConfig(target.name)
    val arcadiusExtension = ArcadiusExtension(target)

    yml.authors {
      add("JulieWoolie") // :3
    }

    target.extensions.run {
      add("pluginYml", yml)
      add("arcadius", arcadiusExtension)
    }

    createPluginYmlTask(target, yml, arcadiusExtension)
    createRootBuildTask(target, yml)
  }

  private fun createRootBuildTask(target: Project, yml: PaperConfig) {
    val task = target.task("buildAndCopyToRoot")
    task.group = TASK_GROUP
    task.description = "Builds the project, and then moves the jar file to the root build directory"

    task.dependsOn("build")

    task.doLast {
      val proj = it.project;
      val root = proj.rootProject;

      val projLibs = buildLibs(proj)
      val rootLibs = buildLibs(root)

      val jarName = "${getArchiveName(yml)}-${proj.version}"

      if (moveJar(projLibs, rootLibs, "$jarName-all.jar")) {
        return@doLast
      }

      moveJar(projLibs, rootLibs, "$jarName.jar")
    }
  }

  private fun buildLibs(project: Project)
      = project.layout.buildDirectory.asFile.get().toPath().resolve("libs")

  private fun moveJar(sourceDir: Path, destDir: Path, fileName: String): Boolean {
    val sourceFile = sourceDir.resolve(fileName)
    val destFile = destDir.resolve(fileName)

    if (!Files.exists(sourceFile)) {
      return false
    }

    if (!Files.exists(destDir)) {
      Files.createDirectories(destDir)
    }

    Files.copy(
        sourceFile,
        destFile,
        StandardCopyOption.COPY_ATTRIBUTES,
        StandardCopyOption.REPLACE_EXISTING
    )
    return true
  }

  private fun createPluginYmlTask(target: Project, yml: PaperConfig, arcadiusExtension: ArcadiusExtension) {
    val task = target.task(CREATE_PLUGIN_YML)
    task.group = TASK_GROUP
    task.description = "Creates a paper-plugin.yml"

    task.doFirst {
      if (arcadiusExtension.skipDependency || !arcadiusExtension.implementedBy.isNullOrEmpty()) {
        return@doFirst
      }

      if (arcadiusExtension.autoAddDependencies) {
        scanForProjectDependencies(it.project, yml)
      }

      setArchiveName(it.project, yml)
      createPluginYml(it)
    }

    target.tasks.findByName("compileJava")?.dependsOn(CREATE_PLUGIN_YML)
  }

  private fun getArchiveName(yml: PaperConfig): String? {
    if (yml.jarName.isEmpty()) {
      return yml.name;
    }

    return yml.jarName;
  }

  private fun setArchiveName(project: Project, yml: PaperConfig) {
    val base = project.extensions.getByType(BasePluginExtension::class.java)
    base.archivesName.set(getArchiveName(yml))
  }

  private fun addGeneratedToSourceSet(project: Project) {
    val jPlugin = project.extensions.findByType(JavaPluginExtension::class.java)!!
    val sourceSets = jPlugin.sourceSets

    sourceSets.findByName("main")?.apply {
      this.resources {
        it.srcDir(GENERATE_CONFIG_DIR)
      }
    }
  }

  private fun scanForProjectDependencies(project: Project, yml: PaperConfig) {
    project.configurations.forEach { it ->
      it.allDependencies.stream()
        .filter { it is ProjectDependency }
        .map { it as ProjectDependency }

        .forEach {
          addDependsFromProject(yml, it)
        }
    }
  }

  private fun addDependsFromProject(yml: PaperConfig, it: ProjectDependency) {
    val proj = it.dependencyProject
    val projExt = proj.extensions.findByType(PaperConfig::class.java)

    val dependencyName: String

    if (projExt != null) {
      dependencyName = "project:${proj.name}"
    } else {
      return
    }

    yml.depends {
      if (map.containsKey(dependencyName)) {
        return@depends
      }

      required(dependencyName)
    }
  }
}