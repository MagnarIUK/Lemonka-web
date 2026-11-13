package com.magnariuk.lemonka.data.db

import org.jetbrains.exposed.v1.core.ResultRow

enum class Role(val id: String, val title: String) {
    User("user", "Користувач"),
    Admin("admin", "Адміністратор");

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: User
    }
}

data class User(
    val id: Int,
    val username: String,
    val passwordHash: String,
    val role: Role = Role.User,
    val createdAt: Long? = null,
    val invitedBy: Int? = null,
) {
    val isAdmin get() = role == Role.Admin
}

data class Project(val id: Int, val name: String, val user: Int, val shareToken: String? = null)
data class Actor(val id: Int, val user: Int, val name: String)
data class Assignment(val id: Int, val actor: Int, val project: Int)
data class Character(val id: Int, val name: String, val project: Int, val actor: Int?)
data class Separator(val id: Int, val separator: String)
data class Setting(val id: Int, val user: Int, val hideSelected: Boolean = true)
data class Token(val id: Int, val user: Int, val token: String, val validTo: Long)

data class Invite(
    val id: Int,
    val code: String,
    val createdBy: Int,
    val createdAt: Long,
    val expiresAt: Long?,
    val maxUses: Int,
    val uses: Int,
    val note: String?,
    val revoked: Boolean,
) {
    fun isUsable(now: Long = nowSeconds()) =
        !revoked && uses < maxUses && (expiresAt == null || expiresAt > now)
}

fun nowSeconds() = System.currentTimeMillis() / 1000

internal fun ResultRow.toUser() = User(
    this[Users.id], this[Users.username], this[Users.password],
    Role.of(this[Users.role]), this[Users.createdAt], this[Users.invitedBy],
)
internal fun ResultRow.toProject() = Project(this[Projects.id], this[Projects.name], this[Projects.user], this[Projects.shareToken])
internal fun ResultRow.toActor() = Actor(this[Actors.id], this[Actors.user], this[Actors.actorName])
internal fun ResultRow.toAssignment() = Assignment(this[Assignments.id], this[Assignments.actor], this[Assignments.project])
internal fun ResultRow.toCharacter() =
    Character(this[Characters.id], this[Characters.name], this[Characters.project], this[Characters.actor])
internal fun ResultRow.toSeparator() = Separator(this[Separators.id], this[Separators.separator])
internal fun ResultRow.toSetting() = Setting(this[Settings.id], this[Settings.user], this[Settings.hideSelected])
internal fun ResultRow.toToken() = Token(this[Tokens.id], this[Tokens.user], this[Tokens.token], this[Tokens.validTo])
internal fun ResultRow.toInvite() = Invite(
    this[Invites.id], this[Invites.code], this[Invites.createdBy], this[Invites.createdAt], this[Invites.expiresAt],
    this[Invites.maxUses], this[Invites.uses], this[Invites.note], this[Invites.revoked],
)
