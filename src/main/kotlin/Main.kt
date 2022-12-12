@file:Suppress("MemberVisibilityCanBePrivate")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import com.jayway.jsonpath.JsonPath
import kotlin.io.path.*

class Keepup : CliktCommand() {
    // === Arguments ===
    val input by argument(help = "Path to the file").inputStream()

    val downloadPath by argument(help = "Path to download files to")
        .path(mustExist = true, canBeFile = false)

    val dest by argument()
        .path(mustExist = true, canBeFile = false)

    // === Options ===
    val jsonPath by option(help = "JsonPath to the root value to keep")
        .default("$")

    val fileType by option(help = "Type of file for the input stream")
        .choice("json", "hocon")
        .default("hocon")

    val ignoreSimilar by option(help = "Don't create symlinks for files with matching characters before the first number")
        .flag(default = true)

    override fun run() {
        val jsonInput = if (fileType == "hocon") {
            println("Converting HOCON to JSON")
            renderHocon(input)
        } else input

        println("Parsing input")
        val parsed = JsonPath.parse(jsonInput)
        val items = parsed.read<Map<String, Any?>>(jsonPath)
        val strings = getLeafStrings(items)

        println("Clearing symlinks")
        clearSymlinks(dest)

        println("Creating new symlinks")
        strings.forEach { (key, source) ->
            val isolatedPath = (downloadPath / key).absolute()
            isolatedPath.createDirectories()
            val files = dest.listDirectoryEntries().filter { it.isRegularFile() }
            download(source, isolatedPath).forEach download@{ item ->
                if (ignoreSimilar && files.any { similar(item.name, it.name) }) {
                    println("Skipping ${item.name} because it is similar to an existing file")
                    return@download
                }
                linkToDest(dest, isolatedPath, item)
            }
        }
        println("Keepup done!")
    }
}

fun main(args: Array<String>) = Keepup().main(args)
