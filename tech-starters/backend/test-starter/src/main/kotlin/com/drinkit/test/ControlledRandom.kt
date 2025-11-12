package com.drinkit.test

import com.drinkit.documentation.tech.starter.TechStarterTool
import kotlin.random.Random

@TechStarterTool
object ControlledRandom {

    private const val SEED = 42

    val value = Random(SEED)
}