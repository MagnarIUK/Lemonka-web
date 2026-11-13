package com.magnariuk.lemonka.data.db

import com.magnariuk.lemonka.data.db.Migrations.currentVersion
import com.magnariuk.lemonka.data.db.Migrations.hasAppTables
import com.magnariuk.lemonka.data.db.Migrations.record
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DatabaseFactory {
    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private val defaultSeparators = listOf(";", ",", "and", "та")

    fun init(dbPath: String) {
        Database.connect("jdbc:sqlite:$dbPath", driver = "org.sqlite.JDBC")
        migrate(dbPath)
        transaction {
            if (Separators.selectAll().empty()) {
                Separators.batchInsert(defaultSeparators) { this[Separators.separator] = it }
            }
        }
    }

    /**
     * Brings any database - brand new, from the old Spring version, or from an earlier version of this one -
     * up to the latest schema. An existing file is backed up next to itself before anything is changed.
     */
    private fun migrate(dbPath: String) {
        val (current, existing) = transaction { currentVersion() to hasAppTables() }
        val pending = Migrations.all.filter { it.version > current }
        if (pending.isEmpty()) {
            log.info("Database {} is up to date (schema v{})", dbPath, current)
            return
        }

        if (existing) {
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val backup = File("$dbPath.backup-v$current-$stamp")
            File(dbPath).copyTo(backup)
            log.info("Backed up {} to {} before migrating", dbPath, backup.name)
        }

        pending.forEach { m ->
            transaction {
                m.up(this)
                record(m)
            }
            log.info("Applied migration v{}: {}", m.version, m.description)
        }
    }
}

/** Runs a blocking JDBC transaction off the request thread. */
suspend fun <T> dbQuery(block: () -> T): T = withContext(Dispatchers.IO) { transaction { block() } }
