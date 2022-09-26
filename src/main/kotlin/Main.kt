import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.inputStream
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.Option
import kotlinx.coroutines.ExperimentalCoroutinesApi

class Keepup : CliktCommand() {
    // make path the first argument
    val input by argument(help = "Path to the file").inputStream()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun run() {
        val conf = Configuration.builder()
            .options(Option.AS_PATH_LIST).build()
        val parsed = JsonPath.using(conf).parse(input)
        val items = parsed.read<List<Any?>>("\$..*")
        /*val keys = parsed.using(conf).read<List<Any?>>("\$..*")
            .filterIsInstance<String>()*/
        echo(items)
//        shell {
//            pipeline {
//
//            }
//        }
    }
}

fun main(args: Array<String>) = Keepup().main(args)
