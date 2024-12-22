package com.magnariuk.lemonkasubstoolw.back.routes

import com.magnariuk.lemonkasubstoolw.data.Classes.Cache
import com.magnariuk.lemonkasubstoolw.data.api.CacheController
import org.jetbrains.exposed.sql.resolveColumnType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
class CacheRoot {

    @GetMapping("/cache")
    public fun cacheRoot(): Cache {
        val c = CacheController().getCache()!!
        return c
    }
}