package com.drinkit.idempotency

import com.drinkit.documentation.tech.starter.TechStarterTool

/**
 * Runs the endpoint once per `Idempotency-Key` header of its client: a retry gets the first answer back. Goes on
 * the controller's method, the override of the interface generated from the API contract included. The endpoint
 * runs in one transaction with its key, so its writes and its answer commit together.
 */
@TechStarterTool
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Idempotent
