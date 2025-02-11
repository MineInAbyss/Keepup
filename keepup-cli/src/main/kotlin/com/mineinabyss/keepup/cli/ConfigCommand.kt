package com.mineinabyss.keepup.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import com.mineinabyss.keepup.api.Keepup
import com.mineinabyss.keepup.config_sync.Inventory
import com.mineinabyss.keepup.config_sync.templating.Templater
import com.mineinabyss.keepup.helpers.MSG
import com.mineinabyss.keepup.t
import kotlin.io.path.inputStream

class ConfigCommand : CliktCommand(name = "config") {
    override fun help(context: Context) = "Syncs local config files to appropriate destinations"

    val include by argument(
        "include",
        help = "The config defined in inventory to sync"
    )

    val inventoryFile by option(
        "-i", "--inventory",
        help = "Inventory file defining config options"
    )
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .required()

    val sourceRoot by option(
        "-s",
        "--source",
        help = "Directory containing source configs to sync, defaults to directory of inventory"
    )
        .path(mustExist = true, canBeFile = false, mustBeReadable = true)
        .defaultLazy { inventoryFile.parent }

    val destRoot by option("-d", "--dest", help = "Directory to sync configs to")
        .path(mustExist = true, canBeFile = false, mustBeWritable = true)
        .required()

    val templateCacheDir by option(
        "-t", "--template-cache",
        help = "Directory to cache template results, if unspecified will not templates .peb files"
    )
        .path(mustExist = false, canBeFile = false)

    override fun run() {
        t.println("${MSG.info} Running config sync for $include...")
        val keepup = Keepup()
        val templater = Templater()
        keepup.configSync(
            inventory = Inventory.from(templater, inventoryFile.inputStream())
        ).sync(
            host = include,
            configsRoot = sourceRoot,
            templateCacheDir = templateCacheDir,
            destRoot = destRoot
        )
    }
}
