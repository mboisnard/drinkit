package com.drinkit.file.storage

import com.drinkit.documentation.tech.starter.TechStarterTool

/**
 * Who may read, and delete, the files kept on one type of business object, beside their uploader. One bean per type,
 * which asks the same rule as the use cases of that object; none means nobody else.
 */
@TechStarterTool
interface FileAccess {

    val resourceType: String

    /** [readerId] is null for an anonymous reader. */
    fun canRead(resourceId: String, readerId: String?): Boolean

    /** Only the uploader deletes a file, unless the object says otherwise. */
    fun canDelete(resourceId: String, userId: String?): Boolean = false
}
