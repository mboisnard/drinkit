package com.drinkit.file.storage.infra.filesystem

import com.drinkit.file.storage.FileId
import com.drinkit.file.storage.infra.files.Storage
import com.drinkit.file.storage.infra.files.StoredFile
import org.springframework.core.io.InputStreamSource
import org.springframework.web.util.UriComponentsBuilder
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal const val SIGNED_FILE_PATH = "/files/{id}"

private const val HMAC = "HmacSHA256"

/**
 * Files on a directory, served by the application itself through an URL signed with HMAC-SHA256.
 *
 * OWASP: File Upload Cheat Sheet (storage outside the webroot, generated file names).
 */
internal class FilesystemStorage(
    private val directory: Path,
    private val signingKey: ByteArray,
    private val clock: Clock,
    private val contextPath: String = "",
) : Storage {

    init {
        Files.createDirectories(directory)
    }

    override fun put(file: StoredFile, content: InputStreamSource) {
        content.inputStream.use { Files.copy(it, directory.resolve(file.id.value), REPLACE_EXISTING) }
    }

    override fun read(file: StoredFile): InputStream = Files.newInputStream(directory.resolve(file.id.value))

    override fun delete(ids: List<FileId>) {
        ids.forEach { Files.deleteIfExists(directory.resolve(it.value)) }
    }

    override fun urlOf(file: StoredFile, ttl: Duration): URI {
        val expires = (clock.instant() + ttl).epochSecond
        val signature = sign(file.id.value, file.type, file.name, expires)

        return UriComponentsBuilder.fromPath(contextPath + SIGNED_FILE_PATH)
            .queryParam("type", file.type)
            .queryParam("name", file.name)
            .queryParam("expires", expires)
            .queryParam("signature", signature)
            .encode()
            .buildAndExpand(file.id.value)
            .toUri()
    }

    /** Null when the signature is wrong, the URL expired or the file is gone. */
    fun open(file: SignedFile): Path? {
        val id = FileId.parseOrNull(file.id)?.value ?: return null
        val expected = sign(id, file.type, file.name, file.expires)
        val valid = MessageDigest.isEqual(expected.toByteArray(), file.signature.toByteArray())
        val path = directory.resolve(id)

        return path.takeIf { valid && clock.instant() < Instant.ofEpochSecond(file.expires) && Files.exists(it) }
    }

    private fun sign(id: String, type: String, name: String, expires: Long): String {
        val mac = Mac.getInstance(HMAC).apply { init(SecretKeySpec(signingKey, HMAC)) }
        val signed = mac.doFinal("$id\n$type\n$name\n$expires".toByteArray())

        return Base64.getUrlEncoder().withoutPadding().encodeToString(signed)
    }
}
