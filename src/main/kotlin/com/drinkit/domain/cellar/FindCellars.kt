package com.drinkit.domain.cellar

import io.github.oshai.kotlinlogging.KotlinLogging

data class FindCellars(
    private val cellarRepository: CellarRepository,
) {
    private val logger = KotlinLogging.logger { }


}
