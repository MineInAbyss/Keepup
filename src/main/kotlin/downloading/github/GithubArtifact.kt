package downloading.github

import downloading.Source

data class GithubArtifact(
    val source: Source,
    val repo: String,
    val releaseVersion: String,
    val regex: String,
) {
    val calculatedRegex = regex.toRegex()

    companion object {
        fun from(source: Source): GithubArtifact {
            val (repo, release, grep) = source.query.removePrefix("github:").split(":")
            return GithubArtifact(source, repo, release, grep)
        }
    }
}
