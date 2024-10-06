package com.mineinabyss.keepup.downloads.github

import kotlin.time.Duration

data class GithubConfig(
    val overrideGithubRelease: GithubReleaseOverride = GithubReleaseOverride.NONE,
    val githubAuthToken: String? = null,
    val cacheExpirationTime: Duration? = null,
)
