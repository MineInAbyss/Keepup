package com.mineinabyss.keepup.api

import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.gray
import com.mineinabyss.keepup.config_sync.ConfigDefinition
import com.mineinabyss.keepup.config_sync.ConfigTreeBuilder
import com.mineinabyss.keepup.config_sync.Inventory
import com.mineinabyss.keepup.config_sync.templating.Templater
import com.mineinabyss.keepup.helpers.MSG
import com.mineinabyss.keepup.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.*
import kotlin.text.Charsets.UTF_8
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

class KeepupConfigSync(
    val inventory: Inventory,
) {
    @OptIn(ExperimentalStdlibApi::class)
    fun sync(
        host: String,
        configsRoot: Path,
        templateCacheDir: Path?,
        destRoot: Path,
    ) {
        val config = (inventory.configs[host] ?: run {
            t.println("${MSG.error} Config not found: $host")
            return
        })
        val included = inventory.getOrCreateConfigs(host)
        val reduced = ConfigDefinition.reduce(included)

        t.println("${MSG.info} Included paths: ${reduced.copyPaths}")
        val tree = ConfigTreeBuilder()
        val destToSource = tree.destFilesForRoots(configsRoot, reduced.copyPaths)
        val trackedFiles = destToSource.keys.map { it.pathString }.toSet()
        t.println("${MSG.info} Synchronizing ${trackedFiles.size} files...")
        templateCacheDir?.createParentDirectories()
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
                    var failed = 0L
                    entries.forEach { (dest, source) ->
                        val sourceModified = source.getLastModifiedTime()
                        val isTemplate = templateCacheDir != null && source.name.endsWith(".peb")
                        val destAbsolute =
                            if (isTemplate)
                                destRoot / (dest.parent ?: Path("")) / dest.name.removeSuffix(".peb")
                            else destRoot / dest

                        val sourceForSkipComparison = if (isTemplate) {
                            val cacheFile = templateCacheDir /
                                    (dest.parent ?: Path("")) /
                                    "${hashString(reduced.variables.toString() + "len: ${source.fileSize()}").toHexString()}-${dest.name}"

                            // Create template cache if necessary
                            // cacheFile name encodes file size so another check isn't necessary
                            if (cacheFile.notExists() || cacheFile.getLastModifiedTime() != sourceModified) {
                                val output = templater.template(
                                    source.inputStream().bufferedReader().readText(),
                                    reduced.variables
                                ).getOrElse {
                                    t.println("${MSG.error} Failed to template $source")
                                    t.println("${it.message ?: it::class.simpleName}")
                                    failed++
                                    return@forEach
                                }
                                destAbsolute.createParentDirectories()
                                cacheFile.createParentDirectories()
                                cacheFile.writeText(output)
                                cacheFile.setLastModifiedTime(sourceModified)
                                templateCreatedCount++
                                t.println("${MSG.template} $source")
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
                    SyncResult(skipCount, copyCount, templateCreatedCount, failed)
                }
            }.awaitAll()
        }
        val skipCount = results.fold(0L) { acc, syncResult -> acc + syncResult.skipped }
        val copyCount = results.fold(0L) { acc, syncResult -> acc + syncResult.copyCount }
        val templateCount = results.fold(0L) { acc, syncResult -> acc + syncResult.templateCount }
        val failedCount = results.fold(0L) { acc, syncResult -> acc + syncResult.failed }

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
        if (failedCount > 0) t.println("${MSG.error} $failedCount files failed to copy")
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
    val failed: Long,
)


fun shouldSkipCopy(path: Path, other: Path) =
    other.exists() &&
            path.getLastModifiedTime() == other.getLastModifiedTime() &&
            path.fileSize() == other.fileSize()

val MD5 = MessageDigest.getInstance("MD5")
fun hashString(str: String): ByteArray =
    MD5.digest(str.toByteArray(UTF_8))
