package com.drinkit.search.engine.infra.meilisearch

import com.drinkit.search.engine.SearchIndexingFailed
import com.meilisearch.sdk.Client
import com.meilisearch.sdk.model.TaskStatus
import java.time.Duration

private val POLL_INTERVAL: Duration = Duration.ofMillis(20)

// The SDK's waitForTask gives up after 5 seconds and returns normally on a task that failed
internal class MeilisearchTasks(private val client: Client, private val timeout: Duration) {

    fun await(index: String, uid: Int) {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (true) {
            val task = client.getTask(uid)
            when (task.status) {
                TaskStatus.SUCCEEDED -> return

                TaskStatus.FAILED, TaskStatus.CANCELED ->
                    throw SearchIndexingFailed(index, task.error?.code, task.error?.message ?: "task $uid canceled")

                else -> if (System.nanoTime() > deadline) {
                    throw SearchIndexingFailed(index, null, "task $uid not finished after $timeout")
                }
            }
            Thread.sleep(POLL_INTERVAL)
        }
    }
}
