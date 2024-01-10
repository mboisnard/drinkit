package com.drinkit.cellars.api

import com.drinkit.api.generated.api.CellarsApiDelegate
import com.drinkit.api.generated.model.CellarsResponse
import com.drinkit.common.toOptional
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.context.request.ServletWebRequest
import java.util.*

@Service
internal class CellarsApi(
    private val request: HttpServletRequest?,
): CellarsApiDelegate {
    override fun getRequest(): Optional<NativeWebRequest> = request?.let { ServletWebRequest(it) }.toOptional()
}