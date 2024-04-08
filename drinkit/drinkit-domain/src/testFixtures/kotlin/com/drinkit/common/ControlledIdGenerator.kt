package com.drinkit.common

class ControlledIdGenerator : IdGenerator {

    var idToGenerate: String? = null

    override fun createNewId(): String =
        idToGenerate ?: "659ee3164b1d53340c4f7608"
}