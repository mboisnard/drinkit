package com.drinkit.configuration

internal class InMemoryConfigurationsTest : ConfigurationsTestContract() {

    override fun fetchConfigurations(): Configurations = InMemoryConfigurations()
}
