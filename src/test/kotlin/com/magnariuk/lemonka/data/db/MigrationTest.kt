package com.magnariuk.lemonka.data.db

import at.favre.lib.crypto.bcrypt.BCrypt
import com.magnariuk.lemonka.auth.AuthResult
import com.magnariuk.lemonka.auth.AuthService
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.*

class MigrationTest {
    private lateinit var dir: File
    private lateinit var db: File

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("lemonka-mig").toFile()
        db = File(dir, "cache.db")
    }

    @AfterTest
    fun tearDown() { dir.deleteRecursively() }

    /** Builds a database the way the old Spring/Vaadin version (Exposed 0.55) left it, with some data. */
    private fun createOldDatabase() {
        val springHash = BCrypt.with(BCrypt.Version.VERSION_2A).hashToString(10, "oldpassword".toCharArray())
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { c ->
            c.createStatement().use { st ->
                listOf(
                    """CREATE TABLE Users (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(1000) NOT NULL, password VARCHAR(1000) NOT NULL, user_data TEXT NULL)""",
                    """CREATE TABLE Projects (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(1000) NOT NULL, "user" INT NOT NULL, CONSTRAINT fk_Projects_user__id FOREIGN KEY ("user") REFERENCES Users(id))""",
                    """CREATE TABLE Actors (id INTEGER PRIMARY KEY AUTOINCREMENT, actor_name VARCHAR(1000) NOT NULL, "user" INT NOT NULL)""",
                    """CREATE TABLE Assignments (id INTEGER PRIMARY KEY AUTOINCREMENT, actor INT NOT NULL, project INT NOT NULL)""",
                    """CREATE TABLE Characters (id INTEGER PRIMARY KEY AUTOINCREMENT, "name" VARCHAR(1000) NOT NULL, project INT NOT NULL, actor INT NULL)""",
                    """CREATE TABLE Separators (id INTEGER PRIMARY KEY AUTOINCREMENT, "separator" VARCHAR(1000) NOT NULL)""",
                    """CREATE TABLE Settings (id INTEGER PRIMARY KEY AUTOINCREMENT, "user" INT NOT NULL, hide_selected BOOLEAN NOT NULL)""",
                    """CREATE TABLE Tokens (id INTEGER PRIMARY KEY AUTOINCREMENT, "user" INT NOT NULL, token VARCHAR(50) NOT NULL, valid_to BIGINT NOT NULL)""",
                    """INSERT INTO Users ("name", password) VALUES ('olduser', '$springHash'), ('second', '$springHash')""",
                    """INSERT INTO Projects ("name", "user") VALUES ('Old Show', 1)""",
                    """INSERT INTO Actors (actor_name, "user") VALUES ('Ivan', 1)""",
                    """INSERT INTO Characters ("name", project, actor) VALUES ('Hero', 1, 1)""",
                    """INSERT INTO Assignments (actor, project) VALUES (1, 1)""",
                    """INSERT INTO Separators ("separator") VALUES (';'), ('та')""",
                    """INSERT INTO Tokens ("user", token, valid_to) VALUES (1, 'oldtoken', ${nowSeconds() + 3600})""",
                ).forEach(st::execute)
            }
        }
    }

    @Test
    fun `old Spring database is upgraded in place and keeps all data`() = runBlocking {
        createOldDatabase()
        DatabaseFactory.init(db.absolutePath)

        // Backup made before touching anything
        val backups = dir.listFiles()!!.filter { it.name.startsWith("cache.db.backup-v0-") }
        assertEquals(1, backups.size)

        // Data survived
        val old = UserRepository.byUsername("olduser")!!
        assertEquals(Role.Admin, old.role, "oldest account becomes admin")
        assertEquals(Role.User, UserRepository.byUsername("second")!!.role)
        assertEquals(listOf("Old Show"), ProjectRepository.forUser(old.id).map { it.name })
        assertEquals(listOf("Hero"), CharacterRepository.forActor(1, 1).map { it.name })
        assertEquals(listOf(";", "та"), SeparatorRepository.all(), "existing separators are not re-seeded")

        // Existing sessions and passwords keep working
        assertEquals("olduser", AuthService.userFor("oldtoken")?.username)
        assertIs<AuthResult.Success>(AuthService.login("olduser", "oldpassword"))

        // Running again is a no-op: no second backup, version stays latest
        DatabaseFactory.init(db.absolutePath)
        assertEquals(1, dir.listFiles()!!.count { it.name.contains(".backup-") })
    }

    @Test
    fun `fresh database gets latest schema without a backup`() = runBlocking {
        DatabaseFactory.init(db.absolutePath)
        assertTrue(dir.listFiles()!!.none { it.name.contains(".backup-") })
        assertEquals(0, UserRepository.count())
        assertEquals(listOf(";", ",", "and", "та"), SeparatorRepository.all())
    }
}
