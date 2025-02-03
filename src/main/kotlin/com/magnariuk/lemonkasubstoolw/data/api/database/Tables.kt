package com.magnariuk.lemonkasubstoolw.data.api.database

import com.magnariuk.lemonkasubstoolw.data.Classes.Ass
import com.magnariuk.lemonkasubstoolw.data.util.td
import org.jetbrains.exposed.sql.*
import kotlin.random.Random


object Users: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val username: Column<String> = varchar("name", 1000)
    val password: Column<String> = varchar("password", 1000)
    val userData: Column<String?> = text("user_data").nullable()
    override val primaryKey = PrimaryKey(id, name = "users_pk")
}
data class User(
    var id: Int,
    var username: String,
    var password: String,
    val userData: UserData?
)
data class UserData(
    val savedSubtitles: MutableList<Ass> = mutableListOf(),
)

object Projects: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val name: Column<String> = varchar("name", 1000)
    val user: Column<Int> = integer("user").references(Users.id)
    override val primaryKey = PrimaryKey(id, name = "projects_pk")
}

data class Project(
    var id: Int,
    var name: String,
    val user: Int?
)

object Actors: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val actorName: Column<String> = varchar("actor_name", 1000)
    val user: Column<Int> = integer("user").references(Users.id)
    override val primaryKey = PrimaryKey(id, name = "actors_pk")
}

data class Actor(
    var id: Int,
    val user: Int,
    var actorName: String
)

object Assignments: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val actor: Column<Int> = integer("actor").references(Actors.id)
    val project: Column<Int> = integer("project").references(Projects.id)
    override val primaryKey = PrimaryKey(id, name = "assignments_pk")
}

data class Assignment(
    var id: Int,
    var actor: Int,
    var project: Int
)

object Characters: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val name: Column<String> = varchar("name", 1000)
    val project: Column<Int> = integer("project").references(Projects.id)
    val actor: Column<Int?> = integer("actor").references(Actors.id).nullable()
    override val primaryKey = PrimaryKey(id, name = "characters_pk")
}

data class Character(
    var id: Int,
    var name: String,
    var project: Int,
    var actor: Int?
)

object Separators: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val separator: Column<String> = varchar("separator", 1000)
    override val primaryKey = PrimaryKey(id, name = "separators_pk")
}

data class Separator(
    var id: Int,
    var separator: String
)

object Settings: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val user: Column<Int> = integer("user").references(Users.id)
    val hideSelected: Column<Boolean> = bool("hide_selected")
    override val primaryKey = PrimaryKey(id, name = "settings_pk")
}

data class Setting(
    var id: Int,
    var hideSelected: Boolean = true,
    var user: Int
)



object Tokens: Table() {
    val id: Column<Int> = integer("id").autoIncrement()
    val user: Column<Int> = integer("user").references(Users.id)
    val token: Column<String> = varchar("token", 50)
    val valid_to: Column<Long> = long("valid_to")
    override val primaryKey = PrimaryKey(id, name = "token_pk")

    fun generateToken(length: Int = 50): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
    fun getExpireTime(): Long {
        return (System.currentTimeMillis() / 1000) + 30.td
    }
}

data class Token(
    val id: Int?,
    val user: Int?,
    val token: String?,
    val valid_to: Long?,
)

object META: Table("meta") {
    val id: Column<Int> = integer("id")
    val version: Column<Float> = float("version")
    override val primaryKey = PrimaryKey(id, name = "meta_pk")
}


