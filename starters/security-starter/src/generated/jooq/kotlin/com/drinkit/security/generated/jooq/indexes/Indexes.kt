/*
 * This file is generated by jOOQ.
 */
package com.drinkit.security.generated.jooq.indexes


import com.drinkit.security.generated.jooq.tables.Role
import com.drinkit.security.generated.jooq.tables.User

import org.jooq.Index
import org.jooq.impl.DSL
import org.jooq.impl.Internal



// -------------------------------------------------------------------------
// INDEX definitions
// -------------------------------------------------------------------------

val USER_EMAIL_IDX: Index = Internal.createIndex(DSL.name("user_email_idx"), User.USER, arrayOf(User.USER.EMAIL), false)
val USER_ID_AUTHORITY_IDX: Index = Internal.createIndex(DSL.name("user_id_authority_idx"), Role.ROLE, arrayOf(Role.ROLE.USER_ID, Role.ROLE.AUTHORITY), true)
