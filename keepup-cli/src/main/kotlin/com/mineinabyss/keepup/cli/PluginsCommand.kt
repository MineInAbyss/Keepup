package com.mineinabyss.keepup.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.widgets.progress.progressBar
import com.github.ajalt.mordant.widgets.progress.progressBarLayout
import com.mineinabyss.keepup.api.*
import com.mineinabyss.keepup.downloads.DownloadResult
import com.mineinabyss.keepup.downloads.github.GithubConfig
import com.mineinabyss.keepup.downloads.github.GithubReleaseOverride
import com.mineinabyss.keepup.helpers.MSG
import com.mineinabyss.keepup.helpers.clearSymlinks
import com.mineinabyss.keepup.helpers.linkToDest
import com.mineinabyss.keepup.helpers.printToConsole
import com.mineinabyss.keepup.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

class PluginsCommand : CliktCommand(name = "plugins", help = "Syncs plugins from a hocon/json config") {
    init {
        context {
            autoEnvvarPrefix = "KEEPUP"
        }
    }

    // === Arguments ===
    val catalog by argument(help = "Path to the version catalog file").inputStream()

    val downloadPath by argument(help = "Path to download files to")
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)

    val dest by argument()
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)

    // === Options ===
    val jsonPath by option(help = "Path to the root object to download from, uses keys separated by .")
        .default("$")

    val fileType by option(help = "Type of file for the input stream")
        .enum<VersionCatalogType>()
        .default(VersionCatalogType.HOCON)

    val ignoreSimilar by option(help = "Don't create symlinks for files with matching characters before the first number")
        .flag(default = true)

    val failAllDownloads by option(help = "Don't actually download anything, useful for testing")
        .flag(default = false)

    val hideProgressBar by option(help = "Does not show progress bar if set to true")
        .flag(default = false)

    val overrideGithubRelease by option(help = "Force downloading the latest version of files from GitHub")
        .enum<GithubReleaseOverride>()
        .default(GithubReleaseOverride.NONE)

    val cacheExpirationTime: Duration by option()
        .convert { Duration.parse(it) }
        .default(10.minutes)

    val githubAuthToken: String? by option(help = "Used to access private repos or get a higher rate limit")

    val githubConfig by lazy {
        GithubConfig(
            githubAuthToken = githubAuthToken,
            overrideGithubRelease = overrideGithubRelease,
            cacheExpirationTime = cacheExpirationTime,
        )
    }

    override fun run() {
        val keepup = Keepup()
        val downloader = keepup.downloader(
            config = KeepupDownloaderConfig(
                downloadCache = downloadPath,
                ignoreSimilar = ignoreSimilar,
                failAllDownloads = failAllDownloads,
            ),
            githubConfig = githubConfig,
        )
        val parser = keepup.catalogParser()

        val sources = parser.parse(
            catalog = KeepupVersionCatalog(
                inputStream = catalog,
                type = fileType,
                include = JsonPath(jsonPath),
            ),
        )

        t.println("${MSG.info} Clearing symlinks")
        clearSymlinks(dest)

        t.println(
            "${MSG.info} Running Keepup on ${TextColors.yellow(sources.size.toString())} items" + if (jsonPath != "$") " from path ${
                TextColors.yellow(jsonPath)
            }" else ""
        )

        progressBarLayout {
            progressBar()
        }
        val progress = if (hideProgressBar) null else t.progressAnimation {
            text("Keepup!")
            percentage()
            progressBar()
            completed()
            timeRemaining()
        }
        progress?.updateTotal(sources.size.toLong())
        progress?.start()

        if (githubConfig.overrideGithubRelease != GithubReleaseOverride.NONE)
            t.println("${TextColors.yellow("[!]")} Overriding GitHub release versions to ${githubConfig.overrideGithubRelease}")

        val startTime = TimeSource.Monotonic.markNow()

        runBlocking(Dispatchers.IO) {
            val downloadResults = downloader.download(sources = sources.toTypedArray(), dest = dest)

            for (result in downloadResults) {
                if (result is DownloadResult.HasFiles) {
                    linkToDest(dest, result)
                }

                progress?.advance(1)
                result.printToConsole()
            }
            progress?.clear()
            progress?.stop()

            val elapsed = startTime.elapsedNow().toString(unit = DurationUnit.SECONDS, decimals = 2)
            t.println("${MSG.info} ${TextColors.brightGreen("done in $elapsed!")}")
        }
    }
}
