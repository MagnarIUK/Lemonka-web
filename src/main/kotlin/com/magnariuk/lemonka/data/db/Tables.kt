package com.magnariuk.lemonka.data.db

import org.jetbrains.exposed.v1.core.Table

// Column/table names are kept identical to the old schema so existing cache.db files keep working.

object Users : Table() {
    val id = integer("id").autoIncrement()
    val username = varchar("name", 1000)
    val password = varchar("password", 1000)
    /** Legacy column, never used. Kept so old databases stay compatible. */
    val userData = text("user_data").nullable()
    val role = varchar("role", 16).default(Role.User.id)            // added in migration 2
    val createdAt = long("created_at").nullable()                   // added in migration 2
    val invitedBy = integer("invited_by").nullable()                // invite id, added in migration 2
    override val primaryKey = PrimaryKey(id, name = "users_pk")
}

object Projects : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 1000)
    val user = integer("user").references(Users.id)
    /** Secret for the public (no-login) project sheet; null = sharing off. Added in migration 4. */
    val shareToken = varchar("share_token", 40).nullable()
    override val primaryKey = PrimaryKey(id, name = "projects_pk")
}

object Actors : Table() {
    val id = integer("id").autoIncrement()
    val actorName = varchar("actor_name", 1000)
    val user = integer("user").references(Users.id)
    override val primaryKey = PrimaryKey(id, name = "actors_pk")
}

object Assignments : Table() {
    val id = integer("id").autoIncrement()
    val actor = integer("actor").references(Actors.id)
    val project = integer("project").references(Projects.id)
    override val primaryKey = PrimaryKey(id, name = "assignments_pk")
}

object Characters : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 1000)
    val project = integer("project").references(Projects.id)
    val actor = integer("actor").references(Actors.id).nullable()
    override val primaryKey = PrimaryKey(id, name = "characters_pk")
}

object Separators : Table() {
    val id = integer("id").autoIncrement()
    val separator = varchar("separator", 1000)
    override val primaryKey = PrimaryKey(id, name = "separators_pk")
}

object Settings : Table() {
    val id = integer("id").autoIncrement()
    val user = integer("user").references(Users.id)
    val hideSelected = bool("hide_selected")
    override val primaryKey = PrimaryKey(id, name = "settings_pk")
}

object Tokens : Table() {
    val id = integer("id").autoIncrement()
    val user = integer("user").references(Users.id)
    val token = varchar("token", 50)
    val validTo = long("valid_to")
    override val primaryKey = PrimaryKey(id, name = "token_pk")
}

object Invites : Table() {
    val id = integer("id").autoIncrement()
    val code = varchar("code", 32).uniqueIndex()
    val createdBy = integer("created_by").references(Users.id)
    val createdAt = long("created_at")
    val expiresAt = long("expires_at").nullable()
    val maxUses = integer("max_uses").default(1)
    val uses = integer("uses").default(0)
    val note = varchar("note", 200).nullable()
    val revoked = bool("revoked").default(false)
    override val primaryKey = PrimaryKey(id, name = "invites_pk")
}

/** The .ass file currently being worked on in a project (one per project). */
object SubtitleFiles : Table("subtitle_files") {
    val id = integer("id").autoIncrement()
    val project = integer("project").references(Projects.id).uniqueIndex()
    val name = varchar("name", 300)
    val content = text("content")
    val uploadedAt = long("uploaded_at")
    override val primaryKey = PrimaryKey(id)
}

/** One picture per character, already resized. Stored in the DB so a backup of cache.db has everything. */
object CharacterImages : Table("character_images") {
    val id = integer("id").autoIncrement()
    val character = integer("character").references(Characters.id).uniqueIndex()
    val contentType = varchar("content_type", 50)
    val data = blob("data")
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

/** Which migrations have been applied to this database file. */
object SchemaVersion : Table("schema_version") {
    val version = integer("version")
    val description = varchar("description", 200)
    val appliedAt = long("applied_at")
    override val primaryKey = PrimaryKey(version)
}
