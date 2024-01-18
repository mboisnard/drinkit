package com.drinkit.upgrader

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UpgraderApplication

fun main(args: Array<String>) {
    runApplication<UpgraderApplication>(*args)
}