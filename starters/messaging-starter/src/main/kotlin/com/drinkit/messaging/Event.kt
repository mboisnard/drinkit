package com.drinkit.messaging


// Enforce relationship between interface and subclasses/implementations with Curiously Recurring Generic Pattern
interface Event<Type : Event<Type>>
