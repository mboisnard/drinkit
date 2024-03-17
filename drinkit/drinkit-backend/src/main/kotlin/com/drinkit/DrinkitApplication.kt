package com.drinkit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DrinkitApplication

fun main(args: Array<String>) {
    runApplication<DrinkitApplication>(*args)
}
