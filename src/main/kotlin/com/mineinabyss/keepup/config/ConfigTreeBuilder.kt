package com.mineinabyss.keepup.config

import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class ConfigTreeBuilder {
    fun destFilesForRoots(
        roots: Collection<Path>
    ): Set<String> {
        val paths = mutableSetOf<Path>()
        roots.forEach { path ->
            path.walk(PathWalkOption.INCLUDE_DIRECTORIES)
                .filter { it.isRegularFile() }
                .forEach {
                    it.relativeTo(path)
                }
        }
        return paths.map { it.pathString }.toSet()
    }

    fun deleteUntrackedFor(
        root: Path,
        deleteUnder: Path,
        tracked: Set<String>,
    ) {
        val relative = deleteUnder.relativeTo(root)
        deleteUnder.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .filter { it.isRegularFile() }
            .filter { it.pathString !in tracked }
            .forEach { it.deleteIfExists() }
    }
}
