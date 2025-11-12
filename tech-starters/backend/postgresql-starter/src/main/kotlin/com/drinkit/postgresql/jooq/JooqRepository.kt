package com.drinkit.postgresql.jooq

import com.drinkit.documentation.tech.starter.TechStarterTool
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Marks a PostgreSQL repository with MANDATORY transaction propagation.
 *
 * IMPORTANT: All methods MUST be called within an existing transaction.
 *
 * Usage:
 * - Use cases (domain layer) MUST be annotated with @Transactional
 * - Tests MUST use @Transactional or configure a transaction manager
 *
 * This enforces that transactions are managed at the use case level,
 * never at the repository level.
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@TechStarterTool
annotation class JooqRepository