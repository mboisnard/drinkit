package com.drinkit.security.infra.session

import org.springframework.session.FindByIndexNameSessionRepository
import org.springframework.session.FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME
import org.springframework.session.MapSession

internal class InMemoryIndexedSessions : FindByIndexNameSessionRepository<MapSession> {

    private val sessions = mutableMapOf<String, MapSession>()

    fun ofUser(userId: String) = MapSession().also {
        it.setAttribute(PRINCIPAL_NAME_INDEX_NAME, userId)
        save(it)
    }

    override fun createSession() = MapSession()

    override fun save(session: MapSession) {
        sessions[session.id] = session
    }

    override fun findById(id: String): MapSession? = sessions[id]

    override fun deleteById(id: String) {
        sessions.remove(id)
    }

    override fun findByIndexNameAndIndexValue(indexName: String, indexValue: String): Map<String, MapSession> =
        sessions.filterValues { it.getAttribute<String>(indexName) == indexValue }
}
