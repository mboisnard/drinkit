/*
 * This file is generated by jOOQ.
 */
package com.drinkit.security.generated.jooq.tables.records


import com.drinkit.security.generated.jooq.tables.Role

import org.jooq.impl.TableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class RoleRecord private constructor() : TableRecordImpl<RoleRecord>(Role.ROLE) {

    open var userId: String
        set(value): Unit = set(0, value)
        get(): String = get(0) as String

    open var authority: String
        set(value): Unit = set(1, value)
        get(): String = get(1) as String

    /**
     * Create a detached, initialised RoleRecord
     */
    constructor(userId: String, authority: String): this() {
        this.userId = userId
        this.authority = authority
        resetChangedOnNotNull()
    }
}
