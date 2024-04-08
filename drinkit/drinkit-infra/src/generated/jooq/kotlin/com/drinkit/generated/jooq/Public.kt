/*
 * This file is generated by jOOQ.
 */
package com.drinkit.generated.jooq


import com.drinkit.generated.jooq.tables.Cellar
import com.drinkit.generated.jooq.tables.User
import com.drinkit.generated.jooq.tables.VerificationToken

import kotlin.collections.List

import org.jooq.Catalog
import org.jooq.Table
import org.jooq.impl.SchemaImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class Public : SchemaImpl("public", DefaultCatalog.DEFAULT_CATALOG) {
    public companion object {

        /**
         * The reference instance of <code>public</code>
         */
        val PUBLIC: Public = Public()
    }

    /**
     * The table <code>public.cellar</code>.
     */
    val CELLAR: Cellar get() = Cellar.CELLAR

    /**
     * The table <code>public.user</code>.
     */
    val USER: User get() = User.USER

    /**
     * The table <code>public.verification_token</code>.
     */
    val VERIFICATION_TOKEN: VerificationToken get() = VerificationToken.VERIFICATION_TOKEN

    override fun getCatalog(): Catalog = DefaultCatalog.DEFAULT_CATALOG

    override fun getTables(): List<Table<*>> = listOf(
        Cellar.CELLAR,
        User.USER,
        VerificationToken.VERIFICATION_TOKEN
    )
}
