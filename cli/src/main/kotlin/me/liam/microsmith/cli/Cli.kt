package me.liam.microsmith.cli

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    exitProcess(MicrosmithCli().run(args))
}
