package com.mineinabyss.keepup

import com.mineinabyss.keepup.api.Keepup
import com.mineinabyss.keepup.config_sync.Inventory
import org.junit.Test
import kotlin.io.path.*
import kotlin.test.assertEquals

class ConfigCopyTest {
    @Test
    fun `should pass integration test with expected file structure`() {
        val keepup = Keepup()
        val tempDir = createTempDirectory("keepup")
        val inventoryFile = Path("src/test/resources/inventory.yml")
        val configsRoot = Path("src/test/resources/configs-source")
        val dest = tempDir / "destRoot"
        val shouldMatch = Path("src/test/resources/expected-output")
        keepup.configSync(
            inventory = Inventory.from(inventoryFile.inputStream())
        ).sync(
            host = "example-host",
            configsRoot = configsRoot,
            templateCacheDir = tempDir / "cacheDir",
            destRoot = dest,
        )

        println("Output: $dest")
        assertEquals(
            shouldMatch.walk()
                .map { it.relativeTo(shouldMatch) }
                .sorted().toList(),
            dest.walk()
                .map { it.relativeTo(dest) }
                .sorted().toList()
        )
    }
}
