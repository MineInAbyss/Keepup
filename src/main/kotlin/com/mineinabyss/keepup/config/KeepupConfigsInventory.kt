package com.mineinabyss.keepup.config

import kotlinx.serialization.Serializable

@Serializable
data class KeepupConfigsInventory(
    val deleteUntracked: List<String>,
    val targets: Map<String, List<String>>
)
