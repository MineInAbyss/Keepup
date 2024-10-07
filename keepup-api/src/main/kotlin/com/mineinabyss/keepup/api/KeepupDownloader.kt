package com.mineinabyss.keepup.api

import com.mineinabyss.keepup.downloads.DownloadResult
import com.mineinabyss.keepup.downloads.github.GithubConfig
import com.mineinabyss.keepup.downloads.parsing.DownloadParser
import com.mineinabyss.keepup.downloads.parsing.DownloadSource
import com.mineinabyss.keepup.similarfiles.SimilarFileChecker
import io.ktor.client.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.div

class KeepupDownloader(
    val http: HttpClient,
    val config: KeepupDownloaderConfig,
    val githubConfig: GithubConfig,
) {
    suspend fun download(
        vararg sources: DownloadSource,
        dest: Path,
    ): Channel<DownloadResult> = coroutineScope {
        val channel = Channel<DownloadResult>(UNLIMITED)
        launch(Dispatchers.IO) {
            http.use { client ->
                val similarFileChecker = if (config.ignoreSimilar) SimilarFileChecker(dest) else null
                val downloader = DownloadParser(
                    failAllDownloads = config.failAllDownloads,
                    client = client,
                    githubConfig = githubConfig,
                    similarFileChecker = similarFileChecker,
                )

                sources.map { source ->
                    val downloadPathForKey = (config.downloadCache / source.keyInConfig).absolute()
                    downloadPathForKey.createDirectories()
                    launch {
                        downloader
                            .download(source, downloadPathForKey)
                            .forEach { channel.trySend(it) }
                    }
                }.joinAll()
                channel.close()
            }
        }
        channel
    }
}
