package commands

import evalBash
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class CachedCommand(val command: String, val path: Path, val expiration: Duration? = null) {
    class Result(
        val wasCached: Boolean,
        val result: String,
    )

    fun getFromCacheOrEval(): Result {
        val expirationPath = path.parent / (path.name + ".expiration")
        // get current time
        val time = LocalDateTime.now()
        val expiryDate = expirationPath.takeIf { it.exists() }?.readText()?.let { LocalDateTime.parse(it) }

        if (path.exists() && (expiryDate == null || time < expiryDate)) {
            return Result(true, path.readText())
        }

        val evaluated = command.evalBash(env = mapOf())
            .onFailure {
                throw IllegalStateException("Failed to evaluate command $command, result:\n${this.stderr.joinToString("\n")}")
            }
            .getOrThrow()
        path.deleteIfExists()
        path.createParentDirectories().createFile().writeText(evaluated)
        // write expiration date
        if (expiration != null) {
            expirationPath.deleteIfExists()
            expirationPath.createFile().writeText((time + expiration.toJavaDuration()).toString())
        }
        return Result(false, evaluated)
    }
}
