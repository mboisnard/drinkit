package com.drinkit.test

import kotlin.random.Random

object ControlledRandom {

    private const val SEED = 42

    val value = Random(SEED)
}