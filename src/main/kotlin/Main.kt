import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import com.jayway.jsonpath.JsonPath
import com.lordcodes.turtle.shellRun

class Keepup : CliktCommand() {
    // make path the first argument
    val input by argument(help = "Path to the file").inputStream()
    val jsonPath by argument(help = "JsonPath to the root value to keep")
    val downloadPath by argument(help = "Path to download files to")
        .path(mustExist = true, canBeFile = false)
    val dest by argument()
        .path(mustExist = true, canBeFile = false)

    fun download(source: String, target: String) {
        when {
            source == "." -> return
            source.matches("^https?://.*".toRegex()) -> Wget(source, target)
            else -> Rclone.sync(source, target)
        }
    }

    override fun run() {
        val parsed = JsonPath.parse(input)
        val items = parsed.read<Map<String, Any?>>("$")
        // Fold leaf strings into a list
        val strings = getLeafStrings(items)

        clearSymlinks(dest)
//        shell {
//            strings.forEach { (key, source) ->
//                val isolatedPath = downloadPath / key
//                Rclone.sync(source, isolatedPath.toString())
//            }
//        }
    }
}

fun main(args: Array<String>) {
    println(shellRun("ls"))
}//Keepup().main(args)
