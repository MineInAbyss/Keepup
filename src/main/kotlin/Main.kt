@file:Suppress("MemberVisibilityCanBePrivate")

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors.brightGreen
import com.github.ajalt.mordant.rendering.TextColors.yellow
import com.github.ajalt.mordant.terminal.Terminal
import com.mineinabyss.keepup.config.ConfigTreeBuilder
import com.mineinabyss.keepup.config.KeepupConfigsInventory
import config.GithubConfig
import downloading.DownloadParser
import downloading.DownloadResult
import downloading.Source
import helpers.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

val keepup by lazy { Keepup() }
val t by lazy { Terminal() }

class Keepup : CliktCommand() {
    init {
        context {
            autoEnvvarPrefix = "KEEPUP"
        }
    }

    // === Arguments ===
    val input by argument(help = "Path to the file").inputStream()

    val downloadPath by argument(help = "Path to download files to")
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)

    val dest by argument()
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)

    // === Options ===
    val jsonPath by option(help = "Path to the root object to download from, uses keys separated by .")
        .default("$")

    val fileType by option(help = "Type of file for the input stream")
        .choice("json", "hocon")
        .default("hocon")

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

    val enableConfigs: Boolean by option(help = "Enable config synchronization features").flag(default = true)
    val configsTarget: String? by option(help = "Target server name for configs feature, used to figure out which configs to copy, etc...")
    val configsInventory by option(help = "Inventory file defining config options").path()
    val configsDestRoot by argument()
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)
        .optional()


    override fun run() {
        if (overrideGithubRelease != GithubReleaseOverride.NONE)
            t.println("${yellow("[!]")} Overriding GitHub release versions to $overrideGithubRelease")

        val startTime = TimeSource.Monotonic.markNow()

        val jsonInput = if (fileType == "hocon") {
            t.println("${MSG.info} Converting HOCON to JSON")
            renderHocon(input)
        } else input

        t.println("${MSG.info} Parsing input")
        val parsed = Json.parseToJsonElement(jsonInput.reader().use { it.readText() })
        val items = jsonPath.removePrefix("$").split(".").fold(parsed.jsonObject) { acc, key ->
            if (key == "") return@fold acc
            acc.getValue(key).jsonObject
        }
        val strings = getLeafStrings(items)

        t.println("${MSG.info} Clearing symlinks")
        clearSymlinks(dest)

        t.println(
            "${MSG.info} Running Keepup on ${yellow(strings.size.toString())} items" + if (jsonPath != "$") " from path ${
                yellow(jsonPath)
            }" else ""
        )

        val progress = if (hideProgressBar) null else t.progressAnimation {
            text("Keepup!")
            percentage()
            progressBar()
            completed()
            timeRemaining()
        }
        progress?.updateTotal(strings.size.toLong())
        progress?.start()

        runBlocking(Dispatchers.IO) {
            val channel = Channel<DownloadResult>()
            launch {
                HttpClient(CIO) {
                    install(HttpTimeout)
                }.use { client ->
                    val similarFileChecker = if (ignoreSimilar) SimilarFileChecker(dest) else null
                    val downloader = DownloadParser(
                        client,
                        githubConfig,
                        similarFileChecker
                    )

                    strings.map { (key, downloadQuery) ->
                        val downloadPathForKey = (downloadPath / key).absolute()
                        downloadPathForKey.createDirectories()
                        launch {
                            downloader.download(Source(key, downloadQuery), downloadPathForKey)
                                .forEach { channel.send(it) }
                            progress?.advance(1)
                        }
                    }.joinAll()
                }
                channel.close()
            }
            for (result in channel) {
                if (result is DownloadResult.HasFiles) {
                    linkToDest(dest, result)
                }
                result.printToConsole()
            }
            progress?.clear()
            progress?.stop()

            if (enableConfigs) {
                val target = configsTarget
                val invPath = configsInventory
                val destRoot = configsDestRoot
                if (target == null || invPath == null || destRoot == null) {
                    t.println("${MSG.error} Configs feature requires target and inventory to be set")
                } else {
                    t.println("${MSG.info} Configs feature enabled!")
                    val inventory = Yaml.default.decodeFromStream<KeepupConfigsInventory>(invPath.inputStream())
                    val paths = (inventory.targets[configsTarget] ?: emptyList())
                        .map { invPath / it / "sync" }
                    val tree = ConfigTreeBuilder()
                    val trackedFiles = tree.destFilesForRoots(paths)
                    t.println("${MSG.info} Deleting untracked files in destination: ${inventory.deleteUntracked}")
                    inventory.deleteUntracked.forEach { deletePath ->
                        tree.deleteUntrackedFor(destRoot, destRoot / deletePath, trackedFiles)
                    }
                }
            }

            val elapsed = startTime.elapsedNow().toString(unit = DurationUnit.SECONDS, decimals = 2)
            t.println("${MSG.info} ${brightGreen("done in $elapsed!")}")
        }
    }
}

fun main(args: Array<String>) = keepup.main(args)
