package com.drinkit.file.storage.infra.files

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.FileNotFound
import com.drinkit.file.storage.FileResource
import com.drinkit.file.storage.FileStorage
import com.drinkit.security.CurrentUser
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.InputStreamSource
import org.springframework.core.io.Resource
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Propagation.REQUIRES_NEW
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.context.request.RequestContextHolder
import java.net.URI
import java.time.Clock
import java.time.Duration
import java.util.UUID

private const val CLEANED_PER_RUN = 100

// User ids are UUIDs, so this owner never matches a user's file
internal const val BACKEND = "system"

// On the class, so that kotlin-spring opens it for its proxy
@Transactional
internal class StoredFiles(
    private val uploads: FileUploads,
    private val metadata: StoredFileMetadata,
    private val storage: Storage,
    private val rights: FileRights,
    private val currentUser: CurrentUser,
    private val urlTtl: Duration,
) : FileStorage {

    // Committed on its own, so that a caller rolling back leaves a temporary file to clean, never an orphan object
    @Transactional(propagation = REQUIRES_NEW)
    override fun store(usage: String, name: String, content: Resource): FileId =
        uploads.upload(usage, name, declaredType = null, size = content.contentLength(), content = content)

    // Joins the caller's transaction, so that it commits or rolls back with its business change
    @Transactional(propagation = MANDATORY)
    override fun keep(id: FileId, usage: String, resource: FileResource) {
        val owner = currentUser.actorId() ?: throw FileNotFound()

        if (!metadata.keep(id, owner, usage, resource)) throw FileNotFound()
    }

    @Transactional(propagation = MANDATORY)
    override fun delete(id: FileId) {
        val file = metadata.findKept(listOf(id)).singleOrNull()
        val allowed = file != null && (isBackend() || rights.canDelete(file, currentUser.orNull()))

        if (!allowed || !metadata.markDeleted(id)) throw FileNotFound()
    }

    @Transactional(readOnly = true)
    override fun urlOf(id: FileId): URI = readableUrls(listOf(id))[id] ?: throw FileNotFound()

    @Transactional(readOnly = true)
    override fun urlsOf(ids: Collection<FileId>): Map<FileId, URI> = readableUrls(ids)

    /** A kept file of a public usage, which anyone reads. */
    @Transactional(readOnly = true)
    fun findPublic(id: FileId): StoredFile? = metadata.findKept(listOf(id)).singleOrNull()?.takeIf(rights::isPublic)

    private fun readableUrls(ids: Collection<FileId>): Map<FileId, URI> {
        val kept = metadata.findKept(ids)
        val readable = if (isBackend()) kept else rights.readable(kept, currentUser.orNull())

        return readable.associate {
            it.id to if (rights.isPublic(it)) rights.publicUrlOf(it) else storage.urlOf(it, urlTtl)
        }
    }

    private fun isBackend() = currentUser.actorId() == BACKEND
}

/** Checks a file, then stores it as temporary, owned by the user who sends it, or by the backend. */
// On the class, so that kotlin-spring opens it for its proxy
@Transactional
internal class FileUploads(
    private val metadata: StoredFileMetadata,
    private val storage: Storage,
    private val rules: UploadRules,
    private val currentUser: CurrentUser,
    private val clock: Clock,
) {

    // A failed write deletes what it wrote of the object, which the cleanup, going by the rows, would never find
    fun upload(usage: String, name: String, declaredType: String?, size: Long, content: InputStreamSource): FileId {
        val accepted = rules.accept(usage, name, declaredType, size, content)
        val file = StoredFile(
            id = FileId(UUID.randomUUID().toString()),
            usage = usage,
            ownerId = checkNotNull(currentUser.actorId()) { "Only a signed-in user or the backend stores a file" },
            type = accepted.type,
            size = size,
            name = accepted.name,
            uploadedAt = clock.instant(),
        )

        metadata.add(file)
        runCatching { storage.put(file, content) }
            .onFailure { failure -> runCatching { storage.delete(listOf(file.id)) }.onFailure(failure::addSuppressed) }
            .getOrThrow()

        return file.id
    }
}

/** Deletes the deleted files, and the temporary ones older than the retention, a batch per run. */
internal class FileCleanUp(
    private val metadata: StoredFileMetadata,
    private val storage: Storage,
    private val clock: Clock,
    private val retention: Duration,
    private val transactions: TransactionTemplate,
) {

    private val logger = KotlinLogging.logger {}

    // A failed run leaves its files to the next one: it never fails the upload that started it
    @Suppress("TooGenericExceptionCaught")
    fun run() {
        try {
            transactions.executeWithoutResult { cleanUp() }
        } catch (exception: RuntimeException) {
            logger.warn(exception) { "Cleaning up forgotten files failed" }
        }
    }

    private fun cleanUp() {
        val files = metadata.takeToCleanUp(uploadedBefore = clock.instant() - retention, limit = CLEANED_PER_RUN)

        if (files.isNotEmpty()) {
            storage.delete(files)
            metadata.removeAll(files)
        }
    }
}

/** The signed-in user, else the backend outside a request, such as a job or an importer: null for an anonymous one. */
internal fun CurrentUser.actorId(): String? =
    orNull()?.id ?: BACKEND.takeIf { RequestContextHolder.getRequestAttributes() == null }
