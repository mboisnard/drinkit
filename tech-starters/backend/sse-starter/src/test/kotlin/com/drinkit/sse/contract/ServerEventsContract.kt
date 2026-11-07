package com.drinkit.sse.contract

import org.springframework.web.bind.annotation.GetMapping

/** The stream as the API contract of an application describes it, for the coverage of its rule. */
internal interface ServerEventsContract {

    @GetMapping("/sse/event-stream")
    fun openServerEventsStream()
}
