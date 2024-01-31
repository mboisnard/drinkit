package com.drinkit

import io.github.serpro69.kfaker.Faker
import io.github.serpro69.kfaker.FakerConfig
import io.github.serpro69.kfaker.fakerConfig

val fakerConfig: FakerConfig = fakerConfig {
    randomSeed = 42L
}

val faker = Faker(fakerConfig)