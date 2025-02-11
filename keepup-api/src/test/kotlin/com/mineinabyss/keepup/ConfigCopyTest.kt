package com.mineinabyss.keepup

import com.mineinabyss.keepup.api.Keepup
import com.mineinabyss.keepup.config_sync.Inventory
import com.mineinabyss.keepup.config_sync.templating.Templater
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
        val templater = Templater()

        keepup.configSync(
            inventory = Inventory.from(templater, inventoryFile.inputStream(), environment = mapOf("TEST_VAR" to "world")),
        ).sync(
            host = "example-host",
            configsRoot = configsRoot,
            templateCacheDir = tempDir / "cacheDir",
            destRoot = dest,
        )

        println("Output: $dest")
        val expectedFiles = shouldMatch.walk()
            .map { it.relativeTo(shouldMatch) }
            .sorted().toList()
        val actualFiles = dest.walk()
            .map { it.relativeTo(dest) }
            .sorted().toList()
        assertEquals(expectedFiles, actualFiles)
        expectedFiles.indices.forEach {
            val expected = shouldMatch / expectedFiles[it]
            val actual = dest / actualFiles[it]
            assertEquals(expected.readText(), actual.readText())
        }
    }
}
