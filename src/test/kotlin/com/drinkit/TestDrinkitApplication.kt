package com.drinkit

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.with

@TestConfiguration(proxyBeanMethods = false)
class TestDrinkitApplication

fun main(args: Array<String>) {
	fromApplication<DrinkitApplication>().with(TestDrinkitApplication::class).run(*args)
}
