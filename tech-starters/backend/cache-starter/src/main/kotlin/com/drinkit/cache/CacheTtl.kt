package com.drinkit.cache

import com.drinkit.documentation.tech.starter.TechStarterTool
import java.time.Duration

/**
 * The time to live a module gives the entries of its cache, declared as a bean. `starters.cache.ttl.<cache>` of the
 * application overrides it.
 */
@TechStarterTool
data class CacheTtl(val cacheName: String, val ttl: Duration)
