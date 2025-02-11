package com.mineinabyss.keepup.config_sync

import com.mineinabyss.keepup.helpers.MSG
import com.mineinabyss.keepup.t
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
class ConfigTreeBuilder {
    fun destFilesForRoots(
        configsRoot: Path,
        roots: Collection<CopyPath>
    ): Map<Path, Path> {
        val destToSource = mutableMapOf<Path, Path>()
        roots.forEach { copyPath ->
            val sourceRoot = configsRoot / copyPath.source
            val destOffset = Path(copyPath.dest)
            when {
                sourceRoot.isRegularFile() -> {
                    val dest = destOffset / sourceRoot.fileName
                    destToSource[dest] = sourceRoot
                    return@forEach
                }
                sourceRoot.isDirectory() -> sourceRoot.walk(PathWalkOption.INCLUDE_DIRECTORIES)
                    .filter { it.isRegularFile() }
                    .forEach { source ->
                        val dest = destOffset / source.relativeTo(sourceRoot)
                        destToSource[dest] = source
                    }
                else -> t.println("${MSG.warn} Included path $sourceRoot does not exist.")
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
        if (deleteDir.notExists()) return
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
