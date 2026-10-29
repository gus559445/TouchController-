package top.fifthlight.touchcontoller.gradle.service

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

abstract class StubGenBuildService : BuildService<BuildServiceParameters.None>, AutoCloseable {
    override fun close() {}
}