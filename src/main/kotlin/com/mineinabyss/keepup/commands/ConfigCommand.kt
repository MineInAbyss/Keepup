package com.mineinabyss.keepup.commands

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.mineinabyss.keepup.config.ConfigDefinition
import com.mineinabyss.keepup.config.ConfigTreeBuilder
import com.mineinabyss.keepup.config.Inventory
import com.mineinabyss.keepup.config.templating.Templater
import helpers.MSG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

class ConfigCommand : CliktCommand(name = "config", help = "Syncs local config files to appropriate destinations") {
    val targetName by argument(help = "Target server name for configs feature, used to figure out which configs to copy, etc...")

    val inventoryPath by argument(help = "Inventory file defining config options")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)

    val destRoot by argument()
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)

    val templateCacheDir by argument(help = "Directory to cache template results, if unspecified will not templates .peb files")
        .path(mustExist = false, canBeFile = false, mustBeWritable = true)
        .optional()

    @OptIn(ExperimentalStdlibApi::class)
    override fun run() {
        t.println("${MSG.info} Running config sync for $targetName...")
        val inventory = Yaml.default.decodeFromStream<Inventory>(inventoryPath.inputStream())
        val config = (inventory.configs[targetName] ?: run {
            t.println("${MSG.error} Config not found: $targetName")
            return
        })
        val included = inventory.getOrCreateConfigs(config.include)
        val reduced = ConfigDefinition.reduce(included + config)

        val paths = reduced.copyPaths.map { inventoryPath.parent / it }

        val tree = ConfigTreeBuilder()
        val destToSource = tree.destFilesForRoots(paths)
        val trackedFiles = destToSource.keys.map { it.pathString }.toSet()
        t.println("${MSG.info} Synchronizing ${trackedFiles.size} files...")

        val startTime = TimeSource.Monotonic.markNow()
        val templater = Templater()
        val results = runBlocking(Dispatchers.IO) {
            val destRoot = destRoot
            val templateCacheDir = templateCacheDir
            destToSource.entries.chunked(LAUNCH_FILES_CHUNK_SIZE).map { entries ->
                async {
                    var skipCount = 0L
                    var copyCount = 0L
                    var templateCreatedCount = 0L
                    entries.forEach { (dest, source) ->
                        val sourceModified = source.getLastModifiedTime()
                        val isTemplate = templateCacheDir != null && source.name.endsWith(".peb")
                        val destAbsolute =
                            if (isTemplate)
                                destRoot / (dest.parent ?: Path(".")) / dest.name.removeSuffix(".peb")
                            else destRoot / dest

                        val sourceForSkipComparison = if (isTemplate) {
                            val cacheFile = templateCacheDir!! / (dest.parent ?: Path(".")) / "${
                                source.fileSize().toHexString()
                            }-${dest.name}"

                            // Create template cache if necessary
                            // cacheFile name encodes file size so another check isn't necessary
                            if (cacheFile.notExists() || cacheFile.getLastModifiedTime() != sourceModified) {
                                val output = templater.template(
                                    source.inputStream().bufferedReader().readText(),
                                    reduced.variables
                                )
                                destAbsolute.createParentDirectories()
                                cacheFile.createParentDirectories()
                                cacheFile.writeText(output)
                                cacheFile.setLastModifiedTime(sourceModified)
                                templateCreatedCount++
                            }
                            cacheFile
                        } else source
                        if (shouldSkipCopy(sourceForSkipComparison, destAbsolute)) {
//                            t.println("Skipping $destAbsolute")
                            skipCount++
                            return@forEach
                        }
                        t.println("${MSG.copy} $source -> $destAbsolute")
                        copyCount++
                        sourceForSkipComparison.copyTo(destAbsolute.createParentDirectories(), overwrite = true)
                        destAbsolute.setLastModifiedTime(sourceModified)
                    }
                    SyncResult(skipCount, copyCount, templateCreatedCount)
                }
            }.awaitAll()
        }
        val skipCount = results.fold(0L) { acc, syncResult -> acc + syncResult.skipped }
        val copyCount = results.fold(0L) { acc, syncResult -> acc + syncResult.copyCount }
        val templateCount = results.fold(0L) { acc, syncResult -> acc + syncResult.templateCount }

        var deleteCount = 0
        if (reduced.files.isNotEmpty()) {
            t.println("${MSG.info} Deleting untracked files in destination: ${reduced.files.keys}")
            reduced.files
                .filterValues { it.deleteUntracked }
                .forEach { (path, config) ->
                    val deletePath = destRoot / path
                    tree.onUntracked(destRoot, deletePath, config, trackedFiles.map {
                        // Remove .peb extension for templates
                        if (templateCacheDir != null) it.removeSuffix(".peb") else it
                    }.toSet()) {
                        t.println("${MSG.delete}${gray("[Untracked] ${it.relativeTo(destRoot)}")}")
                        it.deleteIfExists()
                        deleteCount++
                    }
                }
        }

        val elapsed = startTime.elapsedNow().toString(unit = DurationUnit.SECONDS, decimals = 2)
        t.println("${MSG.info} ${brightGreen("Done synchronizing configs in $elapsed")}")
        t.println("${MSG.info} ${brightGreen("Skipped $skipCount, Copied $copyCount, Deleted $deleteCount, Generated templates $templateCount")}")
    }

    companion object {
        // TODO I have no clue what an optimal chunk size here would be
        const val LAUNCH_FILES_CHUNK_SIZE = 1000
    }
}

class SyncResult(
    val skipped: Long,
    val copyCount: Long,
    val templateCount: Long,
)


fun shouldSkipCopy(path: Path, other: Path) =
    other.exists() &&
            path.getLastModifiedTime() == other.getLastModifiedTime() &&
            path.fileSize() == other.fileSize()
