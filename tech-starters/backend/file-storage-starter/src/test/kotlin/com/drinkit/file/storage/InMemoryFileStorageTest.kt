package com.drinkit.file.storage

internal class InMemoryFileStorageTest : FileStorageTestContract() {

    override val storage = InMemoryFileStorage()

    override fun uploaded(): FileId = storage.upload(USAGE)
}
