package com.mineinabyss.keepup.api

import com.mineinabyss.keepup.downloads.parsing.DownloadSource
import com.mineinabyss.keepup.helpers.MSG
import com.mineinabyss.keepup.helpers.getLeafStrings
import com.mineinabyss.keepup.helpers.renderHocon
import com.mineinabyss.keepup.t
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.InputStream

enum class CatalogType {
    JSON,
    HOCON,
}

data class JsonPath(val path: String) {
    val parts = path.removePrefix("$").split(".")

    fun read(json: JsonElement): JsonObject = parts
        .fold(json.jsonObject) { acc, key ->
            if (key == "") return@fold acc
            acc.getValue(key).jsonObject
        }
}

data class KeepupVersionCatalog(
    val inputStream: InputStream,
    val type: CatalogType,
    val include: JsonPath,
)

class KeepupVersionsCatalogParser(
//    val githubConfig: GithubConfig = GithubConfig(),
) {
    fun parse(
        catalog: KeepupVersionCatalog,
    ): List<DownloadSource> {

        val jsonInputStream = when (catalog.type) {
            CatalogType.HOCON -> {
                t.println("${MSG.info} Converting HOCON to JSON")
                renderHocon(catalog.inputStream)
            }

            CatalogType.JSON -> catalog.inputStream
        }

        t.println("${MSG.info} Parsing input")
        val parsed = Json.parseToJsonElement(jsonInputStream.reader().use { it.readText() })
        val items = catalog.include.read(parsed)
        val strings = getLeafStrings(items)

        return strings.map { (key, value) -> DownloadSource(key, value) }
    }
}
