package com.drinkit.common

import kotlin.reflect.KClass

interface GenerateId {

    fun <Id: AbstractId> invoke(idType: KClass<Id>): Id
}