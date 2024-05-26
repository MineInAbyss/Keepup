package com.mineinabyss.keepup.commands

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.mineinabyss.keepup.config.ConfigTreeBuilder
import com.mineinabyss.keepup.config.Inventory
import helpers.MSG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlin.io.path.*
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

class ConfigCommand : CliktCommand(name = "config") {
    val targetName by argument(help = "Target server name for configs feature, used to figure out which configs to copy, etc...")

    val inventoryPath by argument(help = "Inventory file defining config options")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)

    val destRoot by argument()
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)

    override fun run() {
        t.println("${MSG.info} Running config sync")
        val inventory = Yaml.default.decodeFromStream<Inventory>(inventoryPath.inputStream())
        val paths = (inventory.targets[targetName] ?: emptyList())
            .map { inventoryPath.parent / it / "sync" }
        val tree = ConfigTreeBuilder()
        val destToSource = tree.destFilesForRoots(paths)
        val trackedFiles = destToSource.keys.map { it.pathString }.toSet()
        t.println("Tracked files were $trackedFiles")

        t.println("${MSG.info} Synchronizing files...")

        val startTime = TimeSource.Monotonic.markNow()
        val results = runBlocking(Dispatchers.IO) {
            val destRoot = destRoot
            destToSource.entries.chunked(LAUNCH_FILES_CHUNK_SIZE).map { entries ->
                async {
                    var skipCount = 0L
                    var copyCount = 0L
                    entries.forEach { (dest, source) ->
                        val sourceModified = source.getLastModifiedTime()
                        val destAbsolute = destRoot / dest
                        if (destAbsolute.exists() &&
                            sourceModified == destAbsolute.getLastModifiedTime() &&
                            source.fileSize() == destAbsolute.fileSize()
                        ) {
//                            t.println("Skipping $destAbsolute")
                            skipCount++
                            return@forEach
                        }
                        t.println("${MSG.copy} $source -> $destAbsolute")
                        copyCount++
                        source.copyTo(destAbsolute.createParentDirectories(), overwrite = true)
                        destAbsolute.setLastModifiedTime(sourceModified)
                    }
                    SyncResult(skipCount, copyCount)
                }
            }.awaitAll()
        }
        val skipCount = results.fold(0L) { acc, syncResult -> acc + syncResult.skipped }
        val copyCount = results.fold(0L) { acc, syncResult -> acc + syncResult.copyCount }

        var deleteCount = 0
        if (inventory.files.isNotEmpty()) {
            t.println("${MSG.info} Deleting untracked files in destination: ${inventory.files.keys}")
            inventory.files
                .filterValues { it.deleteUntracked }
                .forEach { (path, config) ->
                    val deletePath = destRoot / path
                    tree.onUntracked(destRoot, deletePath, config, trackedFiles) {
                        t.println("${MSG.delete}${gray("[Untracked] ${it.relativeTo(destRoot)}")}")
                        it.deleteIfExists()
                        deleteCount++
                    }
                }
        }

        val elapsed = startTime.elapsedNow().toString(unit = DurationUnit.SECONDS, decimals = 2)
        t.println("${MSG.info} ${brightGreen("Done synchronizing configs in $elapsed")}")
        t.println("${MSG.info} ${brightGreen("Skipped $skipCount, Copied $copyCount, Deleted $deleteCount!")}")
    }

    companion object {
        // TODO I have no clue what an optimal chunk size here would be
        const val LAUNCH_FILES_CHUNK_SIZE = 1000
    }
}

class SyncResult(
    val skipped: Long,
    val copyCount: Long,
)
