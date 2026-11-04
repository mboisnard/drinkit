package com.drinkit.documentation.processor.ksp.documentation

import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException

private const val LOCK_RETRY_DELAY_MS = 10L

/**
 * Reads and rewrites the file as one step: the KSP task of every module rewrites the same overview, in parallel
 * threads or processes under `org.gradle.parallel`.
 */
internal fun File.rewriteLocked(rewrite: (content: String?) -> String) {
    parentFile?.mkdirs()
    RandomAccessFile(resolveSibling("$name.lock"), "rw").channel.use { channel ->
        channel.lockAgainstThreads().use {
            writeText(rewrite(if (exists()) readText() else null))
        }
    }
}

// A file lock only guards against other processes: a second thread of this JVM gets an exception, and waits
private fun FileChannel.lockAgainstThreads(): FileLock {
    while (true) {
        try {
            return lock()
        } catch (_: OverlappingFileLockException) {
            Thread.sleep(LOCK_RETRY_DELAY_MS)
        }
    }
}
