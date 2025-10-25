package com.drinkit.configuration.infra

import com.drinkit.configuration.Configurations

internal class InMemoryConfigurationsTest : ConfigurationsTestContract() {

    override fun fetchRepository(): Configurations = InMemoryConfigurations()
}