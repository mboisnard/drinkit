package com.drinkit.file.storage.infra.files.jooq

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.FileResource
import com.drinkit.file.storage.generated.jooq.tables.records.StoredFileRecord
import com.drinkit.file.storage.generated.jooq.tables.references.STORED_FILE
import com.drinkit.file.storage.infra.files.StoredFile
import com.drinkit.file.storage.infra.files.StoredFileMetadata
import com.drinkit.file.storage.infra.files.jooq.FileStatus.DELETED
import com.drinkit.file.storage.infra.files.jooq.FileStatus.KEPT
import com.drinkit.file.storage.infra.files.jooq.FileStatus.TEMPORARY
import com.drinkit.postgresql.toStoredTime
import org.jooq.DSLContext
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private enum class FileStatus { TEMPORARY, KEPT, DELETED }

@Transactional(propagation = Propagation.MANDATORY)
internal class JooqStoredFileMetadata(private val dsl: DSLContext) : StoredFileMetadata {

    override fun add(file: StoredFile) {
        dsl.insertInto(STORED_FILE)
            .set(STORED_FILE.ID, file.id.uuid())
            .set(STORED_FILE.USAGE, file.usage)
            .set(STORED_FILE.OWNER_ID, file.ownerId)
            .set(STORED_FILE.STATUS, TEMPORARY.name)
            .set(STORED_FILE.CONTENT_TYPE, file.type)
            .set(STORED_FILE.SIZE, file.size)
            .set(STORED_FILE.NAME, file.name)
            .set(STORED_FILE.UPLOADED_AT, file.uploadedAt.toStoredTime())
            .execute()
    }

    override fun keep(id: FileId, ownerId: String, usage: String, resource: FileResource): Boolean {
        val uuid = id.uuid() ?: return false

        return dsl.update(STORED_FILE)
            .set(STORED_FILE.STATUS, KEPT.name)
            .set(STORED_FILE.RESOURCE_TYPE, resource.type)
            .set(STORED_FILE.RESOURCE_ID, resource.id)
            .where(
                STORED_FILE.ID.eq(uuid),
                STORED_FILE.OWNER_ID.eq(ownerId),
                STORED_FILE.USAGE.eq(usage),
                STORED_FILE.STATUS.eq(TEMPORARY.name),
            )
            .execute() == 1
    }

    override fun markDeleted(id: FileId): Boolean {
        val uuid = id.uuid() ?: return false

        return dsl.update(STORED_FILE)
            .set(STORED_FILE.STATUS, DELETED.name)
            .where(STORED_FILE.ID.eq(uuid), STORED_FILE.STATUS.eq(KEPT.name))
            .execute() == 1
    }

    override fun findKept(ids: Collection<FileId>): List<StoredFile> = dsl.selectFrom(STORED_FILE)
        .where(STORED_FILE.ID.`in`(ids.mapNotNull { it.uuid() }), STORED_FILE.STATUS.eq(KEPT.name))
        .fetch { it.toStoredFile() }

    // SKIP LOCKED leaves the rows another cleanup took, so that two cleanups never wait on each other
    override fun takeToCleanUp(uploadedBefore: Instant, limit: Int): List<FileId> = dsl.select(STORED_FILE.ID)
        .from(STORED_FILE)
        .where(STORED_FILE.STATUS.ne(KEPT.name))
        .and(STORED_FILE.STATUS.eq(DELETED.name).or(STORED_FILE.UPLOADED_AT.lt(uploadedBefore.toStoredTime())))
        .limit(limit)
        .forUpdate()
        .skipLocked()
        .fetch(STORED_FILE.ID)
        .filterNotNull()
        .map { FileId(it.toString()) }

    override fun removeAll(ids: List<FileId>) {
        dsl.deleteFrom(STORED_FILE).where(STORED_FILE.ID.`in`(ids.mapNotNull { it.uuid() })).execute()
    }

    // An id the client made up is no UUID, and so no file
    private fun FileId.uuid(): UUID? = runCatching { UUID.fromString(value) }.getOrNull()

    private fun StoredFileRecord.toStoredFile() = StoredFile(
        id = FileId(id.toString()),
        usage = usage,
        ownerId = ownerId,
        type = contentType,
        size = size,
        name = name,
        uploadedAt = uploadedAt.toInstant(),
        resource = resourceType?.let { type -> resourceId?.let { FileResource(type, it) } },
    )
}
