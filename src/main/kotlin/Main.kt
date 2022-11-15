import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import com.jayway.jsonpath.JsonPath
import commands.DownloadedItem
import commands.Rclone
import commands.Wget
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.div

class Keepup : CliktCommand() {
    // make path the first argument
    val input by argument(help = "Path to the file").inputStream()
    val jsonPath by option(help = "JsonPath to the root value to keep")
        .default("$")
    val downloadPath by argument(help = "Path to download files to")
        .path(mustExist = true, canBeFile = false)
    val dest by argument()
        .path(mustExist = true, canBeFile = false)

    fun download(source: String, targetDir: Path): List<DownloadedItem> = when {
        source == "." -> emptyList()
        source.matches("^https?://.*".toRegex()) -> listOfNotNull(Wget(source, targetDir))
        else -> Rclone.sync(source, targetDir)
    }

    override fun run() {
        val parsed = JsonPath.parse(input)
        val items = parsed.read<Map<String, Any?>>(jsonPath)
        // Fold leaf strings into a list
        val strings = getLeafStrings(items)

        println("Clearing symlinks")
        clearSymlinks(dest)

        println("Creating new symlinks")
        strings.forEach { (key, source) ->
            val isolatedPath = (downloadPath / key).absolute()
            isolatedPath.createDirectories()
            download(source, isolatedPath).forEach {
                (dest / it.name).createSymbolicLinkPointingTo(isolatedPath / it.relativePath)
            }
        }
    }
}

fun main(args: Array<String>) {
    Keepup().main(args)
}
