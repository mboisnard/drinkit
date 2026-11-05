package com.drinkit.config

import java.util.Locale

abstract class AbstractApi {

    fun locale(): Locale = Locale.FRENCH
}
