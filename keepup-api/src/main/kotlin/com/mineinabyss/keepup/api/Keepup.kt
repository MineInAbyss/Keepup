package com.mineinabyss.keepup.api

import com.mineinabyss.keepup.config_sync.Inventory
import com.mineinabyss.keepup.downloads.github.GithubConfig
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import java.nio.file.Path

data class KeepupDownloaderConfig(
    val downloadCache: Path,
    val ignoreSimilar: Boolean = true,
    val failAllDownloads: Boolean = false,
)

data class Keepup(
    val http: HttpClient = HttpClient(CIO) {
        install(HttpTimeout)
    },
) {
    fun catalogParser() = KeepupVersionsCatalogParser()

    fun downloader(
        config: KeepupDownloaderConfig,
        githubConfig: GithubConfig = GithubConfig(),
    ) = KeepupDownloader(
        http = http,
        config = config,
        githubConfig = githubConfig
    )

    fun configSync(inventory: Inventory) = KeepupConfigSync(
        inventory = inventory,
    )
}
