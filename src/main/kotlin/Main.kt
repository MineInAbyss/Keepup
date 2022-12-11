import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import com.jayway.jsonpath.JsonPath
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import commands.DownloadedItem
import commands.Rclone
import commands.Wget
import java.nio.file.Path
import kotlin.io.path.*

class Keepup : CliktCommand() {
    // make path the first argument
    val input by argument(help = "Path to the file").inputStream()
    val jsonPath by option(help = "JsonPath to the root value to keep")
        .default("$")
    val fileType by option().choice("json", "hocon").default("hocon")
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
        val jsonInput = if(fileType == "hocon") {
            println("Converting HOCON to JSON")
            val config = ConfigFactory.parseReader(input.reader()).resolve()
            // Convert config to json
            config.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false)).byteInputStream()
        } else input

        println("Parsing input")
        val parsed = JsonPath.parse(jsonInput)
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
                (dest / it.name).createSymbolicLinkPointingTo((isolatedPath / it.relativePath).relativeTo(dest.absolute()))
            }
        }
        println("Keepup done!")
    }
}

fun main(args: Array<String>) {
    Keepup().main(args)
}
