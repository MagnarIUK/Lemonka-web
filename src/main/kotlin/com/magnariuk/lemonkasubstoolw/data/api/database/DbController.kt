package com.magnariuk.lemonkasubstoolw.data.api.database

import org.springframework.stereotype.Service

@Service
class DbController {
    private final val dbc = DatabaseController().apply { init() }
    val db = DB(dbc)

    fun getDB(): DB = db
}