@file:Suppress("MemberVisibilityCanBePrivate")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.arguments.argument
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
import com.jayway.jsonpath.JsonPath
import config.GithubConfig
import downloading.DownloadParser
import downloading.DownloadResult
import downloading.Source
import helpers.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

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
    val jsonPath by option(help = "JsonPath to the root value to keep")
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

    override fun run() {
        if (overrideGithubRelease != GithubReleaseOverride.NONE)
            t.println("${yellow("[!]")} Overriding GitHub release versions to $overrideGithubRelease")

        val jsonInput = if (fileType == "hocon") {
            t.println("Converting HOCON to JSON")
            renderHocon(input)
        } else input

        t.println("Parsing input")
        val parsed = JsonPath.parse(jsonInput)
        val items = parsed.read<Map<String, Any?>>(jsonPath)
        val strings = getLeafStrings(items)

        t.println("Clearing symlinks")
        clearSymlinks(dest)
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
                HttpClient(CIO).use { client ->
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
            t.println(brightGreen("Keepup done!"))
        }
    }
}

fun main(args: Array<String>) = keepup.main(args)
