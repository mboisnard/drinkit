package com.drinkit.bottle.search

import com.drinkit.api.generated.api.SearchApi
import com.drinkit.config.AbstractApi
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@RestController
@PreAuthorize("isAuthenticated()")
internal class SearchController :
    AbstractApi(),
    SearchApi {

    override fun searchByPhotoUpload(file: MultipartFile): ResponseEntity<Unit> =
        throw ResponseStatusException(FORBIDDEN)
}
