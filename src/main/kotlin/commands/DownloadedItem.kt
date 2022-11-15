package commands

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DownloadedItem(
    @SerialName("Path")
    val relativePath: String,
    @SerialName("Name")
    val name: String,
)
