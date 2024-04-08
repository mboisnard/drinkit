/*
 * This file is generated by jOOQ.
 */
package com.drinkit.generated.jooq.tables.records


import com.drinkit.generated.jooq.tables.User

import java.time.LocalDate
import java.time.LocalDateTime

import org.jooq.Record1
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class UserRecord private constructor() : UpdatableRecordImpl<UserRecord>(User.USER) {

    open var id: String
        set(value): Unit = set(0, value)
        get(): String = get(0) as String

    open var firstname: String?
        set(value): Unit = set(1, value)
        get(): String? = get(1) as String?

    open var lastname: String?
        set(value): Unit = set(2, value)
        get(): String? = get(2) as String?

    open var birthdate: LocalDate?
        set(value): Unit = set(3, value)
        get(): LocalDate? = get(3) as LocalDate?

    open var email: String
        set(value): Unit = set(4, value)
        get(): String = get(4) as String

    open var password: String
        set(value): Unit = set(5, value)
        get(): String = get(5) as String

    open var lastconnection: LocalDateTime?
        set(value): Unit = set(6, value)
        get(): LocalDateTime? = get(6) as LocalDateTime?

    open var status: String
        set(value): Unit = set(7, value)
        get(): String = get(7) as String

    open var completed: Boolean
        set(value): Unit = set(8, value)
        get(): Boolean = get(8) as Boolean

    open var enabled: Boolean
        set(value): Unit = set(9, value)
        get(): Boolean = get(9) as Boolean

    open var roles: Array<String?>?
        set(value): Unit = set(10, value)
        get(): Array<String?>? = get(10) as Array<String?>?

    open var modified: LocalDateTime
        set(value): Unit = set(11, value)
        get(): LocalDateTime = get(11) as LocalDateTime

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<String?> = super.key() as Record1<String?>

    /**
     * Create a detached, initialised UserRecord
     */
    constructor(id: String, firstname: String? = null, lastname: String? = null, birthdate: LocalDate? = null, email: String, password: String, lastconnection: LocalDateTime? = null, status: String, completed: Boolean, enabled: Boolean, roles: Array<String?>? = null, modified: LocalDateTime): this() {
        this.id = id
        this.firstname = firstname
        this.lastname = lastname
        this.birthdate = birthdate
        this.email = email
        this.password = password
        this.lastconnection = lastconnection
        this.status = status
        this.completed = completed
        this.enabled = enabled
        this.roles = roles
        this.modified = modified
        resetChangedOnNotNull()
    }
}
