package config

import helpers.GithubReleaseOverride
import kotlin.time.Duration

class GithubConfig(
    val overrideGithubRelease: GithubReleaseOverride,
    val githubAuthToken: String?,
    val cacheExpirationTime: Duration?,
)
