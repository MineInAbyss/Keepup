package com.mineinabyss.keepup.downloads.github

import com.mineinabyss.keepup.downloads.parsing.DownloadSource

data class GithubArtifact(
    val source: DownloadSource,
    val repo: String,
    val releaseVersion: String,
    val regex: String,
) {
    val calculatedRegex = regex.toRegex()

    companion object {
        fun from(source: DownloadSource): GithubArtifact {
            val (repo, release, grep) = source.query.removePrefix("github:").split(":")
            return GithubArtifact(source, repo, release, grep)
        }
    }
}
