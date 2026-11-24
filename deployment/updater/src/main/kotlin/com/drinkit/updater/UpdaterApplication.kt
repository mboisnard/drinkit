package com.drinkit.updater

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UpdaterApplication

fun main(args: Array<String>) {
    // A changelog parameter left unset stops the run, rather than reaching the database as its literal `${NAME}`
    System.setProperty("liquibase.missingPropertyMode", "ERROR")

    runApplication<UpdaterApplication>(*args)
}
