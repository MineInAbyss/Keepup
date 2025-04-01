package com.mineinabyss.keepup.cli
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.mineinabyss.keepup.t
import kotlin.io.path.*

class PullCommand : CliktCommand(name = "pull"){
    override fun help(context: Context): String = "Pulls a github repository or clones it if it has not been previously pulled."

    val repo by argument(help = "Github repository to pull, specified as 'owner/repo_name'")

    val branch by argument(help = "Branch to pull").optional()

    val destRoot by option("-d", "--dest", help = "Will place repo as a directory inside this directory.")
        .path(mustExist = false, canBeFile = false, mustBeWritable = false)
        .required()

    val keyPath by option("-k", "--key-path", help = "Path to SSH key used to access private repos or get a higher rate limit")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)

    val dontStashChanges by option("-n", "--no-stash", help = "Don't stash changes before pulling. If enabled, may fail pull if unable to merge.")
        .flag(default = false)

    fun runProcess(vararg command: String) {
        ProcessBuilder()
            .command(*command)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
    }

    override fun run() {
        val repoDir = destRoot.resolve(repo.split("/")[1])
        t.println("Pulling files from repo: $repo, branch: ${branch?: "default"}, to directory: $repoDir.")

        var sshString: String
        if (keyPath != null) {
            sshString = "core.sshCommand=ssh -i ${keyPath!!.toAbsolutePath()}"
        } else {
            sshString = ""
        }

        var checkoutCommand: Array<String>
        if (branch != null) {
            checkoutCommand = arrayOf("git", "-C", repoDir.toAbsolutePath().toString(), "checkout", branch!!)
        } else {
            checkoutCommand = emptyArray()
        }

        if (!repoDir.exists()) {
            // Must clone if directory doesn't exist
            t.println("Repository doesn't exist, cloning into $repoDir instead.")
            repoDir.createDirectories()
            runProcess("git", "clone", "-c", sshString, "git@github.com:${repo}.git", repoDir.toAbsolutePath().toString())
            if (branch != null) { // Checkout desired branch
                runProcess(*checkoutCommand)
                runProcess("git", "-C", repoDir.toAbsolutePath().toString(), "pull") // Pull branch, checkout only makes local copy
            }
        } else {
            if (!dontStashChanges) {
                runProcess(
                    "git",
                    "-C",
                    repoDir.toAbsolutePath().toString(),
                    "stash"
                ) // Stash first to discard local changes
            }
            runProcess(*checkoutCommand)
            runProcess("git", "-C", repoDir.toAbsolutePath().toString(), "pull")
        }
    }
}