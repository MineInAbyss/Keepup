import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import com.jayway.jsonpath.JsonPath
import eu.jrie.jetbrains.kotlinshell.shell.Shell
import eu.jrie.jetbrains.kotlinshell.shell.shell
import kotlinx.coroutines.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalCoroutinesApi::class)
class Keepup : CliktCommand() {
    // make path the first argument
    val input by argument(help = "Path to the file").inputStream()
    val jsonPath by argument(help = "JsonPath to the root value to keep")
    val downloadPath by argument(help = "Path to download files to")
        .path(mustExist = true, canBeFile = false)
    val dest by argument()
        .path(mustExist = true, canBeFile = false)

    override fun run() {
//        val conf = Configuration.builder()
//            .options(Option.AS_PATH_LIST).build()
        val parsed = JsonPath.parse(input)
        val items = parsed.read<Map<String, Any?>>("$")
        // Fold leaf strings into a list
        val strings = getLeafStrings(items)

        clearSymlinks(dest)
        shell {
            strings.forEach { (key, value) ->
                downloadPath / key
            }
        }
    }

    suspend fun Shell.download(path: String): Path? {
        when {
            path == "." -> return null
            path.matches("^https?://.*".toRegex()) -> "wget $path -P $downloadPath"()
            else -> "rclone sync $path $downloadPath"()
        }
    }

    fun clearSymlinks(path: Path) {
        // For each file in the directory
        path.listDirectoryEntries().forEach {
            if(!it.isDirectory() && it.isSymbolicLink()) it.deleteIfExists()
        }
    }

    fun getLeafStrings(map: Map<String, Any?>, acc: MutableMap<String, String> = mutableMapOf()): Map<String, String> {
        map.entries.forEach { (key, value) ->
            when (value) {
                is String -> acc[key] = value
                is Map<*, *> -> getLeafStrings(value as Map<String, Any?>, acc)
            }
        }
        return acc
    }
}

fun main(args: Array<String>) = Keepup().main(args)
