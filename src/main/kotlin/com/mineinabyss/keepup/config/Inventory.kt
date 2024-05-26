package com.mineinabyss.keepup.config

import kotlinx.serialization.Serializable

@Serializable
data class Inventory(
    val files: Map<String, FileConfig>,
    val targets: Map<String, List<String>>
)

@Serializable
data class FileConfig(
    val deleteUntracked: Boolean = false,
    val keep: List<String> = emptyList()
)
