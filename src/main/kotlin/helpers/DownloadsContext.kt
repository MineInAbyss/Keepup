package helpers

import java.nio.file.Path

class DownloadsContext(
    val targetDir: Path,
    val forceLatest: Boolean,
)
