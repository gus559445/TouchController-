import net.neoforged.nfrtgradle.CreateMinecraftArtifacts
import top.fifthlight.touchcontoller.gradle.MinecraftVersion
import top.fifthlight.touchcontoller.gradle.service.CreateMinecraftArtifactsService

plugins {
    idea
    java
    id("net.neoforged.moddev")
    id("com.gradleup.gr8")
    id("r8-parallel")
}

val modId: String by extra.properties
val modName: String by extra.properties
val modVersion: String by extra.properties
val modDescription: String by extra.properties
val modLicense: String by extra.properties
val modLicenseLink: String by extra.properties
val modIssueTracker: String by extra.properties
val modHomepage: String by extra.properties
val modAuthors: String by extra.properties
val modContributors: String by extra.properties
val gameVersion: String by extra.properties
val neoforgeVersion: String by extra.properties
val majorLoaderVersion: String by extra.properties
val minecraftVersion = MinecraftVersion(gameVersion)
val useAccessTransformer: String by extra.properties
val useAccessTransformerBool = useAccessTransformer.toBoolean()

val localProperties: Map<String, String> by rootProject.ext
val minecraftVmArgs = localProperties["minecraft.vm-args"]?.toString()?.split(":") ?: listOf()

version = "$modVersion+neoforge-$gameVersion"
group = "top.fifthlight.touchcontroller"

val createMinecraftArtifactsService =
    project.gradle.sharedServices.registerIfAbsent("gr8", CreateMinecraftArtifactsService::class.java) {
        maxParallelUsages = 1
    }

tasks.withType<CreateMinecraftArtifacts> {
    usesService(createMinecraftArtifactsService)
}

neoForge {
    version = neoforgeVersion

    validateAccessTransformers = true

    if (useAccessTransformerBool) {
        accessTransformers.from("src/main/resources/META-INF/accesstransformer.cfg")
    }

    val parchmentVersion = properties["parchmentVersion"]?.toString()
    if (parchmentVersion != null) {
        parchment {
            this.minecraftVersion = gameVersion
            mappingsVersion = parchmentVersion
        }
    }

    runs {
        register("client") {
            client()
            jvmArguments.addAll(minecraftVmArgs)
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

configurations.create("shadow")

tasks.jar {
    archiveBaseName = "$modName-slim"
}

fun DependencyHandlerScope.shadeAndImplementation(dependency: Any) {
    add("shadow", dependency)
    implementation(dependency)
}

fun <T : ModuleDependency> DependencyHandlerScope.shadeAndImplementation(
    dependency: T,
    dependencyConfiguration: T.() -> Unit,
) {
    add("shadow", dependency, dependencyConfiguration)
    implementation(dependency, dependencyConfiguration)
}

dependencies {
    shadeAndImplementation(project(":mod:resources", "texture"))
    shadeAndImplementation(project(":mod:resources", "forge-icon"))
    shadeAndImplementation(project(":mod:resources", "lang"))
    shadeAndImplementation(project(":mod:common")) {
        exclude("org.slf4j")
    }
    shadeAndImplementation(project(":combine"))
}

tasks.processResources {
    val modAuthorsList = modAuthors.split(",").map(String::trim).filter(String::isNotEmpty)
    val modContributorsList = modContributors.split(",").map(String::trim).filter(String::isNotEmpty)
    fun String.quote(quoteStartChar: Char = '"', quoteEndChar: Char = '"') = quoteStartChar + this + quoteEndChar
    val modAuthorsArray = modAuthorsList.joinToString(", ", transform = String::quote).drop(1).dropLast(1)
    val modContributorsArray = modContributorsList.joinToString(", ", transform = String::quote).drop(1).dropLast(1)

    val properties = mutableMapOf(
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version_full" to version,
        "mod_license" to modLicense,
        "mod_license_link" to modLicenseLink,
        "mod_issue_tracker" to modIssueTracker,
        "mod_homepage" to modHomepage,
        "mod_authors_string" to modAuthors,
        "mod_contributors_string" to modContributors,
        "mod_authors_array" to modAuthorsArray,
        "mod_contributors_array" to modContributorsArray,
        "neoforge_version" to neoforgeVersion,
        "mod_description" to modDescription,
        "game_version" to gameVersion,
        "major_loader_version" to majorLoaderVersion,
    )

    properties += if (useAccessTransformerBool) {
        "access_transformer_entry" to """
            [[accessTransformers]]
            file="META-INF/accesstransformer.cfg"
            """.trimIndent()
    } else {
        "access_transformer_entry" to ""
    }

    inputs.properties(properties)

    from(rootProject.file("mod/common-neoforge/src/main/resources/META-INF/neoforge.mods.toml")) {
        into("META-INF")
        expand(properties)
        if (minecraftVersion < MinecraftVersion(1, 20, 6)) {
            rename { "mods.toml" }
        }
    }

    from("pack.mcmeta") {
        expand(properties)
    }

    from(File(rootDir, "LICENSE")) {
        rename { "${it}_${modName}" }
    }
}

gr8 {
    create("gr8") {
        addProgramJarsFrom(configurations.getByName("shadow"))
        addProgramJarsFrom(tasks.jar)

        addClassPathJarsFrom(configurations.runtimeClasspath)

        r8Version("8.9.21")
        proguardFile(rootProject.file("mod/common-neoforge/rules.pro"))
    }
}

val copyJarTask = tasks.register<Jar>("copyJar") {
    dependsOn("gr8Gr8ShadowedJar")
    archiveBaseName = modName
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    val jarFile = tasks.getByName("gr8Gr8ShadowedJar").outputs.files
        .first { it.extension.equals("jar", ignoreCase = true) }

    manifest {
        attributes("MixinConfigs" to "touchcontroller.mixins.json")
    }

    val excludeWhitelist = listOf(
        "accesstransformer.cfg",
        "neoforge.mods.toml",
        "mods.toml",
    )
    from(zipTree(jarFile)) {
        exclude { file ->
            val path = file.relativePath
            if (path.segments.first() == "META-INF") {
                excludeWhitelist.all { path.lastName != it }
            } else {
                path.lastName == "module-info.class"
            }
        }
    }
}

tasks.assemble {
    dependsOn(copyJarTask)
}
