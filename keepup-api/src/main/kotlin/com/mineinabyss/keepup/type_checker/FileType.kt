package com.mineinabyss.keepup.type_checker

sealed interface FileType {
    data object Archive : FileType
    data object HTML : FileType
    data class Other(val type: String) : FileType
}
