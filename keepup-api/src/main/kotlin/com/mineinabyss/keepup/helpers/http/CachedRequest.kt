package com.mineinabyss.keepup.helpers.http

import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class CachedRequest(val path: Path, val expiration: Duration? = null, val evaluate: suspend () -> Result<String>) {
    class Returned(
        val wasCached: Boolean,
        val result: String,
    )

    suspend fun getFromCacheOrEval(): Result<Returned> {
        val expirationPath = path.parent / (path.name + ".expiration")
        // get current time
        val time = LocalDateTime.now()
        val expiryDate = expirationPath.takeIf { it.exists() }?.readText()?.let { LocalDateTime.parse(it) }

        if (path.exists() && (expiryDate == null || time < expiryDate)) {
            return Result.success(Returned(true, path.readText()))
        }

        val evaluated = evaluate()
        evaluated.onSuccess {
            path.deleteIfExists()
            path.createParentDirectories().createFile().writeText(it)
        }
        // write expiration date
        if (expiration != null) {
            expirationPath.deleteIfExists()
            expirationPath.createFile().writeText((time + expiration.toJavaDuration()).toString())
        }
        return evaluated.map { Returned(false, it) }
    }
}
