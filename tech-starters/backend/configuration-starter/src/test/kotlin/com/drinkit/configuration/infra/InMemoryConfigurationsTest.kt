package com.drinkit.configuration.infra

import com.drinkit.configuration.Configurations

internal class InMemoryConfigurationsTest : ConfigurationsTestContract() {

    override fun fetchConfigurations(): Configurations = InMemoryConfigurations()
}
