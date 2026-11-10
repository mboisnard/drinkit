package com.drinkit.security.infra.chain

import com.drinkit.security.CurrentVisitor
import com.drinkit.security.CurrentVisitorTestContract
import com.drinkit.security.IpAddress
import org.junit.jupiter.api.AfterEach
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

internal class RequestCurrentVisitorTest : CurrentVisitorTestContract() {

    override fun givenAVisit(visitorId: String, ipAddress: IpAddress): CurrentVisitor {
        val request = MockHttpServletRequest().apply {
            remoteAddr = ipAddress.value
            setAttribute(VISITOR_ID_ATTRIBUTE, visitorId)
        }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

        return RequestCurrentVisitor()
    }

    @AfterEach
    fun leaveTheRequest() = RequestContextHolder.resetRequestAttributes()
}
