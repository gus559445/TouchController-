@file:Suppress("UNCHECKED_CAST")

package top.fifthlight.touchcontoller.gradle.data

import java.net.URI

data class VersionManifest(
    val latest: Latest,
    val versions: List<VersionEntry>
) {
    constructor(obj: Map<String, Any>) : this(
        latest = Latest(obj["latest"]!! as Map<String, String>),
        versions = (obj["versions"]!! as List<Map<String, String>>).map { VersionEntry(it) },
    )
}

data class Latest(
    val release: String,
    val snapshot: String
) {
    constructor(obj: Map<String, String>) : this(
        release = obj["release"]!!,
        snapshot = obj["snapshot"]!!,
    )
}

data class VersionEntry(
    val id: String,
    val url: String
) {
    constructor(obj: Map<String, String>) : this(
        id = obj["id"]!!,
        url = obj["url"]!!,
    )
}

data class VersionInfo(
    val id: String,
    val downloads: Downloads,
    val libraries: List<Library>,
) {
    constructor(obj: Map<String, Any>) : this(
        id = obj["id"]!! as String,
        downloads = Downloads(obj["downloads"]!! as Map<String, Any>),
        libraries = (obj["libraries"]!! as List<Map<String, Any>>).map(::Library),
    )
}

data class Downloads(
    val client: DownloadArtifact,
    val server: DownloadArtifact,
    val clientMappings: DownloadArtifact?,
    val serverMappings: DownloadArtifact?,
) {
    constructor(obj: Map<String, Any>) : this(
        client = DownloadArtifact(obj["client"]!! as Map<String, String>),
        server = DownloadArtifact(obj["server"]!! as Map<String, String>),
        clientMappings = (obj["client_mappings"] as? Map<String, String>)?.let(::DownloadArtifact),
        serverMappings = (obj["server_mappings"] as? Map<String, String>)?.let(::DownloadArtifact),
    )
}

data class Library(
    val name: String,
    val downloadArtifact: DownloadArtifact,
    val haveRules: Boolean
) {
    constructor(obj: Map<String, Any>) : this(
        name = obj["name"]!! as String,
        downloadArtifact = obj["downloads"]!!
            .let { it as Map<String, Any> }
            .let { it["artifact"] }
            .let { it as Map<String, Any> }
            .let { DownloadArtifact(it) },
        haveRules = obj.containsKey("rules"),
    )
}

data class DownloadArtifact(
    val url: URI,
    val sha1: String,
    val size: Int
) {
    constructor(obj: Map<String, Any>) : this(
        url = URI(obj["url"]!! as String),
        sha1 = obj["sha1"]!! as String,
        size = obj["size"]!! as Int,
    )
}
