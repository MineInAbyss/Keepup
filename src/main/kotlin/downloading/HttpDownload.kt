package downloading

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import java.nio.file.Path
import kotlin.io.path.*

class HttpDownload(
    val client: HttpClient,
    val source: Source,
    val targetDir: Path,
    val fileName: String = source.query.substringAfterLast("/"),
) : Downloader {
    override suspend fun download(): List<DownloadResult> {
        val cacheFile = targetDir.resolve("$fileName.cache")
        val targetFile = targetDir.resolve(fileName)
        val headers = client.head(source.query).headers
        val lastModified = headers["Last-Modified"]?.fromHttpToGmtDate()
        val length = headers["Content-Length"]?.toLongOrNull()

        val cache = "Last-Modified: $lastModified, Content-Length: $length"
        if (targetFile.exists() && cacheFile.exists() && cacheFile.readText() == cache)
            return listOf(DownloadResult.SkippedBecauseCached(targetFile, source.keyInConfig))
        cacheFile.deleteIfExists()
        cacheFile.createFile().writeText(cache)

        client.get(source.query)
            .bodyAsChannel()
            .copyAndClose(targetFile.toFile().writeChannel())

        return listOf(DownloadResult.Downloaded(targetFile, source.keyInConfig))
    }
}
