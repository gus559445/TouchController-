package top.fifthlight.touchcontoller.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import top.fifthlight.touchcontoller.gradle.ext.sha1sum
import top.fifthlight.touchcontoller.gradle.ext.toHexString
import top.fifthlight.touchcontoller.gradle.service.StubGenBuildService
import top.fifthlight.touchcontoller.gradle.task.GenerateStubTask
import java.net.URI

abstract class MinecraftStubExtension {
    abstract val versions: ListProperty<String>
    abstract val manifestUrl: Property<URI>

    init {
        versions.convention(emptyList())
        manifestUrl.convention(URI("https://piston-meta.mojang.com/mc/game/version_manifest.json"))
    }

    fun versions(vararg values: String) {
        versions.set(values.toList())
    }
}

@Suppress("unused")
class StubGenPlugin : Plugin<Project> {
    private fun versionHash(versions: List<String>) = versions.sorted().joinToString(";").sha1sum().toHexString()

    override fun apply(project: Project) {
        val extension = project.extensions.create("minecraftStub", MinecraftStubExtension::class.java)

        val versionHash = extension.versions.map { versionHash(it) }
        val stubCacheDir = project.rootProject.layout.projectDirectory.dir(".gradle/minecraft-stub")
        val outputStubDir = versionHash.map { stubCacheDir.dir("generated-stubs").dir(it) }

        val gr8BuildingService =
            project.gradle.sharedServices.registerIfAbsent("stub-gen", StubGenBuildService::class.java) {
                maxParallelUsages.set(1)
            }

        val generateTask = project.tasks.register("generateStub", GenerateStubTask::class.java) {
            usesService(gr8BuildingService)

            versions.set(extension.versions)
            manifestUrl.set(extension.manifestUrl)
            libraryDir.set(outputStubDir)
            val versionString = extension.versions.map { it.sorted().joinToString("-") }
            outputFile.set(versionString.zip(outputStubDir) { versions, stubDir -> stubDir.file("stub-${versions}.jar") })
        }

        project.tasks.getByName("compileJava") {
            dependsOn(generateTask)
        }

        project.afterEvaluate {
            project.dependencies.add("compileOnly", project.fileTree(outputStubDir))
        }
    }
}
