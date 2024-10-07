package com.mineinabyss.keepup.api

import com.mineinabyss.keepup.downloads.DownloadResult
import com.mineinabyss.keepup.downloads.github.GithubConfig
import com.mineinabyss.keepup.downloads.parsing.DownloadParser
import com.mineinabyss.keepup.downloads.parsing.DownloadSource
import com.mineinabyss.keepup.similarfiles.SimilarFileChecker
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun download(
        vararg sources: DownloadSource,
        dest: Path,
        scope: CoroutineScope,
    ): ReceiveChannel<DownloadResult> = scope.produce(Dispatchers.IO) {
        val similarFileChecker = if (config.ignoreSimilar) SimilarFileChecker(dest) else null
        val downloader = DownloadParser(
            failAllDownloads = config.failAllDownloads,
            client = http,
            githubConfig = githubConfig,
            similarFileChecker = similarFileChecker,
        )

        sources.map { source ->
            val downloadPathForKey = (config.downloadCache / source.keyInConfig).absolute()
            downloadPathForKey.createDirectories()
            launch {
                downloader
                    .download(source, downloadPathForKey)
                    .forEach { channel.send(it) }
            }
        }
    }
}

//
//@OptIn(ExperimentalCoroutinesApi::class)
//suspend fun main() = coroutineScope {
//    val channel = produce<Int> {
//        repeat(20) {
//            launch(Dispatchers.IO) {
//                delay(Random.nextLong(3000))
//                channel.send(1)
//            }
//        }
//    }
//    for(i in channel) {
//        println(i)
//    }
//}
