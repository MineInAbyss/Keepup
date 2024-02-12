package downloading

interface Downloader {
    suspend fun download(): List<DownloadResult>
}
