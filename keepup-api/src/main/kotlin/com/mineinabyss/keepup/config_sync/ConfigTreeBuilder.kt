package com.mineinabyss.keepup.config_sync

import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class ConfigTreeBuilder {
    fun destFilesForRoots(
        roots: Collection<Path>
    ): Map<Path, Path> {
        val destToSource = mutableMapOf<Path, Path>()
        roots.forEach { sourceRoot ->
            sourceRoot.walk(PathWalkOption.INCLUDE_DIRECTORIES)
                .filter { it.isRegularFile() }
                .forEach { source ->
                    val dest = source.relativeTo(sourceRoot)
                    destToSource[dest] = source
                }
        }
        return destToSource
    }

    fun onUntracked(
        root: Path,
        deleteUnder: Path,
        config: FileConfig,
        tracked: Set<String>,
        onUntracked: (Path) -> Unit,
    ) {
        val deleteDir = (root / deleteUnder)
        deleteDir.visitFileTree {
            onPreVisitDirectory { directory, attr ->
                if ("${directory.relativeTo(deleteDir).pathString}/" in config.keep)
                    FileVisitResult.SKIP_SUBTREE
                else
                    FileVisitResult.CONTINUE
            }
            onVisitFile { file, _ ->
                when {
                    file.name in config.keep -> {}
                    file.relativeTo(root).pathString !in tracked -> {
                        onUntracked(file)
                    }
                }
                FileVisitResult.CONTINUE
            }
        }
    }
}
