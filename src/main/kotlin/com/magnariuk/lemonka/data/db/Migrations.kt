package com.magnariuk.lemonka.data.db

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

/**
 * One schema change. Migrations run in order, each in its own transaction, and are recorded in
 * `schema_version`. They are written to be safe on any old database: every step checks before changing.
 *
 * To change the schema: update Tables.kt, then append a new Migration here with the next version.
 * Never edit a migration that has already shipped.
 */
class Migration(val version: Int, val description: String, val up: JdbcTransaction.() -> Unit)

object Migrations {
    val all = listOf(
        Migration(1, "Base schema from the Spring/Vaadin version") {
            // Creates only what's missing. Old databases never had a Users table created by the app.
            SchemaUtils.create(Users, Projects, Actors, Assignments, Characters, Separators, Settings, Tokens)
        },
        Migration(2, "User roles, creation date, invite codes") {
            addColumnIfMissing("Users", "role", "VARCHAR(16) NOT NULL DEFAULT 'user'")
            addColumnIfMissing("Users", "created_at", "BIGINT NULL")
            addColumnIfMissing("Users", "invited_by", "INT NULL")
            SchemaUtils.create(Invites)
            // Existing installs: the oldest account becomes the admin, so someone can manage the app.
            val hasAdmin = Users.selectAll().where { Users.role eq Role.Admin.id }.any()
            if (!hasAdmin) {
                Users.selectAll().orderBy(Users.id).limit(1).firstOrNull()?.let { first ->
                    Users.update({ Users.id eq first[Users.id] }) { it[role] = Role.Admin.id }
                }
            }
        },
        Migration(3, "Stored working subtitle file per project") {
            SchemaUtils.create(SubtitleFiles)
        },
        Migration(4, "Character images and public share links") {
            addColumnIfMissing("Projects", "share_token", "VARCHAR(40) NULL")
            SchemaUtils.create(CharacterImages)
        },
    )

    val latest get() = all.maxOf { it.version }

    /** Current schema version of the connected database (0 = never migrated). */
    fun JdbcTransaction.currentVersion(): Int {
        SchemaUtils.create(SchemaVersion)
        return SchemaVersion.selectAll().maxOfOrNull { it[SchemaVersion.version] } ?: 0
    }

    fun JdbcTransaction.record(m: Migration) {
        SchemaVersion.insert {
            it[version] = m.version
            it[description] = m.description
            it[appliedAt] = nowSeconds()
        }
    }

    /** True if the file already has app tables (i.e. it's an existing database, not a brand new one). */
    fun JdbcTransaction.hasAppTables(): Boolean =
        exec("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('Projects','Actors','Users')") { rs ->
            rs.next()
        } ?: false

    private fun JdbcTransaction.addColumnIfMissing(table: String, column: String, definition: String) {
        val exists = exec("PRAGMA table_info($table)") { rs ->
            var found = false
            while (rs.next()) if (rs.getString("name").equals(column, ignoreCase = true)) found = true
            found
        } ?: false
        if (!exists) exec("ALTER TABLE $table ADD COLUMN $column $definition")
    }
}
